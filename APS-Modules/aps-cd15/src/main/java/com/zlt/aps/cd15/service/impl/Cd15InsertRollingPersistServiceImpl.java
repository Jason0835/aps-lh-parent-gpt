package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftWindowResolver;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftConfigMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15TimedRollingService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15InsertRollingPersistService;
import com.zlt.aps.cd15.service.Cd15TimedRollingPersistService;
import com.zlt.aps.cd15.service.Cd15TimedRollingPrefixResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** CD15斜裁插单、转机台、调量最终短事务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15InsertRollingPersistServiceImpl implements Cd15InsertRollingPersistService {

    private static final int CLASS_COUNT = 8;
    private static final int ACTIVE = 1;
    private static final String AGING_PERIOD_PARAM_CODE = "SYS0601032";
    private static final int DEFAULT_AGING_PERIOD_HOURS = 24;
    private static final String UNRELEASED = "0";
    private static final String UNLOCKED = "0";

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15ScheduleResultLogMapper logMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15TimedRollingService rollingService;
    private final Cd15TimedRollingPrefixResourceService prefixResourceService;
    private final Cd15TimedRollingPersistService rollingPersistService;
    private final Cd15AutoScheduleInputVersionService inputVersionService;
    private final Cd15ParamsMapper paramsMapper;
    private final Cd15EngineShiftConfigMapper shiftConfigMapper;
    private final Cd15ShiftWindowResolver shiftWindowResolver;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(String taskId, Cd15InsertOrderRequest request, RLock lock) {
        this.validateCommitState(taskId, lock);
        String batchNo = this.resolveBatchNo(request.getFactoryCode(), request.getScheduleDate());
        Cd15ScheduleResult result = this.toInsertResult(request, batchNo);
        if (resultMapper.insert(result) != 1) {
            throw new IllegalStateException("保存CD15插单结果失败");
        }
        this.saveInsertLanes(result);
        this.saveLog(taskId, result, "CREATE", "INSERT_ORDER", request, null);
        this.runSuffixRolling(taskId, this.manualTarget(request.getFactoryCode(), request.getScheduleDate(),
                batchNo, this.lastPositiveClassIndex(result) + 1), lock, batchNo, "INSERT_ORDER");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistTransfer(String taskId, Cd15TransferMachineRequest request, RLock lock) {
        this.validateCommitState(taskId, lock);
        List<Cd15ScheduleResult> results = resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, request.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, request.getScheduleDate())
                .eq(Cd15ScheduleResult::getMachineCode, request.getSourceMachineCode())
                .eq(this.hasText(request.getSteelStripCode()), Cd15ScheduleResult::getSteelStripCode, request.getSteelStripCode()));
        if (results.isEmpty()) {
            throw new IllegalStateException("未找到CD15转机台原排程结果");
        }
        results.forEach(result -> {
            String beforeJson = this.json(result);
            result.setMachineCode(request.getTargetMachineCode());
            IntStream.rangeClosed(1, CLASS_COUNT).forEach(classIndex -> {
                Integer produceOrder = this.readTransferProduceOrder(request, classIndex);
                if (produceOrder != null) {
                    result.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), produceOrder);
                }
            });
            if (resultMapper.updateById(result) != 1) {
                throw new IllegalStateException("更新CD15转机台结果失败");
            }
            laneMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleLaneAllocation>()
                    .eq(Cd15ScheduleLaneAllocation::getScheduleResultId, result.getId())
                    .set(Cd15ScheduleLaneAllocation::getMachineCode, request.getTargetMachineCode()));
            this.saveLog(taskId, result, "UPDATE", "TRANSFER_MACHINE", request, beforeJson);
        });
        String batchNo = this.firstBatchNo(results);
        this.runSuffixRolling(taskId, this.manualTarget(request.getFactoryCode(), request.getScheduleDate(),
                batchNo, this.lastPositiveClassIndex(results) + 1), lock, batchNo, "TRANSFER_MACHINE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistChangeQty(String taskId, Cd15ChangeQtyRequest request, RLock lock) {
        this.validateCommitState(taskId, lock);
        Cd15ScheduleResult result = this.findChangeQtyTarget(request);
        String beforeJson = this.json(result);
        this.resolveChangeQtyTargets(request).forEach((classIndex, planQty) -> {
            Double finishQty = this.readDouble(result, String.format("class%dFinishQty", classIndex));
            if (finishQty != null && planQty < finishQty) {
                throw new IllegalStateException("CD15调量目标不能小于已完成数量");
            }
            result.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex), planQty);
            result.setFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex), request.getRemark());
        });
        if (resultMapper.updateById(result) != 1) {
            throw new IllegalStateException("更新CD15调量结果失败");
        }
        this.saveLog(taskId, result, "UPDATE", "CHANGE_QTY", request, beforeJson);
        int nextClassIndex = this.resolveChangeQtyTargets(request).keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(this.lastPositiveClassIndex(result)) + 1;
        this.runSuffixRolling(taskId, this.manualTarget(request.getFactoryCode(), request.getScheduleDate(),
                result.getCd15BatchNo(), nextClassIndex), lock, result.getCd15BatchNo(), "CHANGE_QTY");
    }

    private Cd15ScheduleResult toInsertResult(Cd15InsertOrderRequest request, String batchNo) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setFactoryCode(request.getFactoryCode());
        result.setScheduleDate(request.getScheduleDate());
        result.setCd15BatchNo(batchNo);
        result.setOrderNo("CD15I" + DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now()));
        result.setGroupNo(result.getOrderNo());
        result.setReleaseStatus(UNRELEASED);
        result.setBigRollCode(request.getBigRollCode());
        result.setCuttingAngle(this.resolveInsertCuttingAngle(request));
        result.setMachineCode(request.getMachineCode());
        result.setStorageLaneCode(this.resolveInsertStorageLaneCode(request));
        result.setSteelStripCode(request.getSteelStripCode());
        result.setSourceType("INSERT_ORDER");
        result.setIsLocked(UNLOCKED);
        IntStream.rangeClosed(1, CLASS_COUNT).forEach(classIndex -> {
            Double planQty = this.readInsertPlanQty(request, classIndex);
            Integer produceOrder = this.readInsertProduceOrder(request, classIndex);
            if (planQty != null && planQty > 0D) {
                String prefix = String.format("class%d", classIndex);
                result.setFieldValueByFieldName(prefix + "ScheduleDate", request.getScheduleDate());
                result.setFieldValueByFieldName(prefix + "PlanQty", planQty);
                result.setFieldValueByFieldName(prefix + "FinishQty", 0D);
                result.setFieldValueByFieldName(prefix + "ProduceOrder", produceOrder);
                result.setFieldValueByFieldName(prefix + "FinishRate", 0D);
                result.setFieldValueByFieldName(prefix + "AnalysisInput", this.readInsertAnalysisInput(request, classIndex));
            }
        });
        return result;
    }

    private void saveInsertLanes(Cd15ScheduleResult result) {
        if (!this.hasText(result.getStorageLaneCode())) {
            return;
        }
        IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> this.readDouble(result, String.format("class%dPlanQty", classIndex)) != null
                        && this.readDouble(result, String.format("class%dPlanQty", classIndex)) > 0D)
                .forEach(classIndex -> {
                    Cd15ScheduleLaneAllocation lane = new Cd15ScheduleLaneAllocation();
                    lane.setFactoryCode(result.getFactoryCode());
                    lane.setScheduleDate(result.getScheduleDate());
                    lane.setBatchNo(result.getCd15BatchNo());
                    lane.setScheduleResultId(result.getId());
                    lane.setOrderNo(result.getOrderNo());
                    lane.setGroupNo(result.getGroupNo());
                    lane.setClassField("CLASS" + classIndex);
                    lane.setShiftScheduleDate(result.getScheduleDate());
                    lane.setStorageLaneCode(result.getStorageLaneCode());
                    lane.setSteelStripCode(result.getSteelStripCode());
                    lane.setBigRollCode(result.getBigRollCode());
                    lane.setCuttingAngle(result.getCuttingAngle());
                    lane.setMachineCode(result.getMachineCode());
                    lane.setAllocatedQty(java.math.BigDecimal.valueOf(this.readDouble(result, String.format("class%dPlanQty", classIndex))));
                    lane.setAllocatedCartCount(1);
                    lane.setAllocationOrder(classIndex);
                    if (laneMapper.insert(lane) != 1) {
                        throw new IllegalStateException("保存CD15插单库排分配失败");
                    }
                });
    }

    private String resolveInsertCuttingAngle(Cd15InsertOrderRequest request) {
        if (this.hasText(request.getCuttingAngle())) {
            return request.getCuttingAngle();
        }
        Cd15ScheduleResult referenceResult = this.findInsertReferenceResult(request);
        if (referenceResult != null && this.hasText(referenceResult.getCuttingAngle())) {
            return referenceResult.getCuttingAngle();
        }
        Cd15ScheduleLaneAllocation referenceLane = this.findInsertReferenceLane(request);
        return referenceLane != null && this.hasText(referenceLane.getCuttingAngle())
                ? referenceLane.getCuttingAngle() : null;
    }

    private String resolveInsertStorageLaneCode(Cd15InsertOrderRequest request) {
        if (this.hasText(request.getStorageLaneCode())) {
            return request.getStorageLaneCode();
        }
        Cd15ScheduleLaneAllocation referenceLane = this.findInsertReferenceLane(request);
        if (referenceLane != null && this.hasText(referenceLane.getStorageLaneCode())) {
            return referenceLane.getStorageLaneCode();
        }
        Cd15ScheduleResult referenceResult = this.findInsertReferenceResult(request);
        return referenceResult != null && this.hasText(referenceResult.getStorageLaneCode())
                ? referenceResult.getStorageLaneCode() : null;
    }

    private Cd15ScheduleResult findInsertReferenceResult(Cd15InsertOrderRequest request) {
        List<Cd15ScheduleResult> results = resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, request.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, request.getScheduleDate())
                .eq(Cd15ScheduleResult::getMachineCode, request.getMachineCode())
                .eq(Cd15ScheduleResult::getSteelStripCode, request.getSteelStripCode())
                .eq(this.hasText(request.getBigRollCode()), Cd15ScheduleResult::getBigRollCode, request.getBigRollCode())
                .orderByDesc(Cd15ScheduleResult::getCreateTime));
        return results.stream()
                .filter(result -> this.hasText(result.getCuttingAngle()) || this.hasText(result.getStorageLaneCode()))
                .findFirst()
                .orElseGet(() -> results.stream().findFirst().orElse(null));
    }

    private Cd15ScheduleLaneAllocation findInsertReferenceLane(Cd15InsertOrderRequest request) {
        List<String> classFields = this.resolveInsertClassFields(request);
        List<Cd15ScheduleLaneAllocation> lanes = laneMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                .eq(Cd15ScheduleLaneAllocation::getFactoryCode, request.getFactoryCode())
                .eq(Cd15ScheduleLaneAllocation::getScheduleDate, request.getScheduleDate())
                .eq(Cd15ScheduleLaneAllocation::getMachineCode, request.getMachineCode())
                .eq(Cd15ScheduleLaneAllocation::getSteelStripCode, request.getSteelStripCode())
                .eq(this.hasText(request.getBigRollCode()), Cd15ScheduleLaneAllocation::getBigRollCode, request.getBigRollCode())
                .in(!classFields.isEmpty(), Cd15ScheduleLaneAllocation::getClassField, classFields)
                .orderByDesc(Cd15ScheduleLaneAllocation::getCreateTime));
        return lanes.stream()
                .filter(lane -> this.hasText(lane.getStorageLaneCode()) || this.hasText(lane.getCuttingAngle()))
                .findFirst()
                .orElseGet(() -> lanes.stream().findFirst().orElse(null));
    }

    private List<String> resolveInsertClassFields(Cd15InsertOrderRequest request) {
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> {
                    Double planQty = this.readInsertPlanQty(request, classIndex);
                    return planQty != null && planQty > 0D;
                })
                .mapToObj(classIndex -> "CLASS" + classIndex)
                .collect(Collectors.toList());
    }

    private Cd15ScheduleResult findChangeQtyTarget(Cd15ChangeQtyRequest request) {
        LambdaQueryWrapper<Cd15ScheduleResult> wrapper = new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, request.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, request.getScheduleDate())
                .eq(Cd15ScheduleResult::getMachineCode, request.getMachineCode())
                .eq(Cd15ScheduleResult::getSteelStripCode, request.getSteelStripCode())
                .orderByDesc(Cd15ScheduleResult::getCreateTime)
                .last("limit 1");
        if (request.getScheduleResultId() != null) {
            wrapper.eq(Cd15ScheduleResult::getId, request.getScheduleResultId());
        }
        Cd15ScheduleResult result = resultMapper.selectOne(wrapper);
        if (result == null) {
            throw new IllegalStateException("未找到CD15调量排程结果");
        }
        return result;
    }

    private Map<Integer, Double> resolveChangeQtyTargets(Cd15ChangeQtyRequest request) {
        Map<Integer, Double> targetQtyByClass = new LinkedHashMap<>();
        if (this.hasText(request.getStartClassField()) || request.getTargetPlanQty() != null) {
            if (!this.hasText(request.getStartClassField()) || request.getTargetPlanQty() == null) {
                throw new IllegalArgumentException("CD15调量班次和目标计划量必须同时填写");
            }
            targetQtyByClass.put(this.parseClassIndex(request.getStartClassField()), request.getTargetPlanQty());
        }
        IntStream.rangeClosed(1, CLASS_COUNT).forEach(classIndex -> {
            Double planQty = (Double) request.getFieldValueByFieldName(String.format("class%dPlanQty", classIndex));
            if (planQty != null) {
                targetQtyByClass.put(classIndex, planQty);
            }
        });
        if (targetQtyByClass.isEmpty()) {
            throw new IllegalArgumentException("至少填写一个CD15调量目标计划量");
        }
        return targetQtyByClass;
    }

    private void validateCommitState(String taskId, RLock lock) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("CD15人工调整任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("CD15人工调整执行锁已失效");
        }
    }

    private void markSuccess(String taskId, String batchNo, String actionName) {
        if (!taskService.markSuccessInCurrentTransaction(taskId, batchNo)) {
            throw new IllegalStateException("CD15" + actionName + "任务状态已变化，不能提交结果");
        }
    }

    private void runSuffixRolling(String taskId, Cd15RollingTarget target, RLock lock,
                                  String batchNo, String actionName) {
        if (target == null || target.getTargetClassIndex() > CLASS_COUNT) {
            this.markSuccess(taskId, batchNo, actionName);
            return;
        }
        String inputVersion = inputVersionService.fingerprint(target.getFactoryCode(), target.getScheduleDate());
        List<Cd15RollingPrefixResourceUsage> prefixResourceUsages =
                prefixResourceService.loadPrefixResourceUsages(target);
        Cd15TimedRollingOutput output = rollingService.execute(target, inputVersion,
                this.resolveAgingPeriodHours(target.getFactoryCode()), prefixResourceUsages);
        rollingPersistService.persist(taskId, target, output, lock);
    }

    private Cd15RollingTarget manualTarget(String factoryCode, Date scheduleDate, String batchNo,
                                           int targetClassIndex) {
        int safeClassIndex = Math.max(1, targetClassIndex);
        LocalDate localScheduleDate = this.localDate(scheduleDate);
        List<Cd15ShiftConfig> configs = this.shiftConfigMapper.selectList(
                new LambdaQueryWrapper<Cd15ShiftConfig>()
                        .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd15ShiftConfig::getIsActive, ACTIVE));
        Cd15ShiftDescriptor shift = this.shiftWindowResolver.resolve(localScheduleDate, configs).stream()
                .filter(item -> item.getClassIndex() >= safeClassIndex)
                .findFirst()
                .orElse(null);
        if (shift == null) {
            return null;
        }
        return Cd15RollingTarget.builder()
                .factoryCode(factoryCode)
                .scheduleDate(localScheduleDate)
                .batchNo(batchNo)
                .targetShiftCode(shift.getShiftCode())
                .targetClassField(shift.getClassField())
                .targetClassIndex(shift.getClassIndex())
                .handoverTime(shift.getStartTime())
                .build();
    }

    private int lastPositiveClassIndex(List<Cd15ScheduleResult> results) {
        return results == null ? 0 : results.stream()
                .filter(Objects::nonNull)
                .mapToInt(this::lastPositiveClassIndex)
                .max()
                .orElse(0);
    }

    private int lastPositiveClassIndex(Cd15ScheduleResult result) {
        if (result == null) {
            return 0;
        }
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> this.readDouble(result, String.format("class%dPlanQty", classIndex)) != null
                        && this.readDouble(result, String.format("class%dPlanQty", classIndex)) > 0D)
                .max()
                .orElse(0);
    }

    private int resolveAgingPeriodHours(String factoryCode) {
        Cd15Params param = paramsMapper.selectOne(new LambdaQueryWrapper<Cd15Params>()
                .eq(Cd15Params::getFactoryCode, factoryCode)
                .eq(Cd15Params::getParamCode, AGING_PERIOD_PARAM_CODE)
                .last("limit 1"));
        if (param == null || param.getParamValue() == null || param.getParamValue().trim().isEmpty()) {
            return DEFAULT_AGING_PERIOD_HOURS;
        }
        try {
            return Math.max(0, Integer.parseInt(param.getParamValue().trim()));
        } catch (NumberFormatException exception) {
            return DEFAULT_AGING_PERIOD_HOURS;
        }
    }

    private LocalDate localDate(Date value) {
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void saveLog(String taskId, Cd15ScheduleResult result, String logType,
                         String reasonCode, Object request, String beforeJson) {
        Cd15ScheduleResultLog log = new Cd15ScheduleResultLog();
        log.setScheduleResultId(result.getId());
        log.setTaskId(taskId);
        log.setLogType(logType);
        log.setLogTime(new Date());
        log.setReasonCode(reasonCode);
        log.setReasonDetail(this.reasonDetail(request, result));
        log.setFactoryCode(result.getFactoryCode());
        log.setScheduleDate(result.getScheduleDate());
        log.setCxBatchNo(result.getCxBatchNo());
        log.setBatchNo(result.getCd15BatchNo());
        log.setOrderNo(result.getOrderNo());
        log.setGroupNo(result.getGroupNo());
        log.setBigRollCode(result.getBigRollCode());
        log.setSteelStripCode(result.getSteelStripCode());
        log.setCuttingAngle(result.getCuttingAngle());
        log.setMachineCode(result.getMachineCode());
        log.setStorageLaneCode(result.getStorageLaneCode());
        log.setSourceType(result.getSourceType());
        log.setReleaseStatus(result.getReleaseStatus());
        log.setProductionStatus(result.getProductionStatus());
        log.setBeforeJson(beforeJson);
        log.setAfterJson(this.json(result));
        log.setChangeReason(reasonCode);
        if (logMapper.insert(log) != 1) {
            throw new IllegalStateException("保存CD15人工调整日志失败");
        }
    }

    private String resolveBatchNo(String factoryCode, Date scheduleDate) {
        List<Cd15ScheduleResult> results = resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleResult::getScheduleDate, scheduleDate)
                .orderByDesc(Cd15ScheduleResult::getCreateTime));
        return results.stream().map(Cd15ScheduleResult::getCd15BatchNo)
                .filter(this::hasText).findFirst()
                .orElseGet(() -> "CD15" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
    }

    private String firstBatchNo(List<Cd15ScheduleResult> results) {
        return results.stream().map(Cd15ScheduleResult::getCd15BatchNo)
                .filter(this::hasText).findFirst().orElse(null);
    }

    private String reasonDetail(Object request, Cd15ScheduleResult result) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("request", request);
        detail.put("result", result);
        return this.json(detail);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("CD15人工调整日志序列化失败", exception);
        }
    }

    private Double readInsertPlanQty(Cd15InsertOrderRequest request, int classIndex) {
        return (Double) request.getFieldValueByFieldName(String.format("class%dPlanQty", classIndex));
    }

    private Integer readInsertProduceOrder(Cd15InsertOrderRequest request, int classIndex) {
        return (Integer) request.getFieldValueByFieldName(String.format("class%dProduceOrder", classIndex));
    }

    private String readInsertAnalysisInput(Cd15InsertOrderRequest request, int classIndex) {
        return (String) request.getFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex));
    }

    private Integer readTransferProduceOrder(Cd15TransferMachineRequest request, int classIndex) {
        Integer produceOrder = (Integer) request.getFieldValueByFieldName(String.format("class%dProduceOrder", classIndex));
        return produceOrder != null && produceOrder > 0 ? produceOrder : null;
    }

    private Double readDouble(Cd15ScheduleResult result, String fieldName) {
        Object value = result.getFieldValueByFieldName(fieldName);
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private int parseClassIndex(String classField) {
        try {
            int classIndex = Integer.parseInt(classField.replace("CLASS", ""));
            if (classIndex < 1 || classIndex > CLASS_COUNT) {
                throw new NumberFormatException("class index out of range");
            }
            return classIndex;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("班次必须为CLASS1至CLASS8", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
