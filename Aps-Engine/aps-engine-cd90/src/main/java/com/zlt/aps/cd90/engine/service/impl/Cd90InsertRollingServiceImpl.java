package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.api.domain.vo.Cd90ChangeQtyRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90TransferMachineRequest;
import com.zlt.aps.cd90.engine.algorithm.Cd90MachineCapacityCalculator;
import com.zlt.aps.cd90.engine.algorithm.Cd90RollingScheduleContextManager;
import com.zlt.aps.cd90.engine.algorithm.Cd90StorageLaneAllocator;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineInfoMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleResultMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleLaneAllocationMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90ConstructionMaterialMapper;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90InsertCarryoverImpact;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;
import com.zlt.aps.cd90.engine.model.Cd90InsertLaneAllocationDraft;
import com.zlt.aps.cd90.engine.model.Cd90MachineCapacityTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import com.zlt.aps.cd90.engine.model.Cd90RollingPendingTask;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocationResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90InsertRollingService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基于现有班次和产能计算器执行插单滚动重排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90InsertRollingServiceImpl implements Cd90InsertRollingService {

    private final Cd90AutoScheduleEngineService autoScheduleEngineService;
    private final Cd90EngineScheduleResultMapper scheduleResultMapper;
    private final Cd90EngineScheduleLaneAllocationMapper laneAllocationMapper;
    private final Cd90EngineMachineInfoMapper machineInfoMapper;
    private final Cd90EngineConstructionMapper constructionMapper;
    private final Cd90ConstructionMaterialMapper constructionMaterialMapper;
    private final Cd90MachineCapacityCalculator capacityCalculator;
    private final Cd90AutoScheduleInputService inputService;
    private final Cd90ShiftDemandProvider demandProvider;
    private final Cd90RollingScheduleContextManager rollingContextManager;
    private final Cd90StorageLaneAllocator laneAllocator;

    @Override
    public Cd90InsertRollingOutput execute(Cd90InsertOrderRequest request) {
        return this.executeInternal(request, null, null);
    }

    @Override
    public Cd90InsertRollingOutput executeTransfer(Cd90TransferMachineRequest request) {
        Cd90InsertOrderRequest insertRequest = new Cd90InsertOrderRequest();
        insertRequest.setFactoryCode(request.getFactoryCode());
        insertRequest.setScheduleDate(request.getScheduleDate());
        insertRequest.setMachineCode(request.getTargetMachineCode());
        insertRequest.setClothCode(request.getClothCode());
        insertRequest.setRemark(request.getRemark());
        insertRequest.setConfirmed(request.getConfirmed());
        return this.executeInternal(insertRequest, request, null);
    }

    @Override
    public Cd90InsertRollingOutput executeChangeQty(Cd90ChangeQtyRequest request) {
        Cd90InsertOrderRequest rollingRequest = new Cd90InsertOrderRequest();
        rollingRequest.setFactoryCode(request.getFactoryCode());
        rollingRequest.setScheduleDate(request.getScheduleDate());
        rollingRequest.setMachineCode(request.getMachineCode());
        rollingRequest.setClothCode(request.getClothCode());
        rollingRequest.setRemark(request.getRemark());
        rollingRequest.setConfirmed(request.getConfirmed());
        return this.executeInternal(rollingRequest, null, request);
    }

    private Cd90InsertRollingOutput executeInternal(Cd90InsertOrderRequest request,
                                                    Cd90TransferMachineRequest transferRequest,
                                                    Cd90ChangeQtyRequest changeQtyRequest) {
        Cd90AutoScheduleContext context = autoScheduleEngineService.prepare(
                request.getFactoryCode(), request.getScheduleDate());
        List<Cd90ScheduleResult> sourceResults = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode, request.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate, request.getScheduleDate()));
        if (sourceResults.isEmpty()) {
            throw new IllegalStateException("当前排程日期没有可供插单的原排程结果");
        }
        // 只使用最新批次的排程结果，避免多批次共存时重复处理同一帘布。
        String latestBatchNo = sourceResults.stream()
                .map(Cd90ScheduleResult::getBatchNo).filter(Objects::nonNull)
                .max(String::compareTo)
                .orElseThrow(() -> new IllegalStateException("原排程结果缺少批次号"));
        sourceResults = sourceResults.stream()
                .filter(item -> latestBatchNo.equals(item.getBatchNo()))
                .collect(Collectors.toList());
        String batchNo = sourceResults.stream().map(Cd90ScheduleResult::getBatchNo)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("原排程结果缺少批次号"));
        List<Cd90ScheduleResult> workingResults = sourceResults.stream()
                .map(this::copyResult).collect(Collectors.toList());
        List<Long> sourceResultIds = sourceResults.stream().map(Cd90ScheduleResult::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<Cd90ScheduleLaneAllocation> sourceLaneAllocations = sourceResultIds.isEmpty()
                ? Collections.emptyList() : laneAllocationMapper.selectList(
                        new LambdaQueryWrapper<Cd90ScheduleLaneAllocation>()
                                .in(Cd90ScheduleLaneAllocation::getScheduleResultId, sourceResultIds));
        Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanesByResult = sourceLaneAllocations.stream()
                .collect(Collectors.groupingBy(Cd90ScheduleLaneAllocation::getScheduleResultId));
        Map<Long, List<Cd90InsertLaneAllocationDraft>> replacementLanes = sourceLaneAllocations.stream()
                .collect(Collectors.groupingBy(Cd90ScheduleLaneAllocation::getScheduleResultId,
                        LinkedHashMap::new, Collectors.mapping(this::copyLaneDraft, Collectors.toList())));
        Map<Long, Cd90ScheduleResult> changedById = new LinkedHashMap<>();
        Map<Long, Cd90ScheduleResult> deletedById = new LinkedHashMap<>();
        ChangeQtyPlan changeQtyPlan = null;
        if (changeQtyRequest != null) {
            changeQtyPlan = this.prepareChangeQtySource(changeQtyRequest, request, workingResults);
        }
        if (transferRequest != null) {
            this.prepareTransferSource(transferRequest, request, workingResults,
                    changedById, deletedById, replacementLanes);
        }
        List<Cd90InsertLaneAllocationDraft> insertLanes = new ArrayList<>();
        Cd90ConstructionMaterial insertMaterial = findInsertMaterial(request);
        Cd90ScheduleResult insertResult = changeQtyPlan == null
                ? newInsertResult(request, batchNo, insertMaterial)
                : changeQtyPlan.targetResult;
        Cd90MachineInfo machine = machineInfoMapper.selectOne(
                new LambdaQueryWrapper<Cd90MachineInfo>()
                        .eq(Cd90MachineInfo::getFactoryCode, request.getFactoryCode())
                        .eq(Cd90MachineInfo::getMachineCode, request.getMachineCode())
                        .eq(Cd90MachineInfo::getStatus, "1")
                        .last("limit 1"));
        if (machine == null || machine.getQuota() == null || machine.getQuota() <= 0D) {
            throw new IllegalStateException("插单机台不存在、未启用或定额未维护");
        }

        List<Cd90RollingPendingTask> carryovers = new ArrayList<>();
        Map<String, Cd90MachineTailState> tailByMachine = new HashMap<>();
        List<Cd90InsertCarryoverImpact> carryoverImpacts = new ArrayList<>();
        Cd90RollingScheduleContext rollingResources = null;
        List<Cd90ShiftDescriptor> shifts = context.getShifts();
        for (int shiftIndex = 0; shiftIndex < shifts.size(); shiftIndex++) {
            Cd90ShiftDescriptor shift = shifts.get(shiftIndex);
            int classIndex = classIndex(shift.getClassField());
            Cd90AutoScheduleInput input = inputService.load(context.getFactoryCode(),
                    context.getScheduleDate(), shift.getClassField(), shift.getShiftCode(),
                    context.getResourceBaselineDate(),
                    context.getResourceBaselineShiftCode(),
                    context.getParameters().getAgingPeriodHours());
            this.ensureInsertMaterial(input, insertMaterial);
            if (rollingResources == null) {
                rollingResources = rollingContextManager.initialize(input.getStorageLanesAtSix());
            }
            rollingContextManager.updateCumulativeConsumption(rollingResources,
                    demandProvider.cumulativeConsumptionByClothBeforeShift(context, input, shift));
            Cd90ShiftResourceState resourceState = rollingContextManager.openShift(
                    rollingResources, shift, buildCurlLengthByCloth(input, context),
                    context.getParameters().getRollCoilMeter(),
                    context.getParameters().getRollTotalCount(),
                    Collections.singletonList(request.getMachineCode()));
            Cd90MachineTailState inheritedTail = resourceState.getTailByMachine()
                    .get(request.getMachineCode());
            if (inheritedTail != null) {
                tailByMachine.put(request.getMachineCode(), inheritedTail);
            }
            boolean forceRollCurrentShift = changeQtyPlan != null
                    && changeQtyPlan.targetQtyByClass.containsKey(classIndex);
            if (forceRollCurrentShift) {
                this.clearChangeQtyTargetClass(changeQtyPlan, classIndex, changedById, replacementLanes);
            }
            Double insertQuantity = (Double) request.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            Integer insertOrder = (Integer) request.getFieldValueByFieldName(
                    String.format("class%dProduceOrder", classIndex));
            boolean hasInsert = insertQuantity != null && insertQuantity > 0D;
            boolean shouldRollCurrentShift = hasInsert || !carryovers.isEmpty() || forceRollCurrentShift;
            if (!shouldRollCurrentShift) {
                reserveUnaffectedTasks(resourceState, shift, classIndex, workingResults,
                        sourceLanesByResult, input, context, null);
                rollingContextManager.completeShift(rollingResources, resourceState);
                continue;
            }
            reserveUnaffectedTasks(resourceState, shift, classIndex, workingResults,
                    sourceLanesByResult, input, context, request.getMachineCode());
            ShiftRollingResult rollingResult = rollShift(context, shift, classIndex,
                    request, machine, workingResults, insertResult, carryovers,
                    hasInsert ? BigDecimal.valueOf(insertQuantity) : BigDecimal.ZERO,
                    insertOrder, changedById, tailByMachine, resourceState,
                    input, sourceLanesByResult, replacementLanes, insertLanes,
                    carryoverImpacts);
            carryovers = rollingResult.carryovers;
            rollingContextManager.completeShift(rollingResources, resourceState);
            log.info("[直裁插单] 班次滚动完成, classField={}, inserted={}, taskCount={}, carryoverCount={}",
                    shift.getClassField(), hasInsert, rollingResult.taskCount, carryovers.size());
        }
        if (changeQtyPlan != null) {
            carryoverImpacts.stream()
                    .filter(item -> "INSERT".equals(item.getAffectedType()))
                    .forEach(item -> item.setAffectedType("CHANGE_QTY"));
            List<Cd90InsertLaneAllocationDraft> changedLanes = changedById.keySet().stream()
                    .flatMap(id -> replacementLanes.getOrDefault(id, Collections.emptyList()).stream())
                    .collect(Collectors.toCollection(ArrayList::new));
            return Cd90InsertRollingOutput.builder().context(context).batchNo(batchNo)
                    .insertResult(null)
                    .updatedResults(new ArrayList<>(changedById.values()))
                    .deletedResults(new ArrayList<>(deletedById.values()))
                    .laneAllocations(changedLanes)
                    .unscheduledResults(this.toUnscheduled(request, batchNo, carryovers))
                    .carryoverImpacts(carryoverImpacts).build();
        }
        if (!this.hasScheduledQuantity(insertResult)) {
            List<Cd90RollingPendingTask> insertCarryovers = carryovers.stream()
                    .filter(Cd90RollingPendingTask::isHardInsert)
                    .collect(Collectors.toList());
            List<Cd90InsertCarryoverImpact> insertImpacts = carryoverImpacts.stream()
                    .filter(item -> "INSERT".equals(item.getAffectedType()))
                    .collect(Collectors.toList());
            List<Cd90InsertLaneAllocationDraft> changedLanes = changedById.keySet().stream()
                    .flatMap(id -> replacementLanes.getOrDefault(id, Collections.emptyList()).stream())
                    .collect(Collectors.toCollection(ArrayList::new));
            return Cd90InsertRollingOutput.builder().context(context).batchNo(batchNo)
                    .insertResult(insertResult)
                    .updatedResults(transferRequest == null ? Collections.emptyList()
                            : new ArrayList<>(changedById.values()))
                    .deletedResults(transferRequest == null ? Collections.emptyList()
                            : new ArrayList<>(deletedById.values()))
                    .laneAllocations(transferRequest == null ? Collections.emptyList() : changedLanes)
                    .unscheduledResults(this.toUnscheduled(request, batchNo, insertCarryovers))
                    .carryoverImpacts(insertImpacts).build();
        }
        List<Cd90InsertLaneAllocationDraft> affectedLanes = changedById.keySet().stream()
                .flatMap(id -> replacementLanes.getOrDefault(id, Collections.emptyList()).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        affectedLanes.addAll(insertLanes);
        return Cd90InsertRollingOutput.builder().context(context).batchNo(batchNo)
                .insertResult(insertResult)
                .updatedResults(new ArrayList<>(changedById.values()))
                .deletedResults(new ArrayList<>(deletedById.values()))
                .laneAllocations(affectedLanes)
                .unscheduledResults(this.toUnscheduled(request, batchNo, carryovers))
                .carryoverImpacts(carryoverImpacts).build();
    }

    /**
     * 调量先定位原排程结果，把指定班次的原计划量清空，再按原顺位写入目标量参与滚动。
     */
    private ChangeQtyPlan prepareChangeQtySource(Cd90ChangeQtyRequest changeQtyRequest,
                                                 Cd90InsertOrderRequest rollingRequest,
                                                 List<Cd90ScheduleResult> workingResults) {
        Cd90ScheduleResult targetResult = workingResults.stream()
                .filter(item -> changeQtyRequest.getScheduleResultId() == null
                        || Objects.equals(item.getId(), changeQtyRequest.getScheduleResultId()))
                .filter(item -> changeQtyRequest.getMachineCode().equals(item.getMachineCode()))
                .filter(item -> changeQtyRequest.getClothCode().equals(item.getClothCode()))
                .findFirst().orElseThrow(() -> new IllegalStateException("未找到可调量的直裁排程结果"));
        Map<Integer, BigDecimal> targetQtyByClass = this.resolveChangeQtyTargets(changeQtyRequest);
        targetQtyByClass.forEach((classIndex, targetQuantity) -> {
            if (this.isLocked(targetResult, classIndex)) {
                throw new IllegalStateException("已锁定或已生产的班次计划不能调量");
            }
            Double finishQty = (Double) targetResult.getFieldValueByFieldName(
                    String.format("class%dFinishQty", classIndex));
            if (finishQty != null && BigDecimal.valueOf(finishQty).compareTo(targetQuantity) > 0) {
                throw new IllegalStateException("调量目标不能小于已完成数量");
            }
            Integer produceOrder = this.readOrder(targetResult, classIndex);
            if (produceOrder == null || produceOrder <= 0) {
                produceOrder = this.nextProduceOrder(workingResults,
                        changeQtyRequest.getMachineCode(), classIndex);
            }
            rollingRequest.setFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex), targetQuantity.doubleValue());
            rollingRequest.setFieldValueByFieldName(
                    String.format("class%dProduceOrder", classIndex), produceOrder);
            rollingRequest.setFieldValueByFieldName(
                    String.format("class%dAnalysisInput", classIndex), "调量");
        });
        return new ChangeQtyPlan(targetResult, targetQtyByClass);
    }

    /** 解析调量目标量，支持单班字段和逐班字段两种入参。 */
    private Map<Integer, BigDecimal> resolveChangeQtyTargets(Cd90ChangeQtyRequest request) {
        Map<Integer, BigDecimal> targetQtyByClass = new LinkedHashMap<>();
        if (request.getStartClassField() != null && request.getTargetPlanQty() != null) {
            targetQtyByClass.put(this.classIndex(request.getStartClassField()),
                    BigDecimal.valueOf(request.getTargetPlanQty()));
        }
        IntStream.rangeClosed(1, 6).forEach(classIndex -> {
            Double planQty = (Double) request.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            if (planQty != null) {
                targetQtyByClass.put(classIndex, BigDecimal.valueOf(planQty));
            }
        });
        if (targetQtyByClass.isEmpty()) {
            throw new IllegalStateException("调量目标计划量不能为空");
        }
        targetQtyByClass.forEach((classIndex, targetQuantity) -> {
            if (classIndex < 1 || classIndex > 6 || targetQuantity.signum() < 0) {
                throw new IllegalStateException("调量班次或目标计划量不合法");
            }
        });
        return targetQtyByClass;
    }

    /** 清空调量目标记录在当前班次的旧计划量，后续由滚动内核按目标量重写。 */
    private void clearChangeQtyTargetClass(ChangeQtyPlan changeQtyPlan,
                                           int classIndex,
                                           Map<Long, Cd90ScheduleResult> changedById,
                                           Map<Long, List<Cd90InsertLaneAllocationDraft>> replacementLanes) {
        Cd90ScheduleResult targetResult = changeQtyPlan.targetResult;
        targetResult.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex), null);
        targetResult.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), null);
        targetResult.setFieldValueByFieldName(String.format("class%dAnalysis", classIndex), null);
        changedById.put(targetResult.getId(), targetResult);
        replacementLanes.computeIfAbsent(targetResult.getId(), key -> new ArrayList<>())
                .removeIf(lane -> ("CLASS" + classIndex).equals(lane.getClassField()));
    }
    /**
     * 转机台先在内存中清掉原机台从起始班次开始的该帘布计划，
     * 再把清出的数量作为目标机台插入量交给同一套滚动内核。
     */
    private void prepareTransferSource(Cd90TransferMachineRequest transferRequest,
                                       Cd90InsertOrderRequest insertRequest,
                                       List<Cd90ScheduleResult> workingResults,
                                       Map<Long, Cd90ScheduleResult> changedById,
                                       Map<Long, Cd90ScheduleResult> deletedById,
                                       Map<Long, List<Cd90InsertLaneAllocationDraft>> replacementLanes) {
        if (transferRequest.getSourceMachineCode().equals(transferRequest.getTargetMachineCode())) {
            throw new IllegalStateException("原机台和目标机台不能相同");
        }
        List<Integer> transferClassIndexes = IntStream.rangeClosed(1, 6)
                .filter(classIndex -> this.readTransferProduceOrder(transferRequest, classIndex) != null)
                .boxed().collect(Collectors.toList());
        if (transferClassIndexes.isEmpty()) {
            throw new IllegalStateException("没有可转走的班次计划");
        }
        List<Cd90ScheduleResult> transferSources = workingResults.stream()
                .filter(item -> transferRequest.getSourceMachineCode().equals(item.getMachineCode()))
                .filter(item -> transferRequest.getClothCode().equals(item.getClothCode()))
                .filter(item -> transferClassIndexes.stream()
                        .anyMatch(classIndex -> readPlan(item, classIndex).signum() > 0))
                .collect(Collectors.toList());
        if (transferSources.isEmpty()) {
            throw new IllegalStateException("原机台没有可转走的帘布计划");
        }
        transferClassIndexes.forEach(classIndex -> {
            BigDecimal transferQuantity = transferSources.stream()
                    .map(item -> readPlan(item, classIndex))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (transferQuantity.signum() <= 0) {
                return;
            }
            boolean lockedTransfer = transferSources.stream()
                    .anyMatch(item -> readPlan(item, classIndex).signum() > 0
                            && isLocked(item, classIndex));
            if (lockedTransfer) {
                throw new IllegalStateException("已锁定或已生产的班次计划不能转机台");
            }
            insertRequest.setFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex), transferQuantity.doubleValue());
            insertRequest.setFieldValueByFieldName(
                    String.format("class%dProduceOrder", classIndex),
                    this.resolveTransferProduceOrder(transferRequest, workingResults, classIndex));
            insertRequest.setFieldValueByFieldName(
                    String.format("class%dAnalysisInput", classIndex), "转机台");
        });
        this.compactSourceMachineOrders(workingResults, transferSources, transferRequest.getSourceMachineCode(),
                transferClassIndexes, changedById);
        this.clearTransferSourceClasses(workingResults, transferSources, transferClassIndexes,
                changedById, deletedById, replacementLanes);
    }

    private Integer readTransferProduceOrder(Cd90TransferMachineRequest transferRequest,
                                             int classIndex) {
        Integer produceOrder = (Integer) transferRequest.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
        return produceOrder != null && produceOrder > 0 ? produceOrder : null;
    }

    private int resolveTransferProduceOrder(Cd90TransferMachineRequest transferRequest,
                                            List<Cd90ScheduleResult> workingResults,
                                            int classIndex) {
        Integer produceOrder = (Integer) transferRequest.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
        if (produceOrder != null && produceOrder > 0) {
            return produceOrder;
        }
        return this.nextTargetProduceOrder(workingResults, transferRequest.getTargetMachineCode(), classIndex);
    }

    private int nextTargetProduceOrder(List<Cd90ScheduleResult> workingResults,
                                       String targetMachineCode,
                                       int classIndex) {
        return workingResults.stream()
                .filter(item -> targetMachineCode.equals(item.getMachineCode()))
                .map(item -> readOrder(item, classIndex))
                .filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
    }

    private void clearTransferSourceClasses(List<Cd90ScheduleResult> workingResults,
                                            List<Cd90ScheduleResult> transferSources,
                                            List<Integer> transferClassIndexes,
                                            Map<Long, Cd90ScheduleResult> changedById,
                                            Map<Long, Cd90ScheduleResult> deletedById,
                                            Map<Long, List<Cd90InsertLaneAllocationDraft>> replacementLanes) {
        transferSources.forEach(result -> {
            transferClassIndexes.stream()
                    .filter(classIndex -> readPlan(result, classIndex).signum() > 0)
                    .forEach(classIndex -> {
                        result.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex), null);
                        result.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), null);
                        result.setFieldValueByFieldName(String.format("class%dAnalysis", classIndex), null);
                        result.setFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex), null);
                        if (result.getId() != null) {
                            String classField = "CLASS" + classIndex;
                            replacementLanes.computeIfAbsent(result.getId(), key -> new ArrayList<>())
                                    .removeIf(lane -> classField.equals(lane.getClassField()));
                        }
                    });
            if (result.getId() == null) {
                return;
            }
            if (this.hasScheduledQuantity(result)) {
                changedById.put(result.getId(), result);
                return;
            }
            changedById.remove(result.getId());
            deletedById.put(result.getId(), result);
            workingResults.remove(result);
        });
    }

    private void compactSourceMachineOrders(List<Cd90ScheduleResult> workingResults,
                                            List<Cd90ScheduleResult> transferSources,
                                            String sourceMachineCode,
                                            List<Integer> transferClassIndexes,
                                            Map<Long, Cd90ScheduleResult> changedById) {
        for (Integer classIndex : transferClassIndexes) {
            final int currentClassIndex = classIndex;
            List<Integer> removedOrders = transferSources.stream()
                    .filter(item -> readPlan(item, currentClassIndex).signum() > 0)
                    .map(item -> readOrder(item, currentClassIndex))
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
            if (removedOrders.isEmpty()) {
                continue;
            }
            workingResults.stream()
                    .filter(item -> sourceMachineCode.equals(item.getMachineCode()))
                    .filter(item -> !transferSources.contains(item))
                    .filter(item -> readPlan(item, currentClassIndex).signum() > 0)
                    .forEach(item -> {
                        Integer currentOrder = readOrder(item, currentClassIndex);
                        if (currentOrder == null) {
                            return;
                        }
                        long decrement = removedOrders.stream()
                                .filter(removedOrder -> removedOrder < currentOrder)
                                .count();
                        if (decrement <= 0) {
                            return;
                        }
                        item.setFieldValueByFieldName(String.format("class%dProduceOrder", currentClassIndex),
                                currentOrder - (int) decrement);
                        item.setFieldValueByFieldName(String.format("class%dAnalysis", currentClassIndex),
                                "转机台后原机台顺位前移");
                        if (item.getId() != null) {
                            changedById.put(item.getId(), item);
                        }
                    });
        }
    }
    private ShiftRollingResult rollShift(Cd90AutoScheduleContext context,
                                          Cd90ShiftDescriptor shift,
                                          int classIndex,
                                          Cd90InsertOrderRequest request,
                                          Cd90MachineInfo machine,
                                          List<Cd90ScheduleResult> workingResults,
                                          Cd90ScheduleResult insertResult,
                                          List<Cd90RollingPendingTask> incomingCarryovers,
                                          BigDecimal insertQuantity,
                                          Integer insertOrder,
                                          Map<Long, Cd90ScheduleResult> changedById,
                                          Map<String, Cd90MachineTailState> tailByMachine,
                                          Cd90ShiftResourceState resourceState,
                                          Cd90AutoScheduleInput input,
                                          Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanesByResult,
                                          Map<Long, List<Cd90InsertLaneAllocationDraft>> replacementLanes,
                                          List<Cd90InsertLaneAllocationDraft> insertLanes,
                                          List<Cd90InsertCarryoverImpact> carryoverImpacts) {
        List<Segment> segments = workingResults.stream()
                .filter(item -> request.getMachineCode().equals(item.getMachineCode()))
                .filter(item -> readPlan(item, classIndex).signum() > 0)
                .map(item -> Segment.existing(item, classIndex, readPlan(item, classIndex),
                        readOrder(item, classIndex), isLocked(item, classIndex)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (insertOrder != null) {
            segments.stream()
                    .filter(item -> item.order != null && item.order < insertOrder)
                    .forEach(item -> item.locked = true);
        }
        clearAdjustableClassFields(segments, classIndex, changedById, replacementLanes);
        mergeCarryovers(segments, incomingCarryovers, workingResults, insertResult, classIndex);
        if (insertQuantity.signum() > 0) {
            Segment directInsert = segments.stream()
                    .filter(item -> item.result == insertResult).findFirst().orElse(null);
            if (directInsert == null) {
                directInsert = Segment.insert(insertResult, classIndex, insertQuantity, insertOrder);
                segments.add(directInsert);
            } else {
                directInsert.quantity = directInsert.quantity.add(insertQuantity);
                directInsert.order = insertOrder;
                directInsert.hardInsert = true;
            }
        }
        Cd90MachineTailState previousTail = tailByMachine.get(request.getMachineCode());
        orderSegments(segments, insertOrder, previousTail);

        int fullSeconds = Math.max(1, shift.getDurationSeconds());
        int remainingSeconds = fullSeconds;
        List<Cd90RollingPendingTask> carryovers = new ArrayList<>();
        int nextOrder = 1;
        boolean deferSuffix = false;
        String deferReason = null;
        for (Segment segment : segments) {
            if (deferSuffix) {
                Cd90RollingPendingTask pendingTask = toPending(segment, shift,
                        nextClassField(context, classIndex), segment.quantity, deferReason);
                carryovers.add(pendingTask);
                carryoverImpacts.add(this.toCarryoverImpact(
                        pendingTask, shift.getClassField()));
                continue;
            }
            int assignedOrder = segment.locked && segment.order != null
                    ? segment.order : nextOrder;
            if (segment.locked) {
                nextOrder = Math.max(nextOrder, assignedOrder + 1);
            }
            int remainingSecondsBeforeTrial = remainingSeconds;
            Cd90MachineTailState previousTailBeforeTrial = previousTail;
            BigDecimal scheduled;
            String limitReason = null;
            if (segment.locked) {
                scheduled = segment.quantity;
                Cd90MachineTailState currentTail = Cd90MachineTailState.builder()
                        .clothCode(segment.result.getClothCode())
                        .bigRollCode(segment.result.getBigRollCode()).build();
                Cd90MachineCapacityTrial lockedTrial = capacityCalculator.calculateWithRemainingSeconds(
                        BigDecimal.valueOf(machine.getQuota()),
                        Math.max(1, shift.getDurationSeconds() / 3600), remainingSeconds,
                        previousTail, currentTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        segment.quantity);
                remainingSeconds = lockedTrial.isFullyAccommodated()
                        ? lockedTrial.getRemainingSeconds() : 0;
                previousTail = currentTail;
            } else if (remainingSeconds <= 0) {
                scheduled = BigDecimal.ZERO;
                limitReason = "CAPACITY_LIMIT";
            } else {
                Cd90MachineTailState currentTail = Cd90MachineTailState.builder()
                        .clothCode(segment.result.getClothCode())
                        .bigRollCode(segment.result.getBigRollCode()).build();
                Cd90MachineCapacityTrial trial = capacityCalculator.calculateWithRemainingSeconds(
                        BigDecimal.valueOf(machine.getQuota()),
                        Math.max(1, shift.getDurationSeconds() / 3600), remainingSeconds,
                        previousTail, currentTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        segment.quantity);
                scheduled = trial.getCapacityQuantity();
                remainingSeconds = trial.getRemainingSeconds();
                if (!trial.isFullyAccommodated()) {
                    limitReason = "CAPACITY_LIMIT";
                }
                previousTail = currentTail;
            }
            LaneCommit laneCommit = segment.locked
                    ? reserveLockedLanes(segment, shift, scheduled, resourceState,
                            sourceLanesByResult, input, context)
                    : allocateLanes(segment, shift, scheduled, resourceState, input, context);
            scheduled = normalizeScheduledQuantity(laneCommit.quantity);
            if (!segment.locked && scheduled.signum() <= 0) {
                remainingSeconds = remainingSecondsBeforeTrial;
                previousTail = previousTailBeforeTrial;
            }
            if (scheduled.signum() <= 0 && segment.quantity.signum() > 0) {
                limitReason = laneCommit.limitReason;
            } else if (scheduled.compareTo(segment.quantity) < 0
                    && laneCommit.limitReason != null) {
                limitReason = laneCommit.limitReason;
            }
            if (scheduled.signum() > 0) {
                if (!segment.locked) {
                    nextOrder = Math.max(nextOrder, assignedOrder + 1);
                    writeClass(segment.result, classIndex, shift, scheduled, assignedOrder,
                            limitReason, request);
                    if (segment.result.getId() != null) {
                        changedById.put(segment.result.getId(), segment.result);
                    }
                    List<Cd90InsertLaneAllocationDraft> targetLanes = segment.result.getId() == null
                            ? insertLanes : replacementLanes.computeIfAbsent(
                                    segment.result.getId(), key -> new ArrayList<>());
                    targetLanes.addAll(toLaneDrafts(segment, shift, scheduled,
                            laneCommit.allocations));
                }
                resourceState.getTasks().add(Cd90ShiftScheduleTask.builder()
                        .classField(shift.getClassField())
                        .clothCode(segment.result.getClothCode())
                        .bigRollCode(segment.result.getBigRollCode())
                        .cordSpec(segment.result.getClothCode())
                        .machineCode(segment.result.getMachineCode())
                        .planQuantity(scheduled)
                        .vehicleCount(laneCommit.vehicleCount)
                        .produceOrder(assignedOrder)
                        .expectedStartTime(shift.getStartTime())
                        .expectedEndTime(shift.getEndTime())
                        .laneAllocations(laneCommit.allocations).build());
            }
            BigDecimal remaining = segment.quantity.subtract(scheduled).max(BigDecimal.ZERO);
            if (remaining.signum() > 0) {
                Cd90RollingPendingTask pendingTask = toPending(segment, shift,
                        nextClassField(context, classIndex), remaining, limitReason);
                carryovers.add(pendingTask);
                carryoverImpacts.add(this.toCarryoverImpact(
                        pendingTask, shift.getClassField()));
                if (segment.hardInsert) {
                    deferSuffix = true;
                    deferReason = limitReason;
                }
            }
        }
        if (previousTail != null) {
            tailByMachine.put(request.getMachineCode(), previousTail);
            resourceState.getTailByMachine().put(request.getMachineCode(), previousTail);
        }
        return new ShiftRollingResult(carryovers, segments.size());
    }

    /** 非受影响机台按原库排明细占用本班资源，并作为计划入库滚入下一班。 */
    private void reserveUnaffectedTasks(Cd90ShiftResourceState state,
                                        Cd90ShiftDescriptor shift,
                                        int classIndex,
                                        List<Cd90ScheduleResult> results,
                                        Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanes,
                                        Cd90AutoScheduleInput input,
                                        Cd90AutoScheduleContext context,
                                        String affectedMachineCode) {
        results.stream()
                .filter(item -> readPlan(item, classIndex).signum() > 0)
                .filter(item -> affectedMachineCode == null
                        || !affectedMachineCode.equals(item.getMachineCode()))
                .sorted(Comparator.comparing(item -> readOrder(item, classIndex),
                        Comparator.nullsLast(Integer::compareTo)))
                .forEach(item -> {
                    BigDecimal quantity = readPlan(item, classIndex);
                    LaneCommit laneCommit = reserveExistingLanes(item, shift, quantity, state,
                            sourceLanes.getOrDefault(item.getId(), Collections.emptyList()),
                            input, context);
                    state.getTasks().add(Cd90ShiftScheduleTask.builder()
                            .classField(shift.getClassField()).clothCode(item.getClothCode())
                            .bigRollCode(item.getBigRollCode()).cordSpec(item.getClothCode())
                            .machineCode(item.getMachineCode()).planQuantity(quantity)
                            .vehicleCount(laneCommit.vehicleCount)
                            .produceOrder(readOrder(item, classIndex) == null
                                    ? 1 : readOrder(item, classIndex))
                            .expectedStartTime(shift.getStartTime())
                            .expectedEndTime(shift.getEndTime())
                            .laneAllocations(laneCommit.allocations).build());
                    state.getTailByMachine().put(item.getMachineCode(),
                            Cd90MachineTailState.builder().clothCode(item.getClothCode())
                                    .bigRollCode(item.getBigRollCode()).build());
                });
    }

    private LaneCommit reserveLockedLanes(Segment segment,
                                          Cd90ShiftDescriptor shift,
                                          BigDecimal quantity,
                                          Cd90ShiftResourceState state,
                                          Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanes,
                                          Cd90AutoScheduleInput input,
                                          Cd90AutoScheduleContext context) {
        return reserveExistingLanes(segment.result, shift, quantity, state,
                sourceLanes.getOrDefault(segment.result.getId(), Collections.emptyList()),
                input, context);
    }

    private LaneCommit reserveExistingLanes(Cd90ScheduleResult result,
                                            Cd90ShiftDescriptor shift,
                                            BigDecimal quantity,
                                            Cd90ShiftResourceState state,
                                            List<Cd90ScheduleLaneAllocation> sourceLanes,
                                            Cd90AutoScheduleInput input,
                                            Cd90AutoScheduleContext context) {
        List<Cd90ScheduleLaneAllocation> rows = sourceLanes.stream()
                .filter(item -> shift.getClassField().equals(item.getClassField()))
                .sorted(Comparator.comparing(Cd90ScheduleLaneAllocation::getAllocationOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            return allocateLanes(Segment.existing(result, classIndex(shift.getClassField()),
                    quantity, null, true), shift, quantity, state, input, context);
        }
        // 预检查：原库排若已被其他帘布占用（mergeInbound 跳过入库留下的脏数据），整体降级为重新分配
        // 与 Cd90ResourceSnapshotBuilder.mergeInbound 的软冲突策略对齐，避免整个插单中断
        List<String> conflictedLaneCodes = rows.stream()
                .map(row -> {
                    Cd90StorageLaneState lane = state.getLanes().stream()
                            .filter(item -> row.getStorageLaneCode().equals(item.getLaneCode()))
                            .findFirst().orElse(null);
                    if (lane == null || lane.getClothCode() == null
                            || lane.getClothCode().trim().isEmpty()) {
                        return null;
                    }
                    return result.getClothCode().equals(lane.getClothCode())
                            ? null : row.getStorageLaneCode();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!conflictedLaneCodes.isEmpty()) {
            log.warn("[直裁插单] 原排程库排已被其他帘布占用,降级为重新分配, clothCode={}, conflictedLanes={}",
                    result.getClothCode(), conflictedLaneCodes);
            return allocateLanes(Segment.existing(result, classIndex(shift.getClassField()),
                    quantity, null, true), shift, quantity, state, input, context);
        }
        List<String> capacityChangedLaneCodes = rows.stream()
                .filter(row -> {
                    Cd90StorageLaneState lane = state.getLanes().stream()
                            .filter(item -> row.getStorageLaneCode().equals(item.getLaneCode()))
                            .findFirst().orElse(null);
                    if (lane == null) {
                        return false;
                    }
                    int vehicles = row.getAllocatedCartCount() == null
                            ? 0 : row.getAllocatedCartCount();
                    return vehicles <= 0
                            || lane.getVehicleCount() + vehicles > lane.getMaxVehicleCount();
                })
                .map(Cd90ScheduleLaneAllocation::getStorageLaneCode)
                .distinct()
                .collect(Collectors.toList());
        if (!capacityChangedLaneCodes.isEmpty()
                && !this.isLocked(result, classIndex(shift.getClassField()))) {
            log.warn("[直裁插单] 未开始任务原库排容量不足,降级为重新分配, "
                            + "clothCode={}, changedLanes={}",
                    result.getClothCode(), capacityChangedLaneCodes);
            return allocateLanes(Segment.existing(result, classIndex(shift.getClassField()),
                    quantity, null, true), shift, quantity, state, input, context);
        }
        List<Cd90StorageLaneAllocation> allocations = rows.stream().map(row -> {
            Cd90StorageLaneState lane = state.getLanes().stream()
                    .filter(item -> row.getStorageLaneCode().equals(item.getLaneCode()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "原排程库排不存在于当前资源快照: " + row.getStorageLaneCode()));
            int vehicles = row.getAllocatedCartCount() == null ? 0 : row.getAllocatedCartCount();
            if (vehicles <= 0 || lane.getVehicleCount() + vehicles > lane.getMaxVehicleCount()) {
                throw new IllegalStateException("原排程库排资源已变化，无法保持锁定任务: "
                        + row.getStorageLaneCode());
            }
            if (lane.getClothCode() != null && !lane.getClothCode().trim().isEmpty()
                    && !result.getClothCode().equals(lane.getClothCode())) {
                throw new IllegalStateException("原排程库排已被其他帘布占用: "
                        + row.getStorageLaneCode());
            }
            lane.setClothCode(result.getClothCode());
            lane.setVehicleCount(lane.getVehicleCount() + vehicles);
            return Cd90StorageLaneAllocation.builder()
                    .laneCode(row.getStorageLaneCode()).vehicleCount(vehicles).build();
        }).collect(Collectors.toList());
        int vehicleCount = allocations.stream().mapToInt(Cd90StorageLaneAllocation::getVehicleCount).sum();
        state.setOccupiedToolingCount(state.getOccupiedToolingCount() + vehicleCount);
        return new LaneCommit(quantity, allocations, vehicleCount, null);
    }

    private LaneCommit allocateLanes(Segment segment,
                                     Cd90ShiftDescriptor shift,
                                     BigDecimal requestedQuantity,
                                     Cd90ShiftResourceState state,
                                     Cd90AutoScheduleInput input,
                                     Cd90AutoScheduleContext context) {
        if (requestedQuantity == null || requestedQuantity.signum() <= 0) {
            return LaneCommit.empty();
        }
        Cd90ConstructionMaterial material = findMaterial(input, segment.result.getClothCode());
        if (material == null) {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0,
                    "CONSTRUCTION_MISSING");
        }
        if (input.getBigRollAgingDataMissingCodes() != null
                && input.getBigRollAgingDataMissingCodes().contains(material.getBigRollCode())) {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0,
                    "BIG_ROLL_STOCK_DATA_MISSING");
        }
        BigDecimal fallback = context == null ? BigDecimal.valueOf(1000)
                : context.getParameters().getRollCoilMeter();
        BigDecimal standardCurlLength = material.getCurlLength() == null
                || material.getCurlLength().signum() <= 0 ? fallback : material.getCurlLength();
        int availableTooling = Math.max(0,
                state.getTotalToolingCount() - state.getOccupiedToolingCount());
        if (availableTooling <= 0) {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0, "ROLL_TOOL_LIMIT");
        }
        BigDecimal toolingQuantity = standardCurlLength.multiply(BigDecimal.valueOf(availableTooling));
        BigDecimal trialQuantity = requestedQuantity.min(toolingQuantity);
        Cd90StorageLaneAllocationResult allocation = laneAllocator.allocate(
                segment.result.getClothCode(), trialQuantity, standardCurlLength,
                state.getLanes(), segment.hardInsert);
        if (!allocation.isSuccess() || allocation.getAllocatedVehicleCount() <= 0) {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0,
                    allocation.getFailureReason());
        }
        BigDecimal laneQuantity = standardCurlLength
                .multiply(BigDecimal.valueOf(allocation.getAllocatedVehicleCount()));
        BigDecimal committed = requestedQuantity.min(laneQuantity);
        state.setLanes(allocation.getLanes());
        state.setOccupiedToolingCount(state.getOccupiedToolingCount()
                + allocation.getAllocatedVehicleCount());
        String reason = committed.compareTo(requestedQuantity) < 0
                ? (toolingQuantity.compareTo(requestedQuantity) < 0
                        ? "ROLL_TOOL_LIMIT" : "STORAGE_LANE_LIMIT") : null;
        return new LaneCommit(committed, allocation.getAllocations(),
                allocation.getAllocatedVehicleCount(), reason);
    }

    /** 插单滚动与自动排程提交层保持一致，最终计划量按整数米向上取整。 */
    private BigDecimal normalizeScheduledQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return quantity.setScale(0, RoundingMode.CEILING);
    }

    private List<Cd90InsertLaneAllocationDraft> toLaneDrafts(
            Segment segment, Cd90ShiftDescriptor shift, BigDecimal quantity,
            List<Cd90StorageLaneAllocation> allocations) {
        int totalVehicles = allocations.stream()
                .mapToInt(Cd90StorageLaneAllocation::getVehicleCount).sum();
        BigDecimal[] remaining = {quantity};
        List<Cd90InsertLaneAllocationDraft> drafts = new ArrayList<>();
        for (int index = 0; index < allocations.size(); index++) {
            Cd90StorageLaneAllocation allocation = allocations.get(index);
            BigDecimal allocationQuantity = index == allocations.size() - 1
                    ? remaining[0] : quantity.multiply(BigDecimal.valueOf(allocation.getVehicleCount()))
                            .divide(BigDecimal.valueOf(totalVehicles), 10, RoundingMode.HALF_UP);
            remaining[0] = remaining[0].subtract(allocationQuantity);
            drafts.add(Cd90InsertLaneAllocationDraft.builder()
                    .scheduleResultId(segment.result.getId())
                    .insertResult(segment.result.getId() == null)
                    .classField(shift.getClassField())
                    .shiftScheduleDate(Date.from(shift.getStartTime()
                            .atZone(ZoneId.systemDefault()).toInstant()))
                    .laneCode(allocation.getLaneCode())
                    .allocationQuantity(allocationQuantity)
                    .vehicleCount(allocation.getVehicleCount())
                    .allocationOrder(index + 1).build());
        }
        return drafts;
    }

    private Cd90InsertLaneAllocationDraft copyLaneDraft(Cd90ScheduleLaneAllocation source) {
        return Cd90InsertLaneAllocationDraft.builder()
                .scheduleResultId(source.getScheduleResultId()).insertResult(false)
                .classField(source.getClassField())
                .shiftScheduleDate(source.getShiftScheduleDate())
                .laneCode(source.getStorageLaneCode())
                .allocationQuantity(source.getAllocatedQty() == null
                        ? BigDecimal.ZERO : BigDecimal.valueOf(source.getAllocatedQty()))
                .vehicleCount(source.getAllocatedCartCount() == null
                        ? 0 : source.getAllocatedCartCount())
                .allocationOrder(source.getAllocationOrder() == null
                        ? 1 : source.getAllocationOrder()).build();
    }

    private Map<String, BigDecimal> buildCurlLengthByCloth(
            Cd90AutoScheduleInput input, Cd90AutoScheduleContext context) {
        if (input == null || input.getConstructionMaterials() == null) {
            return new HashMap<>();
        }
        BigDecimal fallback = context.getParameters().getRollCoilMeter();
        return input.getConstructionMaterials().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getClothCode() != null)
                .collect(Collectors.toMap(Cd90ConstructionMaterial::getClothCode,
                        item -> item.getCurlLength() == null
                                || item.getCurlLength().signum() <= 0
                                ? fallback : item.getCurlLength(),
                        (first, second) -> first));
    }

    /** 将插单帘布施工补入按成型计划加载的班次输入，供库排和工装试算使用。 */
    private void ensureInsertMaterial(Cd90AutoScheduleInput input,
                                      Cd90ConstructionMaterial insertMaterial) {
        if (input == null || insertMaterial == null) {
            return;
        }
        List<Cd90ConstructionMaterial> materials = new ArrayList<>(
                input.getConstructionMaterials() == null
                        ? Collections.emptyList() : input.getConstructionMaterials());
        boolean exists = materials.stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> Objects.equals(item.getClothCode(), insertMaterial.getClothCode())
                        && Objects.equals(item.getBigRollCode(), insertMaterial.getBigRollCode()));
        if (!exists) {
            materials.add(insertMaterial);
            input.setConstructionMaterials(materials);
        }
    }

    private Cd90ConstructionMaterial findMaterial(Cd90AutoScheduleInput input, String clothCode) {
        if (input == null || input.getConstructionMaterials() == null) {
            return null;
        }
        return input.getConstructionMaterials().stream()
                .filter(Objects::nonNull)
                .filter(item -> clothCode.equals(item.getClothCode()))
                .findFirst().orElse(null);
    }

    private void orderSegments(List<Segment> segments, Integer insertOrder,
                               Cd90MachineTailState previousTail) {
        List<Segment> sorted = segments.stream()
                .sorted(Comparator.comparing((Segment item) -> !item.locked)
                        .thenComparingInt(item -> continuityRank(item, previousTail))
                        .thenComparing(item -> insertOrder == null ? item.carryover : !item.carryover)
                        .thenComparing(item -> item.order,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(item -> item.result.getId(),
                                Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        Segment hardInsert = sorted.stream().filter(item -> item.hardInsert)
                .findFirst().orElse(null);
        if (hardInsert != null && insertOrder != null) {
            sorted.remove(hardInsert);
            int position = Math.max(0, Math.min(insertOrder - 1, sorted.size()));
            sorted.add(position, hardInsert);
        }
        segments.clear();
        segments.addAll(sorted);
    }

    private int continuityRank(Segment segment, Cd90MachineTailState previousTail) {
        if (segment == null || segment.result == null || previousTail == null) {
            return 3;
        }
        boolean sameSpec = Objects.equals(previousTail.getClothCode(), segment.result.getClothCode());
        boolean sameRoll = Objects.equals(previousTail.getBigRollCode(), segment.result.getBigRollCode());
        if (sameSpec && sameRoll) {
            return 0;
        }
        if (sameSpec) {
            return 1;
        }
        return sameRoll ? 2 : 3;
    }

    private void mergeCarryovers(List<Segment> segments,
                                 List<Cd90RollingPendingTask> carryovers,
                                 List<Cd90ScheduleResult> workingResults,
                                 Cd90ScheduleResult insertResult,
                                 int classIndex) {
        for (Cd90RollingPendingTask pending : carryovers) {
            Cd90ScheduleResult result = pending.isHardInsert() ? insertResult
                    : workingResults.stream()
                            .filter(item -> Objects.equals(item.getId(), pending.getSourceResultId()))
                            .findFirst().orElseThrow(() -> new IllegalStateException(
                                    "跨班待排任务找不到来源排程结果: " + pending.getTaskKey()));
            Segment segment = segments.stream().filter(item -> item.result == result)
                    .findFirst().orElse(null);
            if (segment == null) {
                segment = Segment.carryover(result, classIndex, pending);
                segments.add(segment);
            } else {
                // 已在当前班存在的原任务合并上一班顺延量时，只合并数量，排序交给实际机尾续作规则。
                segment.quantity = segment.quantity.add(pending.getRemainingQuantity());
            }
        }
    }

    private void clearAdjustableClassFields(List<Segment> segments, int classIndex,
                                            Map<Long, Cd90ScheduleResult> changedById,
                                            Map<Long, List<Cd90InsertLaneAllocationDraft>> replacementLanes) {
        segments.stream().filter(item -> !item.locked).forEach(item -> {
            item.result.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex), null);
            item.result.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), null);
            item.result.setFieldValueByFieldName(String.format("class%dAnalysis", classIndex), null);
            if (item.result.getId() != null) {
                changedById.put(item.result.getId(), item.result);
                replacementLanes.computeIfAbsent(item.result.getId(), key -> new ArrayList<>())
                        .removeIf(lane -> ("CLASS" + classIndex).equals(lane.getClassField()));
            }
        });
    }

    private void writeClass(Cd90ScheduleResult result, int classIndex,
                            Cd90ShiftDescriptor shift, BigDecimal quantity,
                            int produceOrder, String limitReason,
                            Cd90InsertOrderRequest request) {
        result.setFieldValueByFieldName(String.format("class%dScheduleDate", classIndex),
                Date.from(shift.getStartTime().atZone(ZoneId.systemDefault()).toInstant()));
        result.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex),
                quantity.doubleValue());
        result.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), produceOrder);
        String analysis = limitReason == null ? "插单滚动重排"
                : "插单后因" + limitReason + "部分顺延至下一班";
        result.setFieldValueByFieldName(String.format("class%dAnalysis", classIndex), analysis);
        if (result.getDataSource() != null && "1".equals(result.getDataSource())) {
            result.setFieldValueByFieldName(
                    String.format("class%dFinishRate", classIndex), 0D);
            result.setFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex),
                    request.getFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex)));
        }
    }

    private Cd90RollingPendingTask toPending(Segment segment, Cd90ShiftDescriptor shift,
                                              String targetClassField,
                                              BigDecimal remaining, String limitReason) {
        return Cd90RollingPendingTask.builder()
                .taskKey(taskKey(segment.result, shift.getClassField(), segment.order))
                .sourceResultId(segment.result.getId())
                .sourceBatchNo(segment.result.getBatchNo())
                .sourceOrderNo(segment.result.getOrderNo())
                .originalClassField(segment.originalClassField)
                .originalProduceOrder(segment.order)
                .targetClassField(targetClassField)
                .clothCode(segment.result.getClothCode())
                .bigRollCode(segment.result.getBigRollCode())
                .sourceMachineCode(segment.result.getMachineCode())
                .requiredMachineCode(segment.result.getMachineCode())
                .originalQuantity(segment.quantity)
                .scheduledQuantity(segment.quantity.subtract(remaining))
                .remainingQuantity(remaining)
                .hardInsert(segment.hardInsert).locked(false)
                .continueFromPreviousShift(true).lastLimitReason(limitReason).build();
    }

    /** 将任务级待排转换为前端确认使用的逐班影响明细。 */
    private Cd90InsertCarryoverImpact toCarryoverImpact(Cd90RollingPendingTask pendingTask,
                                                         String sourceClassField) {
        String reasonCode = pendingTask.getLastLimitReason() == null
                ? "SCHEDULE_WINDOW_LIMIT" : pendingTask.getLastLimitReason();
        return Cd90InsertCarryoverImpact.builder()
                .clothCode(pendingTask.getClothCode())
                .affectedType(pendingTask.isHardInsert() ? "INSERT" : "EXISTING")
                .sourceClassField(sourceClassField)
                .targetClassField(pendingTask.getTargetClassField())
                .carryoverQty(pendingTask.getRemainingQuantity())
                .reasonCode(reasonCode)
                .build();
    }

    private String nextClassField(Cd90AutoScheduleContext context, int classIndex) {
        return context.getShifts().stream()
                .filter(item -> classIndex(item.getClassField()) > classIndex)
                .map(Cd90ShiftDescriptor::getClassField)
                .findFirst().orElse(null);
    }

    /** 判断插单结果是否至少有一个班次形成正排产量。 */
    private boolean hasScheduledQuantity(Cd90ScheduleResult result) {
        return IntStream.rangeClosed(1, 6)
                .mapToObj(classIndex -> (Double) result.getFieldValueByFieldName(
                        String.format("class%dPlanQty", classIndex)))
                .filter(Objects::nonNull)
                .anyMatch(quantity -> quantity > 0D);
    }

    private List<Cd90UnscheduleResult> toUnscheduled(Cd90InsertOrderRequest request,
                                                      String batchNo,
                                                      List<Cd90RollingPendingTask> carryovers) {
        return carryovers.stream().map(task -> {
            Cd90UnscheduleResult result = new Cd90UnscheduleResult();
            result.setFactoryCode(request.getFactoryCode());
            result.setScheduleDate(request.getScheduleDate());
            result.setClothCode(task.getClothCode());
            result.setBigRollCode(task.getBigRollCode());
            result.setDemandQty(task.getOriginalQuantity().doubleValue());
            result.setScheduledQty(task.getScheduledQuantity().doubleValue());
            result.setUnscheduledQty(task.getRemainingQuantity().doubleValue());
            result.setFailStage("SCHEDULE_WINDOW_END");
            String limitReason = task.getLastLimitReason() == null
                    ? "SCHEDULE_WINDOW_LIMIT" : task.getLastLimitReason();
            result.setReasonCode(this.persistedReasonCode(limitReason));
            result.setReasonOrder(1);
            result.setPrimaryReason("1");
            result.setUnscheduledReason(this.unscheduledReason(limitReason));
            result.setCandidateMachineCodes(task.getRequiredMachineCode());
            result.setBatchNo(batchNo);
            result.setDataSource("1");
            result.setProcessedTime(new Date());
            return result;
        }).collect(Collectors.toList());
    }

    /** 内部细分原因落库时兼容现有未排原因编码。 */
    private String persistedReasonCode(String reasonCode) {
        if ("BIG_ROLL_STOCK_DATA_MISSING".equals(reasonCode)
                || "CONSTRUCTION_MISSING".equals(reasonCode)) {
            return "DATA_MISSING";
        }
        return reasonCode;
    }

    /** 为数据缺失类未排结果保存可直接理解的具体说明。 */
    private String unscheduledReason(String reasonCode) {
        if ("BIG_ROLL_STOCK_DATA_MISSING".equals(reasonCode)) {
            return "大卷库存没有成熟时间和单卷米数";
        }
        if ("CONSTRUCTION_MISSING".equals(reasonCode)) {
            return "插单帘布施工宽度或单耗数据不完整";
        }
        return "插单滚动至最后班次仍未完全容纳";
    }

    private Cd90ScheduleResult newInsertResult(Cd90InsertOrderRequest request, String batchNo,
                                                Cd90ConstructionMaterial material) {
        Cd90ScheduleResult result = new Cd90ScheduleResult();
        result.setFactoryCode(request.getFactoryCode());
        result.setScheduleDate(request.getScheduleDate());
        result.setBatchNo(batchNo);
        result.setClothCode(request.getClothCode());
        result.setBigRollCode(material.getBigRollCode());
        result.setUnitConsume(material.getUnitConsumeMillimeter().doubleValue());
        result.setMachineCode(request.getMachineCode());
        result.setDataSource("1");
        result.setIsRelease("0");
        result.setProductionStatus("0");
        result.setPublishSuccessCount(0);
        result.setRemark(request.getRemark());
        return result;
    }

    private Cd90ConstructionMaterial findInsertMaterial(Cd90InsertOrderRequest request) {
        return constructionMapper.selectList(new LambdaQueryWrapper<MdmConstructionInfo>()
                        .eq(MdmConstructionInfo::getFactoryCode, request.getFactoryCode()))
                .stream().flatMap(item -> constructionMaterialMapper.map(item).stream())
                .filter(item -> request.getClothCode().equals(item.getClothCode()))
                .filter(item -> item.getBigRollCode() != null && !item.getBigRollCode().trim().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "插单帘布未匹配到有效施工和大卷: " + request.getClothCode()));
    }

    private Cd90ScheduleResult copyResult(Cd90ScheduleResult source) {
        Cd90ScheduleResult target = new Cd90ScheduleResult();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private boolean isLocked(Cd90ScheduleResult result, int classIndex) {
        Double finish = (Double) result.getFieldValueByFieldName(
                String.format("class%dFinishQty", classIndex));
        Double plan = (Double) result.getFieldValueByFieldName(
                String.format("class%dPlanQty", classIndex));
        return Integer.valueOf(1).equals(result.getIsLocked())
                || (finish != null && finish > 0D)
                || ("1".equals(result.getProductionStatus())
                && plan != null && (finish == null || finish < plan));
    }

    private BigDecimal readPlan(Cd90ScheduleResult result, int classIndex) {
        Double value = (Double) result.getFieldValueByFieldName(
                String.format("class%dPlanQty", classIndex));
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private Integer readOrder(Cd90ScheduleResult result, int classIndex) {
        return (Integer) result.getFieldValueByFieldName(
                String.format("class%dProduceOrder", classIndex));
    }

    private Integer nextProduceOrder(List<Cd90ScheduleResult> results,
                                     String machineCode,
                                     int classIndex) {
        return results.stream()
                .filter(item -> machineCode.equals(item.getMachineCode()))
                .map(item -> readOrder(item, classIndex))
                .filter(Objects::nonNull)
                .filter(order -> order > 0)
                .max(Integer::compareTo)
                .map(order -> order + 1)
                .orElse(1);
    }

    private int classIndex(String classField) {
        return Integer.parseInt(classField.replace("CLASS", ""));
    }

    private String taskKey(Cd90ScheduleResult result, String classField, Integer order) {
        return (result.getId() == null ? "INSERT" : result.getId())
                + "|" + classField + "|" + order;
    }

    private static final class ChangeQtyPlan {
        private final Cd90ScheduleResult targetResult;
        private final Map<Integer, BigDecimal> targetQtyByClass;

        private ChangeQtyPlan(Cd90ScheduleResult targetResult,
                              Map<Integer, BigDecimal> targetQtyByClass) {
            this.targetResult = targetResult;
            this.targetQtyByClass = targetQtyByClass;
        }
    }
    private static final class ShiftRollingResult {
        private final List<Cd90RollingPendingTask> carryovers;
        private final int taskCount;

        private ShiftRollingResult(List<Cd90RollingPendingTask> carryovers, int taskCount) {
            this.carryovers = carryovers;
            this.taskCount = taskCount;
        }
    }

    private static final class LaneCommit {
        private final BigDecimal quantity;
        private final List<Cd90StorageLaneAllocation> allocations;
        private final int vehicleCount;
        private final String limitReason;

        private LaneCommit(BigDecimal quantity,
                           List<Cd90StorageLaneAllocation> allocations,
                           int vehicleCount,
                           String limitReason) {
            this.quantity = quantity;
            this.allocations = allocations;
            this.vehicleCount = vehicleCount;
            this.limitReason = limitReason;
        }

        private static LaneCommit empty() {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0, null);
        }
    }

    private static final class Segment {
        private final Cd90ScheduleResult result;
        private final String originalClassField;
        private BigDecimal quantity;
        private Integer order;
        private boolean locked;
        private boolean hardInsert;
        private boolean carryover;

        private Segment(Cd90ScheduleResult result, String originalClassField,
                        BigDecimal quantity, Integer order, boolean locked,
                        boolean hardInsert, boolean carryover) {
            this.result = result;
            this.originalClassField = originalClassField;
            this.quantity = quantity;
            this.order = order;
            this.locked = locked;
            this.hardInsert = hardInsert;
            this.carryover = carryover;
        }

        private static Segment existing(Cd90ScheduleResult result, int classIndex,
                                        BigDecimal quantity, Integer order, boolean locked) {
            return new Segment(result, "CLASS" + classIndex, quantity, order,
                    locked, false, false);
        }

        private static Segment insert(Cd90ScheduleResult result, int classIndex,
                                      BigDecimal quantity, Integer order) {
            return new Segment(result, "CLASS" + classIndex, quantity, order,
                    false, true, false);
        }

        private static Segment carryover(Cd90ScheduleResult result, int classIndex,
                                         Cd90RollingPendingTask pending) {
            return new Segment(result, pending.getOriginalClassField(),
                    pending.getRemainingQuantity(), pending.getOriginalProduceOrder(),
                    false, pending.isHardInsert(), true);
        }
    }
}
