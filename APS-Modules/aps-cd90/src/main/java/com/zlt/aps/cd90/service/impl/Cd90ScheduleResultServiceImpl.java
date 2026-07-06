package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleRollingAdjustLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90TransferMachineRequest;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskType;
import com.zlt.aps.cd90.engine.model.Cd90BatchDataCheckResult;
import com.zlt.aps.cd90.engine.model.Cd90InsertCarryoverImpact;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleBatchDataValidator;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90InsertRollingService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleRollingAdjustLogMapper;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.model.Cd90ScheduleOverwriteDecision;
import com.zlt.aps.cd90.service.Cd90AutoScheduleAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90InsertOrderAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90ScheduleOverwriteValidator;
import com.zlt.aps.cd90.service.Cd90TimedRollingCheckService;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheduleResultServiceImpl extends AbstractDocService<Cd90ScheduleResult> implements ICd90ScheduleResultService {

    @Resource
    private Cd90ScheduleResultMapper cd90ScheduleResultMapper;
    @Resource
    private Cd90ScheduleOverwriteValidator overwriteValidator;
    @Resource
    private Cd90ScheduleTaskService taskService;
    @Resource
    private Cd90AutoScheduleAsyncExecutor asyncExecutor;
    @Resource
    private Cd90AutoScheduleBatchDataValidator batchDataValidator;
    @Resource
    private Cd90AutoScheduleShiftMapper shiftMapper;
    @Resource
    private Cd90InsertOrderAsyncExecutor insertOrderAsyncExecutor;
    @Resource
    private Cd90ScheduleRollingAdjustLogMapper rollingAdjustLogMapper;
    @Resource
    private Cd90UnscheduleResultMapper unscheduleResultMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private Cd90TimedRollingCheckService timedRollingCheckService;
    @Resource
    private Cd90EngineConstructionMapper constructionMapper;
    @Resource
    private Cd90InsertRollingService insertRollingService;
    @Resource
    private Cd90AutoScheduleLockService lockService;

    /**
     * 接收自动排程请求。
     * 排程算法统一由Aps-Engine中的直裁引擎实现，本服务只负责业务接口转发。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 接口调用成功
     */
    @Override
    public AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return AjaxResult.error("自动排程请求不能为空");
        }
        if (scheduleResult.getFactoryCode() == null || scheduleResult.getFactoryCode().trim().isEmpty()
                || scheduleResult.getScheduleDate() == null) {
            return AjaxResult.error("自动排程工厂编码和排程日期不能为空");
        }
        // 正式进入自动排程前，同步做1.2节批次级数据先行检查；
        // 失败时不创建PENDING任务、不占用执行锁、不进入异步执行器，直接返回结构化错误。
        LocalDate localScheduleDate = scheduleResult.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                scheduleResult.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            // 走success+batchCheckFailed标记，避免HTTP 500被前端拦截器拦截且丢失data；
            // 与needConfirm模式一致，由前端按data.batchCheckFailed分流渲染结构化错误。
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", false);
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        List<Cd90ScheduleResult> existing = cd90ScheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate, scheduleResult.getScheduleDate()));
        Cd90ScheduleOverwriteDecision decision = overwriteValidator.validate(existing,
                Boolean.TRUE.equals(scheduleResult.getForceRegenerate()));
        if (decision.isRejected()) {
            return AjaxResult.error(decision.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        if (decision.isNeedConfirm()) {
            data.put("needConfirm", true);
            return AjaxResult.success(decision.getMessage(), data);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate());
        if (activeTask != null) {
            data.put("needConfirm", false);
            data.put("taskId", activeTask.getTaskId());
            return AjaxResult.success("当前日期已有自动排程任务正在执行", data);
        }
        String snapshot = "factoryCode=" + scheduleResult.getFactoryCode()
                + ",scheduleDate=" + scheduleResult.getScheduleDate()
                + ",forceRegenerate=" + Boolean.TRUE.equals(scheduleResult.getForceRegenerate());
        Cd90ScheduleTask task = taskService.createPending(scheduleResult.getFactoryCode(),
                scheduleResult.getScheduleDate(), Cd90ScheduleTaskType.AUTO_SCHEDULE,
                "MANUAL", snapshot, null);
        asyncExecutor.execute(task.getTaskId(), task.getFactoryCode(), task.getScheduleDate());
        data.put("needConfirm", false);
        data.put("taskId", task.getTaskId());
        return AjaxResult.success("自动排程任务已提交", data);
    }

    @Override
    public AjaxResult shiftDates(Cd90InsertOrderRequest request) {
        if (request == null || request.getScheduleDate() == null
                || request.getFactoryCode() == null || request.getFactoryCode().trim().isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        LocalDate scheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        List<Map<String, Object>> values = shiftMapper.selectList(
                        new LambdaQueryWrapper<Cd90ShiftConfig>()
                                .eq(Cd90ShiftConfig::getFactoryCode, request.getFactoryCode())
                                .eq(Cd90ShiftConfig::getIsActive, 1))
                .stream()
                .sorted(Comparator.comparing(Cd90ShiftConfig::getScheduleDay)
                        .thenComparing(Cd90ShiftConfig::getDayShiftOrder)
                        .thenComparing(Cd90ShiftConfig::getShiftOrder))
                .map(config -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("classField", config.getClassField());
                    item.put("shiftCode", config.getShiftCode());
                    item.put("shiftName", config.getShiftName());
                    item.put("shiftDate", scheduleDate.plusDays(config.getScheduleDay() - 2L)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE));
                    return item;
                }).collect(Collectors.toList());
        return AjaxResult.success(values);
    }

    @Override
    public AjaxResult validateInsert(Cd90InsertOrderRequest request) {
        if (request == null || request.getScheduleDate() == null
                || isBlank(request.getFactoryCode()) || isBlank(request.getMachineCode())
                || isBlank(request.getClothCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        boolean hasPlan = false;
        List<Cd90ScheduleResult> existing = this.selectByDateAndFactory(
                request.getScheduleDate(), request.getFactoryCode());
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            Double planQuantity = (Double) request.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            Integer produceOrder = (Integer) request.getFieldValueByFieldName(
                    String.format("class%dProduceOrder", classIndex));
            boolean positivePlan = planQuantity != null && planQuantity > 0D;
            if (positivePlan != (produceOrder != null && produceOrder > 0)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.pairRequired"));
            }
            if (!positivePlan) {
                continue;
            }
            hasPlan = true;
            int finalClassIndex = classIndex;
            int finalClassIndex1 = classIndex;
            int highestLockedOrder = existing.stream()
                    .filter(item -> request.getMachineCode().equals(item.getMachineCode()))
                    .filter(item -> isLocked(item, finalClassIndex))
                    .map(item -> readProduceOrder(item, finalClassIndex1))
                    .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
            if (produceOrder <= highestLockedOrder) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.lockedPrefix"));
            }
            int finalClassIndex2 = classIndex;
            boolean duplicateSegment = existing.stream()
                    .filter(item -> request.getMachineCode().equals(item.getMachineCode()))
                    .filter(item -> request.getClothCode().equals(item.getClothCode()))
                    .anyMatch(item -> readPlanQuantity(item, finalClassIndex2) > 0D);
            if (duplicateSegment) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.duplicateSegment"));
            }
        }
        return hasPlan ? AjaxResult.success()
                : AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.planRequired"));
    }

    @Override
    public AjaxResult insertOrder(Cd90InsertOrderRequest request) {
        AjaxResult validation = this.validateInsert(request);
        if (!Integer.valueOf(200).equals(validation.get("code"))) {
            return validation;
        }
        // 创建INSERT_ORDER异步任务前，复用自动排程1.2节批次级数据先行检查；
        // 失败时不创建PENDING任务、不占用执行锁、不进入异步执行器，
        // 与autoSchedule一致返回success+batchCheckFailed结构化错误，由前端渲染。
        LocalDate localScheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                request.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new HashMap<>();
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        // 追加针对插窗帘布的 TIRE_FABRIC_LENGTH/TIRE_FABRIC_CRAFT 检查。
        // batchDataValidator.check 基于成型计划胚号+版本维度校验施工，
        // 插单以单独帘布代号指定，需按该帘布代号兜底校验施工层位中的直裁宽度和单耗。
        Map<String, Object> clothCheckResult = checkInsertClothTireFabric(request.getFactoryCode(), request.getClothCode());
        if (clothCheckResult != null) {
            return AjaxResult.success("帘布 " + request.getClothCode() + " 施工数据检查失败", clothCheckResult);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewInsertOrder(request, localScheduleDate);
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd90ScheduleTask task = taskService.createPending(request.getFactoryCode(),
                request.getScheduleDate(), Cd90ScheduleTaskType.INSERT_ORDER,
                "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.execute(task.getTaskId(), request);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        return AjaxResult.success(I18nUtil.getMessage("ui.cd90.insert.submitted"), data);
    }

    /**
     * 使用正式滚动内核执行只读预演，跨班顺延时返回确认明细。
     */
    private AjaxResult previewInsertOrder(Cd90InsertOrderRequest request,
                                          LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            if (taskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            Cd90InsertRollingOutput output = insertRollingService.execute(request);
            List<Cd90InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success(
                    I18nUtil.getMessage("ui.cd90.insert.carryoverConfirm"), data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 将引擎影响模型转换为前端确认结构。 */
    private Map<String, Object> toCarryoverDetail(Cd90InsertCarryoverImpact impact) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("clothCode", impact.getClothCode());
        detail.put("affectedType", impact.getAffectedType());
        detail.put("sourceClassField", impact.getSourceClassField());
        detail.put("targetClassField", impact.getTargetClassField());
        detail.put("carryoverQty", impact.getCarryoverQty());
        detail.put("reasonCode", impact.getReasonCode());
        detail.put("reasonMessage", this.resolveCarryoverReason(impact.getReasonCode()));
        return detail;
    }

    /** 将滚动限制原因转换为用户可理解的国际化说明。 */
    private String resolveCarryoverReason(String reasonCode) {
        if ("CAPACITY_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.capacityLimit");
        }
        if ("STORAGE_LANE_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.storageLaneLimit");
        }
        if ("ROLL_TOOL_LIMIT".equals(reasonCode) || "TOOLING_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.toolingLimit");
        }
        if ("BIG_ROLL_STOCK_DATA_MISSING".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.bigRollStockDataMissing");
        }
        if ("CONSTRUCTION_MISSING".equals(reasonCode) || "DATA_MISSING".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.constructionMissing");
        }
        if ("AGING_PERIOD_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.agingPeriodLimit");
        }
        if ("SCHEDULE_WINDOW_LIMIT".equals(reasonCode)) {
            return I18nUtil.getMessage("ui.cd90.insert.reason.scheduleWindowLimit");
        }
        return I18nUtil.getMessage("ui.cd90.insert.reason.other");
    }

    @Override
    public AjaxResult getInsertTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.INSERT_ORDER.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.taskNotFound"));
        }
        return AjaxResult.success(task);
    }



    @Override
    public AjaxResult validateTransferMachine(Cd90TransferMachineRequest request) {
        if (request == null || request.getScheduleDate() == null
                || isBlank(request.getFactoryCode()) || isBlank(request.getSourceMachineCode())
                || isBlank(request.getTargetMachineCode()) || isBlank(request.getClothCode())
                || isBlank(request.getStartClassField())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.required"));
        }
        if (request.getSourceMachineCode().equals(request.getTargetMachineCode())) {
            return AjaxResult.error("原机台和目标机台不能相同");
        }
        int startClassIndex;
        try {
            startClassIndex = Integer.parseInt(request.getStartClassField().replace("CLASS", ""));
        } catch (NumberFormatException exception) {
            return AjaxResult.error("开始班次必须为CLASS1至CLASS6");
        }
        if (startClassIndex < 1 || startClassIndex > 6) {
            return AjaxResult.error("开始班次必须为CLASS1至CLASS6");
        }
        List<Cd90ScheduleResult> existing = this.selectByDateAndFactory(
                request.getScheduleDate(), request.getFactoryCode());
        List<Cd90ScheduleResult> transferPlans = existing.stream()
                .filter(item -> request.getSourceMachineCode().equals(item.getMachineCode()))
                .filter(item -> request.getClothCode().equals(item.getClothCode()))
                .collect(Collectors.toList());
        boolean hasTransferPlan = transferPlans.stream()
                .anyMatch(item -> IntStream.rangeClosed(startClassIndex, 6)
                        .anyMatch(classIndex -> readPlanQuantity(item, classIndex) > 0D));
        if (!hasTransferPlan) {
            return AjaxResult.error("原机台从起始班次开始没有可转走的帘布计划");
        }
        boolean missingProduceOrder = transferPlans.stream()
                .anyMatch(item -> IntStream.rangeClosed(startClassIndex, 6)
                        .anyMatch(classIndex -> readPlanQuantity(item, classIndex) > 0D
                                && readTransferProduceOrder(request, classIndex) == null));
        return missingProduceOrder ? AjaxResult.error("转机台目标顺序不能为空") : AjaxResult.success();
    }

    @Override
    public AjaxResult transferMachine(Cd90TransferMachineRequest request) {
        AjaxResult validation = this.validateTransferMachine(request);
        if (!Integer.valueOf(200).equals(validation.get("code"))) {
            return validation;
        }
        LocalDate localScheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                request.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            Map<String, Object> data = new HashMap<>();
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                request.getFactoryCode(), request.getScheduleDate());
        if (activeTask != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
        }
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            AjaxResult previewResult = this.previewTransferMachine(request, localScheduleDate);
            if (previewResult != null) {
                return previewResult;
            }
        }
        Cd90ScheduleTask task = taskService.createPending(request.getFactoryCode(),
                request.getScheduleDate(), Cd90ScheduleTaskType.TRANSFER_MACHINE,
                "MANUAL", request.toString(), null);
        insertOrderAsyncExecutor.executeTransfer(task.getTaskId(), request);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        return AjaxResult.success("转机台任务已提交", data);
    }

    private AjaxResult previewTransferMachine(Cd90TransferMachineRequest request,
                                              LocalDate scheduleDate) {
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            if (taskService.findActive(request.getFactoryCode(), request.getScheduleDate()) != null) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.activeTask"));
            }
            Cd90InsertRollingOutput output = insertRollingService.executeTransfer(request);
            List<Cd90InsertCarryoverImpact> impacts = output.getCarryoverImpacts() == null
                    ? Collections.emptyList() : output.getCarryoverImpacts();
            if (impacts.isEmpty()) {
                return null;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", true);
            data.put("carryoverDetails", impacts.stream()
                    .map(this::toCarryoverDetail)
                    .collect(Collectors.toList()));
            return AjaxResult.success("转机台会引起跨班顺延，请确认后继续", data);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public AjaxResult getTransferMachineTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.TRANSFER_MACHINE.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.insert.taskNotFound"));
        }
        return AjaxResult.success(task);
    }

    @Override
    public AjaxResult checkTimedRolling(Cd90RollingCheckRequest request) {
        return timedRollingCheckService.check(request);
    }

    @Override
    public AjaxResult getTimedRollingTask(String taskId) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.ROLLING_SCHEDULE.equals(task.getTaskType())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.rolling.taskNotFound"));
        }
        Map<String, Object> response = objectMapper.convertValue(task, Map.class);
        String targetShiftCode = null;
        String inputVersion = null;
        if (!isBlank(task.getRequestSnapshot())) {
            try {
                JsonNode snapshot = objectMapper.readTree(task.getRequestSnapshot());
                inputVersion = snapshot.path("inputVersion").asText(null);
                targetShiftCode = snapshot.path("target")
                        .path("targetShiftCode").asText(null);
            } catch (Exception exception) {
                response.put("snapshotParseError", true);
            }
        }
        Number adjustedCount = rollingAdjustLogMapper.selectCount(
                new LambdaQueryWrapper<Cd90ScheduleRollingAdjustLog>()
                        .eq(Cd90ScheduleRollingAdjustLog::getTaskId,
                                task.getTaskId()));
        Number unscheduledCount = 0;
        if (!isBlank(task.getBatchNo())) {
            unscheduledCount = unscheduleResultMapper.selectCount(
                    new LambdaQueryWrapper<Cd90UnscheduleResult>()
                            .eq(Cd90UnscheduleResult::getFactoryCode, task.getFactoryCode())
                            .eq(Cd90UnscheduleResult::getScheduleDate, task.getScheduleDate())
                            .eq(Cd90UnscheduleResult::getBatchNo, task.getBatchNo()));
        }
        response.put("targetShiftCode", targetShiftCode);
        response.put("inputVersion", inputVersion);
        response.put("sourceBatchNo", task.getBatchNo());
        response.put("adjustedCount", adjustedCount);
        response.put("unscheduledCount", unscheduledCount);
        return AjaxResult.success(response);
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isLocked(Cd90ScheduleResult result, int classIndex) {
        Double finishQuantity = readDouble(result, String.format("class%dFinishQty", classIndex));
        Double planQuantity = readDouble(result, String.format("class%dPlanQty", classIndex));
        return Integer.valueOf(1).equals(result.getIsLocked())
                || (finishQuantity != null && finishQuantity > 0D)
                || ("1".equals(result.getProductionStatus())
                && planQuantity != null && (finishQuantity == null || finishQuantity < planQuantity));
    }

    private Integer readProduceOrder(Cd90ScheduleResult result, int classIndex) {
        return (Integer) result.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
    }

    private double readPlanQuantity(Cd90ScheduleResult result, int classIndex) {
        Double value = readDouble(result, String.format("class%dPlanQty", classIndex));
        return value == null ? 0D : value;
    }

    private Integer readTransferProduceOrder(Cd90TransferMachineRequest request, int classIndex) {
        Integer produceOrder = (Integer) request.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
        return produceOrder != null && produceOrder > 0 ? produceOrder : null;
    }

    private Double readDouble(Cd90ScheduleResult result, String fieldName) {
        return (Double) result.getFieldValueByFieldName(fieldName);
    }

    @Override
    protected String getDocTypeCode() { return "CD90_SCHEDULE_RESULT"; }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_SCHEDULE_RESULT");
        return sysDocType;
    }

    /** 将批次级检查错误列表转为前端可渲染的List<Map>结构。 */
    private List<Map<String, Object>> toErrorList(List<Cd90BatchDataCheckResult.CheckError> errors) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (errors == null) {
            return result;
        }
        for (Cd90BatchDataCheckResult.CheckError error : errors) {
            Map<String, Object> item = new HashMap<>();
            item.put("field", error.getField());
            item.put("reasonCode", error.getReasonCode());
            item.put("message", error.getMessage());
            item.put("suggestion", error.getSuggestion());
            result.add(item);
        }
        return result;
    }

    /**
     * 检查插窗帘布的 TIRE_FABRIC_CRAFT 和 TIRE_FABRIC_LENGTH 施工数据。
     * 以帘布代号查询施工信息中匹配的层位，校验直裁宽度和单耗均存在且为正。
     *
     * @param factoryCode 工厂编码
     * @param clothCode   帘布代号
     * @return 失败时返回包含 batchCheckFailed 等的 Map（与前端的 batchCheckFailed=true 协定对齐），通过时返回 null
     */
    private Map<String, Object> checkInsertClothTireFabric(String factoryCode, String clothCode) {
        if (isBlank(clothCode)) {
            return null;
        }
        List<MdmConstructionInfo> constructions = constructionMapper.selectList(
                Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .and(w -> w.eq(MdmConstructionInfo::getTireFabricCode1, clothCode)
                                .or().eq(MdmConstructionInfo::getTireFabricCode2, clothCode)
                                .or().eq(MdmConstructionInfo::getTireFabricCode3, clothCode)));
        List<Map<String, Object>> errors = new ArrayList<>();
        boolean clothFound = false;
        if (constructions != null) {
            for (MdmConstructionInfo construction : constructions) {
                String prefix = "胎胚 " + construction.getConstructionCode()
                        + " 施工版本 " + construction.getConstructionVersion() + " ";
                for (int layer = 1; layer <= 3; layer++) {
                    String layerClothCode = getMdmLayerClothCode(construction, layer);
                    if (!clothCode.equals(layerClothCode)) {
                        continue;
                    }
                    clothFound = true;
                    // 检查 TIRE_FABRIC_CRAFT{n}
                    String craftRaw = getMdmLayerCraftRaw(construction, layer);
                    if (!isPositiveDecimal(craftRaw)) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("field", "施工信息");
                        error.put("reasonCode", "DATA_MISSING");
                        error.put("message", prefix + "第 " + layer + " 层帘布 " + clothCode + " 直裁宽度缺失或非正");
                        error.put("suggestion", "请在施工信息页面维护 TIRE_FABRIC_CRAFT" + layer + " 且大于0");
                        errors.add(error);
                    }
                    // 检查 TIRE_FABRIC_LENGTH{n}
                    BigDecimal length = getMdmLayerLength(construction, layer);
                    if (length == null || length.signum() <= 0) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("field", "施工信息");
                        error.put("reasonCode", "DATA_MISSING");
                        error.put("message", prefix + "第 " + layer + " 层帘布 " + clothCode + " 单耗缺失或非正");
                        error.put("suggestion", "请在施工信息页面维护 TIRE_FABRIC_LENGTH" + layer + " 且大于0");
                        errors.add(error);
                    }
                }
            }
        }
        if (!clothFound) {
            Map<String, Object> error = new HashMap<>();
            error.put("field", "施工信息");
            error.put("reasonCode", "DATA_MISSING");
            error.put("message", "帘布 " + clothCode + " 未在任何施工信息中找到");
            error.put("suggestion", "请检查帘布代号维护是否正确");
            errors.add(error);
        }
        if (!errors.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("batchCheckFailed", true);
            result.put("errors", errors);
            result.put("warnings", new ArrayList<>());
            return result;
        }
        return null;
    }

    /** 取施工记录指定层位的帘布代号（1=TIRE_FABRIC_CODE1, 2=TIRE_FABRIC_CODE2, 3=TIRE_FABRIC_CODE3）。 */
    private String getMdmLayerClothCode(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricCode1();
            case 2: return construction.getTireFabricCode2();
            case 3: return construction.getTireFabricCode3();
            default: return null;
        }
    }

    /** 取施工记录指定层位的直裁宽度原始值（TIRE_FABRIC_CRAFT1/2/3）。 */
    private String getMdmLayerCraftRaw(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricCraft1();
            case 2: return construction.getTireFabricCraft2();
            case 3: return construction.getTireFabricCraft3();
            default: return null;
        }
    }

    /** 取施工记录指定层位的单耗（TIRE_FABRIC_LENGTH1/2/3）。 */
    private BigDecimal getMdmLayerLength(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricLength1();
            case 2: return construction.getTireFabricLength2();
            case 3: return construction.getTireFabricLength3();
            default: return null;
        }
    }

    /** 判断字符串是否为正数（可解析为 >0 的数值）。 */
    private boolean isPositiveDecimal(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        try {
            return new BigDecimal(raw.trim()).signum() > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<Cd90ScheduleResult> selectByDateAndFactory(Date scheduleDate, String factoryCode) {
        if (scheduleDate == null || factoryCode == null || factoryCode.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Cd90ScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90ScheduleResult::getScheduleDate, scheduleDate)
                .eq(Cd90ScheduleResult::getFactoryCode, factoryCode);
        return cd90ScheduleResultMapper.selectList(wrapper);
    }

    @Override
    public List<Cd90ScheduleResult> getCd90ScheduleResultListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Cd90ScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Cd90ScheduleResult::getId, ids);
        return cd90ScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 批量更新发布状态。REQUIRES_NEW 独立短事务：
     * 即便外层 MES 调用 try 块抛异常，失败状态回写也能独立提交，避免状态丢失。
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int batchUpdateReleaseStatus(List<Cd90ScheduleResult> list, String targetStatus) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        for (Cd90ScheduleResult entity : list) {
            entity.setIsRelease(targetStatus);
            if (ApsConstant.IS_RELEASE.equals(targetStatus)) {
                entity.setPublishSuccessCount(
                        Optional.ofNullable(entity.getPublishSuccessCount()).orElse(0) + 1);
                entity.setNewestPublishTime(now);
            }
        }
        this.baseDao.updateBatch(list);
        return list.size();
    }
}
