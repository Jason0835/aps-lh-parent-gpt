package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.algorithm.Cd15BigRollAgingAllocator;
import com.zlt.aps.cd15.engine.algorithm.Cd15BigRollMeterCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineCandidateResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineCapacityCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15MachineModeResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15RollingScheduleContextManager;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftResourceCommitter;
import com.zlt.aps.cd15.engine.algorithm.Cd15StorageLaneAllocator;
import com.zlt.aps.cd15.engine.algorithm.Cd15VehiclePlanQuantityCalculator;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15ConstructionMaterialMapper;
import com.zlt.aps.cd15.engine.constant.Cd15CutMode;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15InsertCarryoverImpact;
import com.zlt.aps.cd15.engine.model.Cd15InsertRollingOutput;
import com.zlt.aps.cd15.engine.model.Cd15InsertLaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15MachineCandidateResolution;
import com.zlt.aps.cd15.engine.model.Cd15MachineCapacityTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialPlan;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitRequest;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocationResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleEngineService;
import com.zlt.aps.cd15.engine.service.Cd15InsertRollingService;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cd15.engine.service.Cd15MachineResourceService;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基于现有班次和产能计算器执行插单滚动重排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15InsertRollingServiceImpl implements Cd15InsertRollingService {

    private final Cd15AutoScheduleEngineService autoScheduleEngineService;
    private final Cd15EngineScheduleResultMapper scheduleResultMapper;
    private final Cd15EngineScheduleLaneAllocationMapper laneAllocationMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15ConstructionMaterialMapper constructionMaterialMapper;
    private final Cd15MachineCapacityCalculator capacityCalculator;
    private final Cd15MachineCandidateResolver machineCandidateResolver;
    private final Cd15MachineModeResolver machineModeResolver;
    private final Cd15MachineResourceService machineResourceService;
    private final Cd15BigRollAgingAllocator bigRollAgingAllocator;
    private final Cd15BigRollMeterCalculator bigRollMeterCalculator;
    private final Cd15AutoScheduleInputService inputService;
    private final Cd15ShiftDemandProvider demandProvider;
    private final Cd15RollingScheduleContextManager rollingContextManager;
    private final Cd15StorageLaneAllocator laneAllocator;
    private final Cd15ShiftResourceCommitter shiftResourceCommitter;
    private final Cd15VehiclePlanQuantityCalculator vehiclePlanQuantityCalculator;

    @Override
    public Cd15InsertRollingOutput execute(Cd15InsertOrderRequest request) {
        return this.executeInternal(request, null, null);
    }

    @Override
    public Cd15InsertRollingOutput executeTransfer(Cd15TransferMachineRequest request) {
        Cd15InsertOrderRequest insertRequest = new Cd15InsertOrderRequest();
        insertRequest.setFactoryCode(request.getFactoryCode());
        insertRequest.setScheduleDate(request.getScheduleDate());
        insertRequest.setMachineCode(request.getTargetMachineCode());
        insertRequest.setSteelStripCode(request.getSteelStripCode());
        insertRequest.setRemark(request.getRemark());
        insertRequest.setConfirmed(request.getConfirmed());
        return this.executeInternal(insertRequest, request, null);
    }

    @Override
    public Cd15InsertRollingOutput executeChangeQty(Cd15ChangeQtyRequest request) {
        Cd15InsertOrderRequest rollingRequest = new Cd15InsertOrderRequest();
        rollingRequest.setFactoryCode(request.getFactoryCode());
        rollingRequest.setScheduleDate(request.getScheduleDate());
        rollingRequest.setMachineCode(request.getMachineCode());
        rollingRequest.setSteelStripCode(request.getSteelStripCode());
        rollingRequest.setRemark(request.getRemark());
        rollingRequest.setConfirmed(request.getConfirmed());
        return this.executeInternal(rollingRequest, null, request);
    }

    private Cd15InsertRollingOutput executeInternal(Cd15InsertOrderRequest request,
                                                    Cd15TransferMachineRequest transferRequest,
                                                    Cd15ChangeQtyRequest changeQtyRequest) {
        Cd15AutoScheduleContext context = autoScheduleEngineService.prepare(
                request.getFactoryCode(), request.getScheduleDate());
        List<Cd15ScheduleResult> sourceResults = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .eq(Cd15ScheduleResult::getFactoryCode, request.getFactoryCode())
                        .eq(Cd15ScheduleResult::getScheduleDate, request.getScheduleDate()));
        if (sourceResults.isEmpty()) {
            throw new IllegalStateException("当前排程日期没有可供插单的原排程结果");
        }
        // 只使用最新批次的排程结果，避免多批次共存时重复处理同一钢带。
        String latestBatchNo = sourceResults.stream()
                .map(Cd15ScheduleResult::getCd15BatchNo).filter(Objects::nonNull)
                .max(String::compareTo)
                .orElseThrow(() -> new IllegalStateException("原排程结果缺少批次号"));
        sourceResults = sourceResults.stream()
                .filter(item -> latestBatchNo.equals(item.getCd15BatchNo()))
                .collect(Collectors.toList());
        String batchNo = sourceResults.stream().map(Cd15ScheduleResult::getCd15BatchNo)
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("原排程结果缺少批次号"));
        List<Cd15ScheduleResult> workingResults = sourceResults.stream()
                .map(this::copyResult).collect(Collectors.toList());
        workingResults.forEach(item -> item.setBigRollConsumeQty(BigDecimal.ZERO));
        List<Long> sourceResultIds = sourceResults.stream().map(Cd15ScheduleResult::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<Cd15ScheduleLaneAllocation> sourceLaneAllocations = sourceResultIds.isEmpty()
                ? Collections.emptyList() : laneAllocationMapper.selectList(
                        new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                                .in(Cd15ScheduleLaneAllocation::getScheduleResultId, sourceResultIds));
        Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanesByResult = sourceLaneAllocations.stream()
                .collect(Collectors.groupingBy(Cd15ScheduleLaneAllocation::getScheduleResultId));
        Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes = sourceLaneAllocations.stream()
                .collect(Collectors.groupingBy(Cd15ScheduleLaneAllocation::getScheduleResultId,
                        LinkedHashMap::new, Collectors.mapping(this::copyLaneDraft, Collectors.toList())));
        Map<Long, Cd15ScheduleResult> changedById = new LinkedHashMap<>();
        Map<Long, Cd15ScheduleResult> deletedById = new LinkedHashMap<>();
        ChangeQtyPlan changeQtyPlan = null;
        if (changeQtyRequest != null) {
            changeQtyPlan = this.prepareChangeQtySource(changeQtyRequest, request, workingResults);
        }
        TransferPlan transferPlan = null;
        if (transferRequest != null) {
            transferPlan = this.prepareTransferSource(transferRequest, workingResults,
                    changedById, deletedById, replacementLanes);
            workingResults.addAll(transferPlan.targetResults);
        }
        List<Cd15InsertLaneAllocationDraft> insertLanes = new ArrayList<>();
        Cd15ScheduleResult insertResult = null;
        List<Cd15ScheduleResult> newResultCandidates = new ArrayList<>();
        List<Cd15ConstructionMaterial> adjustmentMaterials = new ArrayList<>();
        if (changeQtyPlan != null) {
            insertResult = changeQtyPlan.targetResult;
            adjustmentMaterials.add(this.findConstructionMaterial(
                    changeQtyPlan.targetResult));
        } else if (transferPlan != null) {
            newResultCandidates.addAll(transferPlan.targetResults);
            transferPlan.targetResults.stream()
                    .map(this::findConstructionMaterial)
                    .forEach(adjustmentMaterials::add);
        } else {
            Cd15ConstructionMaterial insertMaterial = this.findInsertMaterial(request);
            insertResult = this.newInsertResult(request, batchNo, insertMaterial);
            newResultCandidates.add(insertResult);
            adjustmentMaterials.add(insertMaterial);
        }

        List<Cd15RollingPendingTask> carryovers = new ArrayList<>();
        Map<String, Cd15MachineTailState> tailByMachine = new HashMap<>();
        List<Cd15InsertCarryoverImpact> carryoverImpacts = new ArrayList<>();
        Cd15RollingScheduleContext rollingResources = null;
        List<Cd15ShiftDescriptor> shifts = context.getShifts();
        for (int shiftIndex = 0; shiftIndex < shifts.size(); shiftIndex++) {
            Cd15ShiftDescriptor shift = shifts.get(shiftIndex);
            int classIndex = classIndex(shift.getClassField());
            Cd15AutoScheduleInput input = inputService.load(context.getFactoryCode(),
                    context.getScheduleDate(), shift.getClassField(), shift.getShiftCode(),
                    context.getParameters().getAgingPeriodHours());
            Cd15MachineResourceSnapshot machineSnapshot = machineResourceService.load(
                    context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
            adjustmentMaterials.forEach(material ->
                    this.ensureInsertMaterial(input, material));
            if (rollingResources == null) {
                rollingResources = rollingContextManager.initialize(input.getStorageLanesAtSix());
            }
            rollingContextManager.updateCumulativeConsumption(rollingResources,
                    demandProvider.cumulativeConsumptionBySteelStripBeforeShift(context, input, shift));
            Cd15ShiftResourceState resourceState = rollingContextManager.openShift(
                    rollingResources, shift, buildCurlLengthBySteelStrip(input, context),
                    context.getParameters().getRollCoilMeter(),
                    context.getParameters().getRollTotalCount(),
                    machineSnapshot.getMachines().stream()
                            .map(Cd15MachineResource::getMachineCode)
                            .collect(Collectors.toList()));
            resourceState.setBigRollAgingStocks(
                    rollingContextManager.restoreBigRollAllocations(
                            rollingResources, input.getBigRollAgingStocks()));
            this.applyMaintenanceAvailability(resourceState, machineSnapshot, shift);
            Cd15MachineTailState inheritedTail = resourceState.getTailByMachine()
                    .get(request.getMachineCode());
            if (inheritedTail != null) {
                tailByMachine.put(request.getMachineCode(), inheritedTail);
            }
            boolean forceChangeQtyShift = changeQtyPlan != null
                    && changeQtyPlan.targetQtyByClass.containsKey(classIndex);
            boolean forceTransferShift = transferPlan != null
                    && transferPlan.classIndexes.contains(classIndex);
            boolean forceRollCurrentShift = forceChangeQtyShift || forceTransferShift;
            if (forceChangeQtyShift) {
                this.clearChangeQtyTargetClass(changeQtyPlan, classIndex, changedById, replacementLanes);
            }
            Double insertQuantity = transferPlan == null
                    ? (Double) request.getFieldValueByFieldName(
                            String.format("class%dPlanQty", classIndex)) : null;
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
                    request, machineSnapshot, workingResults, insertResult, carryovers,
                    hasInsert ? BigDecimal.valueOf(insertQuantity) : BigDecimal.ZERO,
                    insertOrder, changedById, tailByMachine, resourceState,
                    input, sourceLanesByResult, replacementLanes, insertLanes,
                    carryoverImpacts);
            carryovers = rollingResult.carryovers;
            rollingContextManager.completeShift(rollingResources, resourceState);
            log.info("[斜裁插单] 班次滚动完成, classField={}, inserted={}, taskCount={}, carryoverCount={}",
                    shift.getClassField(), hasInsert, rollingResult.taskCount, carryovers.size());
        }
        List<Cd15ScheduleResult> insertedResults = newResultCandidates.stream()
                .filter(this::hasScheduledQuantity)
                .collect(Collectors.toList());
        if (changeQtyPlan != null) {
            carryoverImpacts.stream()
                    .filter(item -> "INSERT".equals(item.getAffectedType()))
                    .forEach(item -> item.setAffectedType("CHANGE_QTY"));
            List<Cd15InsertLaneAllocationDraft> changedLanes = changedById.keySet().stream()
                    .flatMap(id -> replacementLanes.getOrDefault(id, Collections.emptyList()).stream())
                    .collect(Collectors.toCollection(ArrayList::new));
            return Cd15InsertRollingOutput.builder().context(context).batchNo(batchNo)
                    .insertedResults(Collections.emptyList())
                    .updatedResults(new ArrayList<>(changedById.values()))
                    .deletedResults(new ArrayList<>(deletedById.values()))
                    .laneAllocations(changedLanes)
                    .unscheduledResults(this.toUnscheduled(request, batchNo, carryovers))
                    .carryoverImpacts(carryoverImpacts).build();
        }
        if (insertedResults.isEmpty()) {
            List<Cd15RollingPendingTask> insertCarryovers = carryovers.stream()
                    .filter(Cd15RollingPendingTask::isHardInsert)
                    .collect(Collectors.toList());
            List<Cd15InsertCarryoverImpact> insertImpacts = carryoverImpacts.stream()
                    .filter(item -> "INSERT".equals(item.getAffectedType()))
                    .collect(Collectors.toList());
            List<Cd15InsertLaneAllocationDraft> changedLanes = changedById.keySet().stream()
                    .flatMap(id -> replacementLanes.getOrDefault(id, Collections.emptyList()).stream())
                    .collect(Collectors.toCollection(ArrayList::new));
            return Cd15InsertRollingOutput.builder().context(context).batchNo(batchNo)
                    .insertedResults(Collections.emptyList())
                    .updatedResults(transferRequest == null ? Collections.emptyList()
                            : new ArrayList<>(changedById.values()))
                    .deletedResults(transferRequest == null ? Collections.emptyList()
                            : new ArrayList<>(deletedById.values()))
                    .laneAllocations(transferRequest == null ? Collections.emptyList() : changedLanes)
                    .unscheduledResults(this.toUnscheduled(request, batchNo, insertCarryovers))
                    .carryoverImpacts(insertImpacts).build();
        }
        List<Cd15InsertLaneAllocationDraft> affectedLanes = changedById.keySet().stream()
                .flatMap(id -> replacementLanes.getOrDefault(id, Collections.emptyList()).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        affectedLanes.addAll(insertLanes);
        return Cd15InsertRollingOutput.builder().context(context).batchNo(batchNo)
                .insertedResults(insertedResults)
                .updatedResults(new ArrayList<>(changedById.values()))
                .deletedResults(new ArrayList<>(deletedById.values()))
                .laneAllocations(affectedLanes)
                .unscheduledResults(this.toUnscheduled(request, batchNo, carryovers))
                .carryoverImpacts(carryoverImpacts).build();
    }

    /**
     * 调量先定位原排程结果，把指定班次的原计划量清空，再按原顺位写入目标量参与滚动。
     */
    private ChangeQtyPlan prepareChangeQtySource(Cd15ChangeQtyRequest changeQtyRequest,
                                                 Cd15InsertOrderRequest rollingRequest,
                                                 List<Cd15ScheduleResult> workingResults) {
        if (changeQtyRequest.getScheduleResultId() == null) {
            throw new IllegalStateException("调量必须指定排程结果ID");
        }
        Cd15ScheduleResult targetResult = workingResults.stream()
                .filter(item -> Objects.equals(
                        item.getId(), changeQtyRequest.getScheduleResultId()))
                .filter(item -> changeQtyRequest.getMachineCode().equals(item.getMachineCode()))
                .filter(item -> changeQtyRequest.getSteelStripCode().equals(item.getSteelStripCode()))
                .findFirst().orElseThrow(() -> new IllegalStateException("未找到可调量的斜裁排程结果"));
        Map<Integer, BigDecimal> targetQtyByClass = this.resolveChangeQtyTargets(changeQtyRequest);
        if (this.hasText(changeQtyRequest.getGroupNo())
                && !Objects.equals(changeQtyRequest.getGroupNo(), targetResult.getGroupNo())) {
            throw new IllegalStateException("调量分裁组号与排程结果不一致");
        }
        List<Cd15ScheduleResult> adjustmentGroup = this.resolveAdjustmentGroup(
                workingResults, targetResult);
        boolean splitCut = Cd15CutMode.SPLIT.equals(this.cutMode(targetResult));
        targetQtyByClass.forEach((classIndex, targetQuantity) -> {
            if (splitCut && targetQuantity.signum() <= 0) {
                throw new IllegalStateException("分裁组合调量后的目标数量必须大于0");
            }
            if (adjustmentGroup.stream().anyMatch(
                    result -> this.isLocked(result, classIndex))) {
                throw new IllegalStateException("已锁定或已生产的班次计划不能调量");
            }
            if (splitCut && adjustmentGroup.stream()
                    .anyMatch(result -> this.readPlan(result, classIndex).signum() <= 0)) {
                throw new IllegalStateException("分裁组合在目标班次缺少配对计划，不能调量");
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
    private Map<Integer, BigDecimal> resolveChangeQtyTargets(Cd15ChangeQtyRequest request) {
        Map<Integer, BigDecimal> targetQtyByClass = new LinkedHashMap<>();
        if (request.getStartClassField() != null && request.getTargetPlanQty() != null) {
            targetQtyByClass.put(this.classIndex(request.getStartClassField()),
                    BigDecimal.valueOf(request.getTargetPlanQty()));
        }
        IntStream.rangeClosed(1, 8).forEach(classIndex -> {
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
            if (classIndex < 1 || classIndex > 8 || targetQuantity.signum() < 0) {
                throw new IllegalStateException("调量班次或目标计划量不合法");
            }
        });
        return targetQtyByClass;
    }

    /** 清空调量目标记录在当前班次的旧计划量，后续由滚动内核按目标量重写。 */
    private void clearChangeQtyTargetClass(ChangeQtyPlan changeQtyPlan,
                                           int classIndex,
                                           Map<Long, Cd15ScheduleResult> changedById,
                                           Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes) {
        Cd15ScheduleResult targetResult = changeQtyPlan.targetResult;
        targetResult.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex), null);
        targetResult.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), null);
        targetResult.setFieldValueByFieldName(String.format("class%dAnalysis", classIndex), null);
        changedById.put(targetResult.getId(), targetResult);
        replacementLanes.computeIfAbsent(targetResult.getId(), key -> new ArrayList<>())
                .removeIf(lane -> ("CLASS" + classIndex).equals(lane.getClassField()));
    }
    /**
     * 转机台先按结果ID解析单裁或完整分裁组，再为目标机台创建对应的新结果。
     */
    private TransferPlan prepareTransferSource(
            Cd15TransferMachineRequest transferRequest,
            List<Cd15ScheduleResult> workingResults,
            Map<Long, Cd15ScheduleResult> changedById,
            Map<Long, Cd15ScheduleResult> deletedById,
            Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes) {
        if (transferRequest.getSourceMachineCode().equals(transferRequest.getTargetMachineCode())) {
            throw new IllegalStateException("原机台和目标机台不能相同");
        }
        if (transferRequest.getScheduleResultId() == null) {
            throw new IllegalStateException("转机台必须指定排程结果ID");
        }
        List<Integer> transferClassIndexes = IntStream.rangeClosed(1, 8)
                .filter(classIndex -> this.readTransferProduceOrder(transferRequest, classIndex) != null)
                .boxed().collect(Collectors.toList());
        if (transferClassIndexes.isEmpty()) {
            throw new IllegalStateException("没有可转走的班次计划");
        }
        Cd15ScheduleResult selectedResult = workingResults.stream()
                .filter(item -> Objects.equals(
                        item.getId(), transferRequest.getScheduleResultId()))
                .filter(item -> transferRequest.getSourceMachineCode().equals(item.getMachineCode()))
                .filter(item -> transferRequest.getSteelStripCode().equals(item.getSteelStripCode()))
                .findFirst().orElseThrow(() ->
                        new IllegalStateException("原机台没有选中的钢带计划"));
        if (this.hasText(transferRequest.getGroupNo())
                && !Objects.equals(transferRequest.getGroupNo(), selectedResult.getGroupNo())) {
            throw new IllegalStateException("转机台分裁组号与排程结果不一致");
        }
        List<Cd15ScheduleResult> transferSources = this.resolveAdjustmentGroup(
                workingResults, selectedResult);
        List<Cd15ScheduleResult> targetResults = transferSources.stream()
                .map(source -> this.newTransferResult(
                        source, transferRequest.getTargetMachineCode(),
                        transferRequest.getRemark()))
                .collect(Collectors.toList());
        transferClassIndexes.forEach(classIndex -> {
            if (transferSources.stream().anyMatch(
                    item -> this.readPlan(item, classIndex).signum() <= 0)) {
                throw new IllegalStateException(
                        "转机台目标班次缺少单裁计划或完整分裁配对计划");
            }
            boolean lockedTransfer = transferSources.stream()
                    .anyMatch(item -> this.isLocked(item, classIndex));
            if (lockedTransfer) {
                throw new IllegalStateException("已锁定或已生产的班次计划不能转机台");
            }
            int targetOrder = this.resolveTransferProduceOrder(
                    transferRequest, workingResults, classIndex);
            for (int index = 0; index < transferSources.size(); index++) {
                Cd15ScheduleResult source = transferSources.get(index);
                Cd15ScheduleResult target = targetResults.get(index);
                target.setFieldValueByFieldName(
                        String.format("class%dPlanQty", classIndex),
                        this.readPlan(source, classIndex).doubleValue());
                target.setFieldValueByFieldName(
                        String.format("class%dProduceOrder", classIndex), targetOrder);
                target.setFieldValueByFieldName(
                        String.format("class%dAnalysisInput", classIndex), "转机台");
            }
        });
        this.compactSourceMachineOrders(workingResults, transferSources, transferRequest.getSourceMachineCode(),
                transferClassIndexes, changedById);
        this.clearTransferSourceClasses(workingResults, transferSources, transferClassIndexes,
                changedById, deletedById, replacementLanes);
        return new TransferPlan(targetResults, transferClassIndexes);
    }

    private Integer readTransferProduceOrder(Cd15TransferMachineRequest transferRequest,
                                             int classIndex) {
        Integer produceOrder = (Integer) transferRequest.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
        return produceOrder != null && produceOrder > 0 ? produceOrder : null;
    }

    private int resolveTransferProduceOrder(Cd15TransferMachineRequest transferRequest,
                                            List<Cd15ScheduleResult> workingResults,
                                            int classIndex) {
        Integer produceOrder = (Integer) transferRequest.getFieldValueByFieldName(String.format(
                "class%dProduceOrder", classIndex));
        if (produceOrder != null && produceOrder > 0) {
            return produceOrder;
        }
        return this.nextTargetProduceOrder(workingResults, transferRequest.getTargetMachineCode(), classIndex);
    }

    private int nextTargetProduceOrder(List<Cd15ScheduleResult> workingResults,
                                       String targetMachineCode,
                                       int classIndex) {
        return workingResults.stream()
                .filter(item -> targetMachineCode.equals(item.getMachineCode()))
                .map(item -> readOrder(item, classIndex))
                .filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
    }

    private void clearTransferSourceClasses(List<Cd15ScheduleResult> workingResults,
                                            List<Cd15ScheduleResult> transferSources,
                                            List<Integer> transferClassIndexes,
                                            Map<Long, Cd15ScheduleResult> changedById,
                                            Map<Long, Cd15ScheduleResult> deletedById,
                                            Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes) {
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

    private void compactSourceMachineOrders(List<Cd15ScheduleResult> workingResults,
                                            List<Cd15ScheduleResult> transferSources,
                                            String sourceMachineCode,
                                            List<Integer> transferClassIndexes,
                                            Map<Long, Cd15ScheduleResult> changedById) {
        for (Integer classIndex : transferClassIndexes) {
            final int currentClassIndex = classIndex;
            List<Integer> removedOrders = transferSources.stream()
                    .filter(item -> readPlan(item, currentClassIndex).signum() > 0)
                    .map(item -> readOrder(item, currentClassIndex))
                    .filter(Objects::nonNull)
                    .distinct()
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
    private ShiftRollingResult rollShift(Cd15AutoScheduleContext context,
                                          Cd15ShiftDescriptor shift,
                                          int classIndex,
                                          Cd15InsertOrderRequest request,
                                          Cd15MachineResourceSnapshot machineSnapshot,
                                          List<Cd15ScheduleResult> workingResults,
                                          Cd15ScheduleResult insertResult,
                                          List<Cd15RollingPendingTask> incomingCarryovers,
                                          BigDecimal insertQuantity,
                                          Integer insertOrder,
                                          Map<Long, Cd15ScheduleResult> changedById,
                                          Map<String, Cd15MachineTailState> tailByMachine,
                                          Cd15ShiftResourceState resourceState,
                                          Cd15AutoScheduleInput input,
                                          Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanesByResult,
                                          Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes,
                                          List<Cd15InsertLaneAllocationDraft> insertLanes,
                                          List<Cd15InsertCarryoverImpact> carryoverImpacts) {
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
        Cd15MachineTailState previousTail = tailByMachine.get(request.getMachineCode());
        orderSegments(segments, insertOrder, previousTail);

        int fullSeconds = Math.max(1, shift.getDurationSeconds());
        int remainingSeconds = resourceState.getRemainingSecondsByMachine()
                .getOrDefault(request.getMachineCode(), 0);
        List<Cd15RollingPendingTask> carryovers = new ArrayList<>();
        int nextOrder = 1;
        boolean deferSuffix = false;
        String deferReason = null;
        Set<String> processedSplitGroups = new HashSet<>();
        for (Segment segment : segments) {
            if (deferSuffix) {
                Cd15RollingPendingTask pendingTask = toPending(segment, shift,
                        nextClassField(context, classIndex), segment.quantity, deferReason);
                carryovers.add(pendingTask);
                carryoverImpacts.add(this.toCarryoverImpact(
                        pendingTask, shift.getClassField()));
                continue;
            }
            if (Cd15CutMode.SPLIT.equals(this.cutMode(segment.result))) {
                String groupNo = segment.result.getGroupNo();
                if (!this.hasText(groupNo)) {
                    throw new IllegalStateException("分裁滚动任务缺少组号");
                }
                List<Segment> splitSegments = segments.stream()
                        .filter(item -> Cd15CutMode.SPLIT.equals(
                                this.cutMode(item.result)))
                        .filter(item -> Objects.equals(
                                groupNo, item.result.getGroupNo()))
                        .collect(Collectors.toList());
                if (splitSegments.size() > 2) {
                    throw new IllegalStateException("分裁组只能包含一条或两条结果");
                }
                if (splitSegments.size() == 2) {
                    if (!processedSplitGroups.add(groupNo)) {
                        continue;
                    }
                SplitRollingOutcome splitOutcome = this.rollSplitGroup(
                        context, shift, classIndex, request, machineSnapshot,
                        splitSegments, nextOrder, remainingSeconds, previousTail,
                        changedById, resourceState, input, replacementLanes,
                        sourceLanesByResult,
                        insertLanes, carryovers, carryoverImpacts);
                remainingSeconds = splitOutcome.remainingSeconds;
                previousTail = splitOutcome.previousTail;
                nextOrder = splitOutcome.nextOrder;
                if (splitOutcome.deferSuffix) {
                    deferSuffix = true;
                    deferReason = splitOutcome.deferReason;
                }
                continue;
                }
            }
            int assignedOrder = segment.locked && segment.order != null
                    ? segment.order : nextOrder;
            if (segment.locked) {
                nextOrder = Math.max(nextOrder, assignedOrder + 1);
            }
            int remainingSecondsBeforeTrial = remainingSeconds;
            Cd15MachineTailState previousTailBeforeTrial = previousTail;
            Cd15ConstructionMaterial taskMaterial = this.requireMaterial(input, segment.result);
            boolean splitCut = Cd15CutMode.SPLIT.equals(
                    this.cutMode(segment.result));
            Cd15MachineResource machine = this.requireTargetMachine(
                    request.getMachineCode(), segment.result, taskMaterial,
                    shift, machineSnapshot, context);
            if (splitCut) {
                this.validateSplitWidth(machine, machineSnapshot,
                        segment.result.getCuttingAngle(), taskMaterial, taskMaterial);
            }
            BigDecimal shiftCapacity = this.machineModeResolver.capacity(machine, splitCut);
            if (shiftCapacity == null || shiftCapacity.signum() <= 0) {
                throw new IllegalStateException(
                        "目标机台当前裁断模式未维护有效班产能力: "
                                + request.getMachineCode());
            }
            LocalDateTime originalStart = shift.getStartTime().plusSeconds(
                    Math.max(0, fullSeconds - remainingSecondsBeforeTrial));
            BigDecimal requestedBigRollConsume = this.bigRollMeterCalculator
                    .calculateForPlanQuantity(segment.quantity,
                            taskMaterial.getUnitConsumeMillimeter(),
                            taskMaterial.getCraftWidth(),
                            taskMaterial.getCordWidth());
            String bigRollFailureReason = this.bigRollFailureReason(
                    input, segment.result, resourceState);
            Cd15BigRollAgingAllocation agingPreview =
                    bigRollFailureReason == null
                            ? this.bigRollAgingAllocator.preview(
                                    resourceState.getBigRollAgingStocks(),
                                    segment.result.getBigRollCode(),
                                    requestedBigRollConsume, originalStart)
                            : null;
            if (bigRollFailureReason == null
                    && (agingPreview == null || !agingPreview.isSuccess())) {
                bigRollFailureReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
            }
            int agingDelaySeconds = agingPreview == null
                    ? 0 : agingPreview.getDelaySeconds();
            int availableSeconds = Math.max(
                    0, remainingSecondsBeforeTrial - agingDelaySeconds);
            LocalDateTime expectedStart = agingPreview == null
                    ? originalStart : agingPreview.getTaskStartTime();
            LocalDateTime expectedEnd = expectedStart;
            BigDecimal scheduled;
            String limitReason = null;
            Cd15MachineCapacityTrial capacityTrial = null;
            if (bigRollFailureReason != null) {
                if (segment.locked) {
                    throw new IllegalStateException(
                            "锁定任务大卷资源不可用: " + segment.result.getBigRollCode());
                }
                scheduled = BigDecimal.ZERO;
                limitReason = bigRollFailureReason;
            } else if (segment.locked) {
                scheduled = segment.quantity;
                Cd15MachineTailState currentTail = this.tailState(segment.result, input);
                capacityTrial = capacityCalculator.calculateWithRemainingSeconds(
                        shiftCapacity, Math.max(1, shift.getDurationSeconds() / 3600),
                        availableSeconds,
                        previousTail, currentTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        segment.quantity);
                if (!capacityTrial.isFullyAccommodated()) {
                    throw new IllegalStateException(
                            "锁定任务超过目标机台当前班次剩余产能");
                }
                remainingSeconds = capacityTrial.getRemainingSeconds();
                expectedEnd = expectedStart.plusSeconds(
                        capacityTrial.getChangeSeconds()
                                + capacityTrial.getProductionSeconds());
                previousTail = currentTail;
            } else if (availableSeconds <= 0) {
                scheduled = BigDecimal.ZERO;
                limitReason = "CAPACITY_LIMIT";
            } else {
                Cd15MachineTailState currentTail = this.tailState(segment.result, input);
                capacityTrial = capacityCalculator.calculateWithRemainingSeconds(
                        shiftCapacity, Math.max(1, shift.getDurationSeconds() / 3600),
                        availableSeconds,
                        previousTail, currentTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        segment.quantity);
                scheduled = splitCut
                        ? this.roundSingleSpecSplitDown(
                                capacityTrial.getCapacityQuantity(),
                                taskMaterial.getCraftWidth())
                        : capacityTrial.getCapacityQuantity();
                remainingSeconds = capacityTrial.getRemainingSeconds();
                expectedEnd = expectedStart.plusSeconds(
                        capacityTrial.getChangeSeconds()
                                + capacityTrial.getProductionSeconds());
                if (!capacityTrial.isFullyAccommodated()) {
                    limitReason = "CAPACITY_LIMIT";
                }
                previousTail = currentTail;
            }
            LaneCommit laneCommit = segment.locked
                    ? reserveLockedLanes(segment, shift, scheduled, resourceState,
                            sourceLanesByResult, input, context)
                    : splitCut
                            ? this.allocateSingleSpecSplitLanes(segment, shift,
                                    scheduled, resourceState, input, context)
                            : allocateLanes(segment, shift, scheduled,
                                    resourceState, input, context);
            scheduled = splitCut ? laneCommit.quantity
                    : normalizeScheduledQuantity(laneCommit.quantity);
            if (!segment.locked && scheduled.signum() <= 0) {
                remainingSeconds = remainingSecondsBeforeTrial;
                previousTail = previousTailBeforeTrial;
            }
            if (scheduled.signum() <= 0 && segment.quantity.signum() > 0
                    && laneCommit.limitReason != null) {
                limitReason = laneCommit.limitReason;
            } else if (scheduled.compareTo(segment.quantity) < 0
                    && laneCommit.limitReason != null) {
                limitReason = laneCommit.limitReason;
            }
            BigDecimal bigRollConsumeQuantity = BigDecimal.ZERO;
            if (scheduled.signum() > 0) {
                if (capacityTrial != null) {
                    int productionSeconds = this.scaledProductionSeconds(
                            capacityTrial, scheduled);
                    remainingSeconds = Math.max(0, remainingSecondsBeforeTrial
                            - agingDelaySeconds - capacityTrial.getChangeSeconds()
                            - productionSeconds);
                    expectedEnd = expectedStart.plusSeconds(
                            capacityTrial.getChangeSeconds() + productionSeconds);
                }
                bigRollConsumeQuantity = this.bigRollMeterCalculator
                        .calculateForPlanQuantity(scheduled,
                                taskMaterial.getUnitConsumeMillimeter(),
                                taskMaterial.getCraftWidth(),
                                taskMaterial.getCordWidth());
                Cd15BigRollAgingAllocation allocation = this.bigRollAgingAllocator.allocate(
                        resourceState.getBigRollAgingStocks(),
                        segment.result.getBigRollCode(),
                        bigRollConsumeQuantity, originalStart);
                if (!allocation.isSuccess()) {
                    throw new IllegalStateException(
                            "大卷资源试算成功后提交失败: "
                                    + segment.result.getBigRollCode());
                }
                this.addBigRollConsumption(
                        segment.result, bigRollConsumeQuantity);
                if (!segment.locked) {
                    nextOrder = Math.max(nextOrder, assignedOrder + 1);
                    writeClass(segment.result, classIndex, shift, scheduled, assignedOrder,
                            limitReason, request);
                    if (segment.result.getId() != null) {
                        changedById.put(segment.result.getId(), segment.result);
                    }
                    List<Cd15InsertLaneAllocationDraft> targetLanes = segment.result.getId() == null
                            ? insertLanes : replacementLanes.computeIfAbsent(
                                    segment.result.getId(), key -> new ArrayList<>());
                    targetLanes.addAll(toLaneDrafts(segment, shift, scheduled,
                            laneCommit.allocations));
                }
                resourceState.getTasks().add(Cd15ShiftScheduleTask.builder()
                        .classField(shift.getClassField())
                        .sourceTaskKey(this.taskKey(segment.result,
                                shift.getClassField(), assignedOrder))
                        .sourceResultId(segment.result.getId())
                        .materialKey(this.materialKey(taskMaterial))
                        .steelStripCode(segment.result.getSteelStripCode())
                        .bigRollCode(segment.result.getBigRollCode())
                        .cuttingAngle(segment.result.getCuttingAngle())
                        .craftWidth(taskMaterial.getCraftWidth())
                        .unitConsumeMillimeter(
                                taskMaterial.getUnitConsumeMillimeter())
                        .cordWidth(taskMaterial.getCordWidth())
                        .curlLength(taskMaterial.getCurlLength())
                        .bigRollConsumeQuantity(bigRollConsumeQuantity)
                        .cutMode(this.cutMode(segment.result))
                        .splitGroupKey(segment.result.getGroupNo())
                        .cordSpec(segment.result.getSteelStripCode())
                        .machineCode(segment.result.getMachineCode())
                        .planQuantity(scheduled)
                        .vehicleCount(laneCommit.vehicleCount)
                        .produceOrder(assignedOrder)
                        .expectedStartTime(expectedStart)
                        .expectedEndTime(expectedEnd)
                        .laneAllocations(laneCommit.allocations).build());
            }
            BigDecimal remaining = segment.quantity.subtract(scheduled).max(BigDecimal.ZERO);
            if (remaining.signum() > 0) {
                Cd15RollingPendingTask pendingTask = toPending(segment, shift,
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
        resourceState.getRemainingSecondsByMachine().put(
                request.getMachineCode(), remainingSeconds);
        if (previousTail != null) {
            tailByMachine.put(request.getMachineCode(), previousTail);
            resourceState.getTailByMachine().put(request.getMachineCode(), previousTail);
        }
        return new ShiftRollingResult(carryovers, segments.size());
    }

    /** 将分裁组合按一个作业单元提交，任一资源不足时两条一起顺延。 */
    private SplitRollingOutcome rollSplitGroup(
            Cd15AutoScheduleContext context,
            Cd15ShiftDescriptor shift,
            int classIndex,
            Cd15InsertOrderRequest request,
            Cd15MachineResourceSnapshot machineSnapshot,
            List<Segment> splitSegments,
            int nextOrder,
            int remainingSeconds,
            Cd15MachineTailState previousTail,
            Map<Long, Cd15ScheduleResult> changedById,
            Cd15ShiftResourceState resourceState,
            Cd15AutoScheduleInput input,
            Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes,
            Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanesByResult,
            List<Cd15InsertLaneAllocationDraft> insertLanes,
            List<Cd15RollingPendingTask> carryovers,
            List<Cd15InsertCarryoverImpact> carryoverImpacts) {
        if (splitSegments.size() != 2) {
            throw new IllegalStateException("分裁滚动任务必须包含两条结果");
        }
        Segment first = splitSegments.get(0);
        Segment second = splitSegments.get(1);
        this.validateSplitSegments(first, second);
        boolean locked = first.locked && second.locked;
        if (first.locked != second.locked) {
            throw new IllegalStateException("分裁组合两条结果的锁定状态不一致");
        }
        int assignedOrder = locked
                ? this.requireCommonSplitOrder(first, second) : nextOrder;
        Cd15ConstructionMaterial firstMaterial = this.requireMaterial(
                input, first.result);
        Cd15ConstructionMaterial secondMaterial = this.requireMaterial(
                input, second.result);
        Cd15MachineResource firstMachine = this.requireTargetMachine(
                request.getMachineCode(), first.result, firstMaterial, shift,
                machineSnapshot, context);
        Cd15MachineResource secondMachine = this.requireTargetMachine(
                request.getMachineCode(), second.result, secondMaterial, shift,
                machineSnapshot, context);
        if (!Objects.equals(firstMachine.getMachineCode(), secondMachine.getMachineCode())) {
            throw new IllegalStateException("分裁组合两条结果未命中同一目标机台");
        }
        this.validateSplitWidth(firstMachine, machineSnapshot,
                first.result.getCuttingAngle(), firstMaterial, secondMaterial);
        BigDecimal firstShiftCapacity = this.machineModeResolver.capacity(firstMachine, true);
        BigDecimal secondShiftCapacity = this.machineModeResolver.capacity(secondMachine, true);
        if (firstShiftCapacity == null || firstShiftCapacity.signum() <= 0
                || secondShiftCapacity == null || secondShiftCapacity.signum() <= 0) {
            throw new IllegalStateException("目标机台未维护有效分裁班产能力");
        }
        Cd15MachineTailState splitTail = this.splitTail(first, second);
        int shiftHours = Math.max(1, shift.getDurationSeconds() / 3600);
        Cd15MachineCapacityTrial firstCapacity = capacityCalculator
                .calculateWithRemainingSeconds(firstShiftCapacity, shiftHours,
                        remainingSeconds, previousTail, splitTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        first.quantity);
        Cd15MachineCapacityTrial secondCapacity = capacityCalculator
                .calculateWithRemainingSeconds(secondShiftCapacity, shiftHours,
                        remainingSeconds, previousTail, splitTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        second.quantity);
        if (!firstCapacity.isFullyAccommodated()
                || !secondCapacity.isFullyAccommodated()) {
            return this.deferSplitGroup(context, shift, classIndex,
                    splitSegments, nextOrder, remainingSeconds, previousTail,
                    carryovers, carryoverImpacts, "CAPACITY_LIMIT");
        }
        String bigRollFailure = this.bigRollFailureReason(
                input, first.result, resourceState);
        if (bigRollFailure != null) {
            return this.deferSplitGroup(context, shift, classIndex,
                    splitSegments, nextOrder, remainingSeconds, previousTail,
                    carryovers, carryoverImpacts, bigRollFailure);
        }
        if (locked) {
            return this.commitLockedSplitGroup(context, shift, classIndex,
                    splitSegments, firstMaterial, secondMaterial,
                    firstCapacity, secondCapacity, assignedOrder,
                    nextOrder, remainingSeconds, splitTail, resourceState,
                    input, sourceLanesByResult);
        }
        Cd15ShiftCommitRequest firstCommitRequest = this.splitCommitRequest(
                context, shift, first, firstMaterial, firstCapacity,
                firstMachine.getMachineCode());
        Cd15ShiftCommitRequest secondCommitRequest = this.splitCommitRequest(
                context, shift, second, secondMaterial, secondCapacity,
                secondMachine.getMachineCode());
        Cd15SplitShiftCommitResult splitCommit = shiftResourceCommitter.commitSplit(
                firstCommitRequest, secondCommitRequest, resourceState);
        if (!splitCommit.isSuccess()
                || !this.isCompleteSplitCommit(splitCommit, first, second)) {
            String failureReason = splitCommit.getFailureReason() == null
                    ? "SPLIT_GROUP_LIMIT" : splitCommit.getFailureReason();
            return this.deferSplitGroup(context, shift, classIndex,
                    splitSegments, nextOrder, remainingSeconds, previousTail,
                    carryovers, carryoverImpacts, failureReason);
        }
        List<Cd15ShiftScheduleTask> tasks = java.util.Arrays.asList(
                splitCommit.getFirstTask(), splitCommit.getSecondTask());
        for (int index = 0; index < splitSegments.size(); index++) {
            Segment segment = splitSegments.get(index);
            Cd15ShiftScheduleTask task = tasks.get(index);
            task.setSourceTaskKey(this.taskKey(
                    segment.result, shift.getClassField(), assignedOrder));
            task.setSourceResultId(segment.result.getId());
            task.setProduceOrder(assignedOrder);
            this.addBigRollConsumption(segment.result,
                    task.getBigRollConsumeQuantity());
            this.writeClass(segment.result, classIndex, shift,
                    task.getPlanQuantity(), assignedOrder, null, request);
            if (segment.result.getId() != null) {
                changedById.put(segment.result.getId(), segment.result);
            }
            List<Cd15InsertLaneAllocationDraft> targetLanes =
                    segment.result.getId() == null
                            ? insertLanes : replacementLanes.computeIfAbsent(
                                    segment.result.getId(), key -> new ArrayList<>());
            targetLanes.addAll(this.toLaneDrafts(segment, shift,
                    task.getPlanQuantity(), task.getLaneAllocations()));
        }
        this.applyShiftState(resourceState, splitCommit.getState());
        Cd15MachineTailState committedTail = resourceState.getTailByMachine()
                .get(firstMachine.getMachineCode());
        int committedRemaining = resourceState.getRemainingSecondsByMachine()
                .getOrDefault(firstMachine.getMachineCode(), remainingSeconds);
        return new SplitRollingOutcome(committedRemaining, committedTail,
                Math.max(nextOrder, assignedOrder + 1), false, null);
    }

    private SplitRollingOutcome commitLockedSplitGroup(
            Cd15AutoScheduleContext context,
            Cd15ShiftDescriptor shift,
            int classIndex,
            List<Segment> splitSegments,
            Cd15ConstructionMaterial firstMaterial,
            Cd15ConstructionMaterial secondMaterial,
            Cd15MachineCapacityTrial firstCapacity,
            Cd15MachineCapacityTrial secondCapacity,
            int assignedOrder,
            int nextOrder,
            int remainingSeconds,
            Cd15MachineTailState splitTail,
            Cd15ShiftResourceState resourceState,
            Cd15AutoScheduleInput input,
            Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanesByResult) {
        Segment first = splitSegments.get(0);
        Segment second = splitSegments.get(1);
        Cd15ShiftResourceState working = this.copyShiftState(resourceState);
        BigDecimal firstConsume = bigRollMeterCalculator.calculateForPlanQuantity(
                first.quantity, firstMaterial.getUnitConsumeMillimeter(),
                firstMaterial.getCraftWidth(), firstMaterial.getCordWidth());
        BigDecimal secondConsume = bigRollMeterCalculator.calculateForPlanQuantity(
                second.quantity, secondMaterial.getUnitConsumeMillimeter(),
                secondMaterial.getCraftWidth(), secondMaterial.getCordWidth());
        int fullSeconds = Math.max(1, shift.getDurationSeconds());
        LocalDateTime originalStart = shift.getStartTime().plusSeconds(
                Math.max(0, fullSeconds - remainingSeconds));
        Cd15BigRollAgingAllocation agingAllocation = bigRollAgingAllocator.allocate(
                working.getBigRollAgingStocks(), first.result.getBigRollCode(),
                firstConsume.add(secondConsume), originalStart);
        if (!agingAllocation.isSuccess()) {
            throw new IllegalStateException("锁定分裁组合大卷库存或成熟时间不足");
        }
        LaneCommit firstLanes = this.reserveLockedLanes(first, shift,
                first.quantity, working, sourceLanesByResult, input, context);
        LaneCommit secondLanes = this.reserveLockedLanes(second, shift,
                second.quantity, working, sourceLanesByResult, input, context);
        if (firstLanes.quantity.compareTo(first.quantity) != 0
                || secondLanes.quantity.compareTo(second.quantity) != 0
                || working.getOccupiedToolingCount() > working.getTotalToolingCount()) {
            throw new IllegalStateException("锁定分裁组合库排或工装资源不足");
        }
        int changeSeconds = Math.max(firstCapacity.getChangeSeconds(),
                secondCapacity.getChangeSeconds());
        int productionSeconds = Math.max(firstCapacity.getProductionSeconds(),
                secondCapacity.getProductionSeconds());
        int occupiedSeconds = agingAllocation.getDelaySeconds()
                + changeSeconds + productionSeconds;
        if (occupiedSeconds > remainingSeconds) {
            throw new IllegalStateException("锁定分裁组合超过目标机台当前班次剩余产能");
        }
        LocalDateTime expectedStart = agingAllocation.getTaskStartTime();
        LocalDateTime expectedEnd = expectedStart.plusSeconds(
                changeSeconds + productionSeconds);
        Cd15ShiftScheduleTask firstTask = this.manualSplitTask(
                first, firstMaterial, firstLanes, firstConsume, shift,
                assignedOrder, expectedStart, expectedEnd);
        Cd15ShiftScheduleTask secondTask = this.manualSplitTask(
                second, secondMaterial, secondLanes, secondConsume, shift,
                assignedOrder, expectedStart, expectedEnd);
        working.getTasks().add(firstTask);
        working.getTasks().add(secondTask);
        working.getRemainingSecondsByMachine().put(first.result.getMachineCode(),
                remainingSeconds - occupiedSeconds);
        working.getTailByMachine().put(first.result.getMachineCode(), splitTail);
        working.getTailSpecByMachine().put(first.result.getMachineCode(),
                first.result.getSteelStripCode() + "+" + second.result.getSteelStripCode());
        this.applyShiftState(resourceState, working);
        return new SplitRollingOutcome(remainingSeconds - occupiedSeconds,
                splitTail, Math.max(nextOrder, assignedOrder + 1), false, null);
    }

    private Cd15ShiftCommitRequest splitCommitRequest(
            Cd15AutoScheduleContext context,
            Cd15ShiftDescriptor shift,
            Segment segment,
            Cd15ConstructionMaterial material,
            Cd15MachineCapacityTrial capacity,
            String machineCode) {
        BigDecimal curlLength = material.getCurlLength() == null
                || material.getCurlLength().signum() <= 0
                ? context.getParameters().getRollCoilMeter()
                : material.getCurlLength();
        BigDecimal vehicleQuantity = vehiclePlanQuantityCalculator.calculate(
                material.getUnitConsumeMillimeter(), material.getCraftWidth(),
                curlLength);
        Cd15MachineTrial trial = Cd15MachineTrial.builder()
                .machineCode(machineCode).actualQuantity(segment.quantity)
                .vehiclePlanQuantity(vehicleQuantity)
                .capacityQuantity(capacity.getCapacityQuantity())
                .finalSchedulableQuantity(segment.quantity)
                .fullyAccommodated(true)
                .changeSeconds(capacity.getChangeSeconds())
                .productionSeconds(capacity.getProductionSeconds())
                .remainingSeconds(capacity.getRemainingSeconds()).build();
        return Cd15ShiftCommitRequest.builder()
                .materialKey(this.materialKey(material))
                .steelStripCode(segment.result.getSteelStripCode())
                .bigRollCode(segment.result.getBigRollCode())
                .cordSpec(segment.result.getSteelStripCode())
                .cuttingAngle(segment.result.getCuttingAngle())
                .craftWidth(material.getCraftWidth())
                .unitConsumeMillimeter(material.getUnitConsumeMillimeter())
                .cordWidth(material.getCordWidth()).curlLength(curlLength)
                .cutMode(Cd15CutMode.SPLIT)
                .splitGroupKey(segment.result.getGroupNo())
                .classField(shift.getClassField())
                .shiftStart(shift.getStartTime()).shiftEnd(shift.getEndTime())
                .closeOut(false)
                .partialMinVehicleCount(
                        context.getParameters().getPartialMinVehicleCount())
                .trialPlan(Cd15MachineTrialPlan.builder()
                        .trials(Collections.singletonList(trial))
                        .selectedTrial(trial).build()).build();
    }

    private Cd15ShiftScheduleTask manualSplitTask(
            Segment segment,
            Cd15ConstructionMaterial material,
            LaneCommit lanes,
            BigDecimal bigRollConsumeQuantity,
            Cd15ShiftDescriptor shift,
            int assignedOrder,
            LocalDateTime expectedStart,
            LocalDateTime expectedEnd) {
        return Cd15ShiftScheduleTask.builder()
                .classField(shift.getClassField())
                .sourceTaskKey(this.taskKey(
                        segment.result, shift.getClassField(), assignedOrder))
                .sourceResultId(segment.result.getId())
                .materialKey(this.materialKey(material))
                .steelStripCode(segment.result.getSteelStripCode())
                .bigRollCode(segment.result.getBigRollCode())
                .cuttingAngle(segment.result.getCuttingAngle())
                .craftWidth(material.getCraftWidth())
                .unitConsumeMillimeter(material.getUnitConsumeMillimeter())
                .cordWidth(material.getCordWidth()).curlLength(material.getCurlLength())
                .bigRollConsumeQuantity(bigRollConsumeQuantity)
                .cutMode(Cd15CutMode.SPLIT)
                .splitGroupKey(segment.result.getGroupNo())
                .cordSpec(segment.result.getSteelStripCode())
                .machineCode(segment.result.getMachineCode())
                .planQuantity(segment.quantity).vehicleCount(lanes.vehicleCount)
                .produceOrder(assignedOrder).expectedStartTime(expectedStart)
                .expectedEndTime(expectedEnd).laneAllocations(lanes.allocations).build();
    }

    private SplitRollingOutcome deferSplitGroup(
            Cd15AutoScheduleContext context,
            Cd15ShiftDescriptor shift,
            int classIndex,
            List<Segment> splitSegments,
            int nextOrder,
            int remainingSeconds,
            Cd15MachineTailState previousTail,
            List<Cd15RollingPendingTask> carryovers,
            List<Cd15InsertCarryoverImpact> carryoverImpacts,
            String reasonCode) {
        if (splitSegments.stream().anyMatch(segment -> segment.locked)) {
            throw new IllegalStateException("锁定分裁组合无法完整保留: " + reasonCode);
        }
        for (Segment segment : splitSegments) {
            Cd15RollingPendingTask pendingTask = this.toPending(segment, shift,
                    this.nextClassField(context, classIndex),
                    segment.quantity, reasonCode);
            carryovers.add(pendingTask);
            carryoverImpacts.add(this.toCarryoverImpact(
                    pendingTask, shift.getClassField()));
        }
        boolean hardInsert = splitSegments.stream()
                .anyMatch(segment -> segment.hardInsert);
        return new SplitRollingOutcome(remainingSeconds, previousTail,
                nextOrder, hardInsert, hardInsert ? reasonCode : null);
    }

    private boolean isCompleteSplitCommit(Cd15SplitShiftCommitResult commit,
                                          Segment first,
                                          Segment second) {
        return commit.getFirstTask() != null && commit.getSecondTask() != null
                && commit.getFirstTask().getPlanQuantity() != null
                && commit.getSecondTask().getPlanQuantity() != null
                && commit.getFirstTask().getPlanQuantity().compareTo(
                        this.normalizeScheduledQuantity(first.quantity)) == 0
                && commit.getSecondTask().getPlanQuantity().compareTo(
                        this.normalizeScheduledQuantity(second.quantity)) == 0;
    }

    private void validateSplitSegments(Segment first, Segment second) {
        if (first == null || second == null
                || first.quantity == null || first.quantity.signum() <= 0
                || second.quantity == null || second.quantity.signum() <= 0
                || Objects.equals(first.result.getSteelStripCode(),
                        second.result.getSteelStripCode())
                || !Objects.equals(first.result.getGroupNo(), second.result.getGroupNo())
                || !Objects.equals(first.result.getMachineCode(), second.result.getMachineCode())
                || !Objects.equals(first.result.getBigRollCode(), second.result.getBigRollCode())
                || !Objects.equals(first.result.getCuttingAngle(), second.result.getCuttingAngle())) {
            throw new IllegalStateException("分裁组合必须是同组、同机台、同大卷、同角度的两条不同钢带任务");
        }
    }

    private int requireCommonSplitOrder(Segment first, Segment second) {
        if (first.order == null || first.order <= 0
                || !Objects.equals(first.order, second.order)) {
            throw new IllegalStateException("锁定分裁组合两条结果必须共用生产顺序");
        }
        return first.order;
    }

    private Cd15MachineTailState splitTail(Segment first, Segment second) {
        return Cd15MachineTailState.builder()
                .materialKey(first.result.getGroupNo())
                .steelStripCode(first.result.getSteelStripCode()
                        + "+" + second.result.getSteelStripCode())
                .bigRollCode(first.result.getBigRollCode())
                .cuttingAngle(first.result.getCuttingAngle()).build();
    }

    private void validateSplitWidth(Cd15MachineResource machine,
                                    Cd15MachineResourceSnapshot snapshot,
                                    String cuttingAngle,
                                    Cd15ConstructionMaterial first,
                                    Cd15ConstructionMaterial second) {
        BigDecimal combinedWidth = first.getCraftWidth().add(second.getCraftWidth());
        BigDecimal angleMaximum = snapshot.getAngleWidthMaxByAngle() == null
                ? null : snapshot.getAngleWidthMaxByAngle().get(cuttingAngle);
        if (angleMaximum == null || angleMaximum.signum() <= 0
                || combinedWidth.compareTo(angleMaximum) > 0
                || (machine.getClothWidthMax() != null
                && combinedWidth.compareTo(machine.getClothWidthMax()) > 0)
                || (machine.getClothWidthMin() != null
                && combinedWidth.compareTo(machine.getClothWidthMin()) < 0)) {
            throw new IllegalStateException("分裁组合宽度不满足角度或机台宽度范围");
        }
    }

    private Cd15ShiftResourceState copyShiftState(Cd15ShiftResourceState source) {
        List<Cd15StorageLaneState> lanes = source.getLanes() == null
                ? new ArrayList<>() : source.getLanes().stream()
                        .map(item -> Cd15StorageLaneState.builder()
                                .laneCode(item.getLaneCode())
                                .steelStripCode(item.getSteelStripCode())
                                .vehicleCount(item.getVehicleCount())
                                .maxVehicleCount(item.getMaxVehicleCount()).build())
                        .collect(Collectors.toList());
        Map<String, Cd15MachineTailState> tails = new HashMap<>();
        if (source.getTailByMachine() != null) {
            source.getTailByMachine().forEach((machineCode, tail) ->
                    tails.put(machineCode, tail == null ? null
                            : Cd15MachineTailState.builder()
                                    .materialKey(tail.getMaterialKey())
                                    .steelStripCode(tail.getSteelStripCode())
                                    .bigRollCode(tail.getBigRollCode())
                                    .cuttingAngle(tail.getCuttingAngle()).build()));
        }
        List<Cd15BigRollAgingStock> agingStocks = source.getBigRollAgingStocks() == null
                ? new ArrayList<>() : source.getBigRollAgingStocks().stream()
                        .map(item -> Cd15BigRollAgingStock.builder()
                                .sourceType(item.getSourceType())
                                .sourceId(item.getSourceId())
                                .bigRollCode(item.getBigRollCode())
                                .bigRollBarcode(item.getBigRollBarcode())
                                .availableQuantity(item.getAvailableQuantity())
                                .allocatedQuantity(item.getAllocatedQuantity())
                                .stockInTime(item.getStockInTime())
                                .releaseTime(item.getReleaseTime()).build())
                        .collect(Collectors.toList());
        return Cd15ShiftResourceState.builder().lanes(lanes)
                .totalToolingCount(source.getTotalToolingCount())
                .occupiedToolingCount(source.getOccupiedToolingCount())
                .remainingSecondsByMachine(source.getRemainingSecondsByMachine() == null
                        ? new HashMap<>()
                        : new HashMap<>(source.getRemainingSecondsByMachine()))
                .tailSpecByMachine(source.getTailSpecByMachine() == null
                        ? new HashMap<>() : new HashMap<>(source.getTailSpecByMachine()))
                .tailByMachine(tails)
                .tasks(source.getTasks() == null
                        ? new ArrayList<>() : new ArrayList<>(source.getTasks()))
                .bigRollAgingStocks(agingStocks).build();
    }

    private void applyShiftState(Cd15ShiftResourceState target,
                                 Cd15ShiftResourceState source) {
        target.setLanes(source.getLanes());
        target.setTotalToolingCount(source.getTotalToolingCount());
        target.setOccupiedToolingCount(source.getOccupiedToolingCount());
        target.setRemainingSecondsByMachine(source.getRemainingSecondsByMachine());
        target.setTailSpecByMachine(source.getTailSpecByMachine());
        target.setTailByMachine(source.getTailByMachine());
        target.setTasks(source.getTasks());
        target.setBigRollAgingStocks(source.getBigRollAgingStocks());
    }

    /** 检修时长从当前班可用秒数中扣除。 */
    private void applyMaintenanceAvailability(
            Cd15ShiftResourceState state,
            Cd15MachineResourceSnapshot snapshot,
            Cd15ShiftDescriptor shift) {
        if (state == null || state.getRemainingSecondsByMachine() == null
                || snapshot == null || snapshot.getMachines() == null) {
            return;
        }
        snapshot.getMachines().stream()
                .filter(Objects::nonNull)
                .filter(machine -> machine.getMachineCode() != null)
                .forEach(machine -> state.getRemainingSecondsByMachine().put(
                        machine.getMachineCode(),
                        Math.max(0, shift.getDurationSeconds()
                                - machine.getMaintenanceSeconds())));
    }

    /** 按自动排程相同的全部硬约束确认目标机台可用于当前任务。 */
    private Cd15MachineResource requireTargetMachine(
            String targetMachineCode,
            Cd15ScheduleResult result,
            Cd15ConstructionMaterial material,
            Cd15ShiftDescriptor shift,
            Cd15MachineResourceSnapshot snapshot,
            Cd15AutoScheduleContext context) {
        Cd15MachineCandidateResolution resolution =
                this.machineCandidateResolver.resolveDetailed(
                        result.getSteelStripCode(), result.getBigRollCode(),
                        material.getCraftWidth(), result.getCuttingAngle(),
                        snapshot.getAngleWidthMaxByAngle(), shift.getShiftCode(),
                        shift.getStartTime(), shift.getEndTime(),
                        snapshot.getMachines(), snapshot.getBindings(),
                        snapshot.getRestrictions(),
                        context.getParameters().getMachinePriority());
        boolean candidateMatched = resolution.getCandidates() != null
                && resolution.getCandidates().stream()
                .anyMatch(candidate -> Objects.equals(
                        targetMachineCode, candidate.getMachineCode()));
        Cd15MachineResource machine = snapshot.getMachines().stream()
                .filter(item -> Objects.equals(
                        targetMachineCode, item.getMachineCode()))
                .findFirst().orElse(null);
        boolean splitCut = Cd15CutMode.SPLIT.equals(this.cutMode(result));
        if (!candidateMatched || !this.machineModeResolver.matches(machine, splitCut)) {
            throw new IllegalStateException(
                    "目标机台不满足当前班次硬约束, machineCode="
                            + targetMachineCode + ", reason="
                            + resolution.getFailureReason());
        }
        return machine;
    }

    private Cd15ConstructionMaterial requireMaterial(
            Cd15AutoScheduleInput input, Cd15ScheduleResult result) {
        Cd15ConstructionMaterial material = this.findMaterial(input, result);
        if (material == null || material.getCraftWidth() == null
                || material.getCraftWidth().signum() <= 0
                || material.getUnitConsumeMillimeter() == null
                || material.getUnitConsumeMillimeter().signum() <= 0) {
            throw new IllegalStateException(
                    "排程结果未匹配到完整施工材料: "
                            + (result == null ? null : result.getSteelStripCode()));
        }
        return material;
    }

    private String cutMode(Cd15ScheduleResult result) {
        String mode = result == null || result.getCutMode() == null
                ? "" : result.getCutMode().trim().toUpperCase();
        if (!Cd15CutMode.SINGLE.equals(mode)
                && !Cd15CutMode.SPLIT.equals(mode)) {
            throw new IllegalStateException(
                    "排程结果裁断模式必须为SINGLE或SPLIT");
        }
        return mode;
    }

    private String bigRollFailureReason(
            Cd15AutoScheduleInput input,
            Cd15ScheduleResult result,
            Cd15ShiftResourceState state) {
        if (input != null && input.getBigRollAgingDataMissingCodes() != null
                && input.getBigRollAgingDataMissingCodes().contains(
                        result.getBigRollCode())) {
            return "BIG_ROLL_STOCK_DATA_MISSING";
        }
        if (state == null || state.getBigRollAgingStocks() == null
                || state.getBigRollAgingStocks().isEmpty()) {
            return Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
        }
        return null;
    }

    private BigDecimal reserveBigRoll(
            Cd15AutoScheduleInput input,
            Cd15ShiftResourceState state,
            Cd15ScheduleResult result,
            Cd15ConstructionMaterial material,
            BigDecimal quantity,
            LocalDateTime originalStart) {
        String failureReason = this.bigRollFailureReason(input, result, state);
        if (failureReason != null) {
            throw new IllegalStateException(
                    "原排程任务大卷资源不可用: " + failureReason);
        }
        BigDecimal consumption = this.bigRollMeterCalculator
                .calculateForPlanQuantity(quantity,
                        material.getUnitConsumeMillimeter(),
                        material.getCraftWidth(), material.getCordWidth());
        Cd15BigRollAgingAllocation allocation = this.bigRollAgingAllocator.allocate(
                state.getBigRollAgingStocks(), result.getBigRollCode(),
                consumption, originalStart);
        if (!allocation.isSuccess()) {
            throw new IllegalStateException(
                    "原排程任务大卷库存或成熟时间不足: "
                            + result.getBigRollCode());
        }
        this.addBigRollConsumption(result, consumption);
        return consumption;
    }

    private void addBigRollConsumption(
            Cd15ScheduleResult result, BigDecimal consumption) {
        BigDecimal existing = result.getBigRollConsumeQty() == null
                ? BigDecimal.ZERO : result.getBigRollConsumeQty();
        result.setBigRollConsumeQty(existing.add(consumption));
    }

    private int scaledProductionSeconds(
            Cd15MachineCapacityTrial trial, BigDecimal scheduledQuantity) {
        if (trial == null || trial.getProductionSeconds() <= 0
                || trial.getCapacityQuantity() == null
                || trial.getCapacityQuantity().signum() <= 0) {
            return 0;
        }
        return scheduledQuantity.multiply(
                        BigDecimal.valueOf(trial.getProductionSeconds()))
                .divide(trial.getCapacityQuantity(), 0, RoundingMode.CEILING)
                .intValueExact();
    }

    /** 非受影响机台按原库排明细占用本班资源，并作为计划入库滚入下一班。 */
    private void reserveUnaffectedTasks(Cd15ShiftResourceState state,
                                        Cd15ShiftDescriptor shift,
                                        int classIndex,
                                        List<Cd15ScheduleResult> results,
                                        Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanes,
                                        Cd15AutoScheduleInput input,
                                        Cd15AutoScheduleContext context,
                                        String affectedMachineCode) {
        results.stream()
                .filter(item -> readPlan(item, classIndex).signum() > 0)
                .filter(item -> affectedMachineCode == null
                        || !affectedMachineCode.equals(item.getMachineCode()))
                .sorted(Comparator.comparing(item -> readOrder(item, classIndex),
                        Comparator.nullsLast(Integer::compareTo)))
                .forEach(item -> {
                    BigDecimal quantity = readPlan(item, classIndex);
                    Cd15ConstructionMaterial taskMaterial =
                            this.requireMaterial(input, item);
                    BigDecimal bigRollConsumeQuantity = this.reserveBigRoll(
                            input, state, item, taskMaterial, quantity,
                            shift.getStartTime());
                    LaneCommit laneCommit = reserveExistingLanes(item, shift, quantity, state,
                            sourceLanes.getOrDefault(item.getId(), Collections.emptyList()),
                            input, context);
                    state.getTasks().add(Cd15ShiftScheduleTask.builder()
                            .classField(shift.getClassField())
                            .sourceTaskKey(this.taskKey(item,
                                    shift.getClassField(), readOrder(item, classIndex)))
                            .sourceResultId(item.getId())
                            .materialKey(this.materialKey(taskMaterial))
                            .steelStripCode(item.getSteelStripCode())
                            .bigRollCode(item.getBigRollCode())
                            .cuttingAngle(item.getCuttingAngle())
                            .craftWidth(taskMaterial.getCraftWidth())
                            .unitConsumeMillimeter(
                                    taskMaterial.getUnitConsumeMillimeter())
                            .cordWidth(taskMaterial.getCordWidth())
                            .curlLength(taskMaterial.getCurlLength())
                            .bigRollConsumeQuantity(bigRollConsumeQuantity)
                            .cutMode(this.cutMode(item))
                            .splitGroupKey(item.getGroupNo())
                            .cordSpec(item.getSteelStripCode())
                            .machineCode(item.getMachineCode()).planQuantity(quantity)
                            .vehicleCount(laneCommit.vehicleCount)
                            .produceOrder(readOrder(item, classIndex) == null
                                    ? 1 : readOrder(item, classIndex))
                            .expectedStartTime(shift.getStartTime())
                            .expectedEndTime(shift.getEndTime())
                            .laneAllocations(laneCommit.allocations).build());
                    state.getTailByMachine().put(
                            item.getMachineCode(), this.tailState(item, input));
                });
    }

    private LaneCommit reserveLockedLanes(Segment segment,
                                          Cd15ShiftDescriptor shift,
                                          BigDecimal quantity,
                                          Cd15ShiftResourceState state,
                                          Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanes,
                                          Cd15AutoScheduleInput input,
                                          Cd15AutoScheduleContext context) {
        return reserveExistingLanes(segment.result, shift, quantity, state,
                sourceLanes.getOrDefault(segment.result.getId(), Collections.emptyList()),
                input, context);
    }

    private LaneCommit reserveExistingLanes(Cd15ScheduleResult result,
                                            Cd15ShiftDescriptor shift,
                                            BigDecimal quantity,
                                            Cd15ShiftResourceState state,
                                            List<Cd15ScheduleLaneAllocation> sourceLanes,
                                            Cd15AutoScheduleInput input,
                                            Cd15AutoScheduleContext context) {
        List<Cd15ScheduleLaneAllocation> rows = sourceLanes.stream()
                .filter(item -> shift.getClassField().equals(item.getClassField()))
                .sorted(Comparator.comparing(Cd15ScheduleLaneAllocation::getAllocationOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            return allocateLanes(Segment.existing(result, classIndex(shift.getClassField()),
                    quantity, null, true), shift, quantity, state, input, context);
        }
        // 预检查：原库排若已被其他钢带占用（mergeInbound 跳过入库留下的脏数据），整体降级为重新分配
        // 与 Cd15ResourceSnapshotBuilder.mergeInbound 的软冲突策略对齐，避免整个插单中断
        List<String> conflictedLaneCodes = rows.stream()
                .map(row -> {
                    Cd15StorageLaneState lane = state.getLanes().stream()
                            .filter(item -> row.getStorageLaneCode().equals(item.getLaneCode()))
                            .findFirst().orElse(null);
                    if (lane == null || lane.getSteelStripCode() == null
                            || lane.getSteelStripCode().trim().isEmpty()) {
                        return null;
                    }
                    return result.getSteelStripCode().equals(lane.getSteelStripCode())
                            ? null : row.getStorageLaneCode();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!conflictedLaneCodes.isEmpty()) {
            log.warn("[斜裁插单] 原排程库排已被其他钢带占用,降级为重新分配, steelStripCode={}, conflictedLanes={}",
                    result.getSteelStripCode(), conflictedLaneCodes);
            return allocateLanes(Segment.existing(result, classIndex(shift.getClassField()),
                    quantity, null, true), shift, quantity, state, input, context);
        }
        List<Cd15StorageLaneAllocation> allocations = rows.stream().map(row -> {
            Cd15StorageLaneState lane = state.getLanes().stream()
                    .filter(item -> row.getStorageLaneCode().equals(item.getLaneCode()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "原排程库排不存在于当前资源快照: " + row.getStorageLaneCode()));
            int vehicles = row.getAllocatedCartCount() == null ? 0 : row.getAllocatedCartCount();
            if (vehicles <= 0 || lane.getVehicleCount() + vehicles > lane.getMaxVehicleCount()) {
                throw new IllegalStateException("原排程库排资源已变化，无法保持锁定任务: "
                        + row.getStorageLaneCode());
            }
            if (lane.getSteelStripCode() != null && !lane.getSteelStripCode().trim().isEmpty()
                    && !result.getSteelStripCode().equals(lane.getSteelStripCode())) {
                throw new IllegalStateException("原排程库排已被其他钢带占用: "
                        + row.getStorageLaneCode());
            }
            lane.setSteelStripCode(result.getSteelStripCode());
            lane.setVehicleCount(lane.getVehicleCount() + vehicles);
            return Cd15StorageLaneAllocation.builder()
                    .laneCode(row.getStorageLaneCode()).vehicleCount(vehicles).build();
        }).collect(Collectors.toList());
        int vehicleCount = allocations.stream().mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        state.setOccupiedToolingCount(state.getOccupiedToolingCount() + vehicleCount);
        return new LaneCommit(quantity, allocations, vehicleCount, null);
    }

    private LaneCommit allocateLanes(Segment segment,
                                     Cd15ShiftDescriptor shift,
                                     BigDecimal requestedQuantity,
                                     Cd15ShiftResourceState state,
                                     Cd15AutoScheduleInput input,
                                     Cd15AutoScheduleContext context) {
        if (requestedQuantity == null || requestedQuantity.signum() <= 0) {
            return LaneCommit.empty();
        }
        Cd15ConstructionMaterial material = findMaterial(input, segment.result);
        if (material == null || material.getCraftWidth() == null
                || material.getUnitConsumeMillimeter() == null) {
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
        BigDecimal curlLength = material.getCurlLength() == null
                || material.getCurlLength().signum() <= 0 ? fallback : material.getCurlLength();
        BigDecimal vehicleQuantity = vehiclePlanQuantityCalculator.calculate(
                material.getUnitConsumeMillimeter(), material.getCraftWidth(), curlLength);
        int availableTooling = Math.max(0,
                state.getTotalToolingCount() - state.getOccupiedToolingCount());
        if (availableTooling <= 0) {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0, "ROLL_TOOL_LIMIT");
        }
        BigDecimal toolingQuantity = vehicleQuantity.multiply(BigDecimal.valueOf(availableTooling));
        BigDecimal trialQuantity = requestedQuantity.min(toolingQuantity);
        Cd15StorageLaneAllocationResult allocation = laneAllocator.allocate(
                segment.result.getSteelStripCode(), trialQuantity, vehicleQuantity,
                state.getLanes(), segment.hardInsert);
        if (!allocation.isSuccess() || allocation.getAllocatedVehicleCount() <= 0) {
            return new LaneCommit(BigDecimal.ZERO, Collections.emptyList(), 0,
                    allocation.getFailureReason());
        }
        BigDecimal laneQuantity = vehicleQuantity
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

    /** 单条分裁结果按两路相同规格原子分配库排和小车工装。 */
    private LaneCommit allocateSingleSpecSplitLanes(
            Segment segment,
            Cd15ShiftDescriptor shift,
            BigDecimal requestedQuantity,
            Cd15ShiftResourceState state,
            Cd15AutoScheduleInput input,
            Cd15AutoScheduleContext context) {
        if (requestedQuantity == null || requestedQuantity.signum() <= 0) {
            return LaneCommit.empty();
        }
        Cd15ConstructionMaterial material = this.findMaterial(
                input, segment.result);
        if (material == null || material.getCraftWidth() == null
                || material.getUnitConsumeMillimeter() == null) {
            return new LaneCommit(BigDecimal.ZERO,
                    Collections.emptyList(), 0, "CONSTRUCTION_MISSING");
        }
        BigDecimal fallback = context == null ? BigDecimal.valueOf(1000)
                : context.getParameters().getRollCoilMeter();
        BigDecimal curlLength = material.getCurlLength() == null
                || material.getCurlLength().signum() <= 0
                ? fallback : material.getCurlLength();
        BigDecimal vehicleQuantity = this.vehiclePlanQuantityCalculator.calculate(
                material.getUnitConsumeMillimeter(), material.getCraftWidth(),
                curlLength);
        int availableTooling = Math.max(0,
                state.getTotalToolingCount() - state.getOccupiedToolingCount());
        int availablePairCount = availableTooling / 2;
        if (availablePairCount <= 0) {
            return new LaneCommit(BigDecimal.ZERO,
                    Collections.emptyList(), 0, "ROLL_TOOL_LIMIT");
        }
        BigDecimal toolingQuantity = vehicleQuantity
                .multiply(new BigDecimal("2"))
                .multiply(BigDecimal.valueOf(availablePairCount));
        BigDecimal trialQuantity = requestedQuantity.min(toolingQuantity);
        BigDecimal branchTrialQuantity = trialQuantity.divide(
                new BigDecimal("2"), 10, RoundingMode.UNNECESSARY);
        Cd15ShiftResourceState preview = this.copyShiftState(state);
        Cd15StorageLaneAllocationResult firstPreview = this.laneAllocator.allocate(
                segment.result.getSteelStripCode(), branchTrialQuantity,
                vehicleQuantity, preview.getLanes(), segment.hardInsert);
        if (!firstPreview.isSuccess()) {
            return new LaneCommit(BigDecimal.ZERO,
                    Collections.emptyList(), 0, firstPreview.getFailureReason());
        }
        preview.setLanes(firstPreview.getLanes());
        Cd15StorageLaneAllocationResult secondPreview = this.laneAllocator.allocate(
                segment.result.getSteelStripCode(), branchTrialQuantity,
                vehicleQuantity, preview.getLanes(), segment.hardInsert);
        if (!secondPreview.isSuccess()) {
            return new LaneCommit(BigDecimal.ZERO,
                    Collections.emptyList(), 0, secondPreview.getFailureReason());
        }
        int pairVehicleCount = Math.min(
                firstPreview.getAllocatedVehicleCount(),
                secondPreview.getAllocatedVehicleCount());
        if (pairVehicleCount <= 0) {
            return new LaneCommit(BigDecimal.ZERO,
                    Collections.emptyList(), 0, "STORAGE_LANE_LIMIT");
        }
        BigDecimal branchCommittedQuantity = branchTrialQuantity.min(
                vehicleQuantity.multiply(BigDecimal.valueOf(pairVehicleCount)));
        Cd15ShiftResourceState working = this.copyShiftState(state);
        Cd15StorageLaneAllocationResult first = this.laneAllocator.allocate(
                segment.result.getSteelStripCode(), branchCommittedQuantity,
                vehicleQuantity, working.getLanes(), segment.hardInsert);
        working.setLanes(first.getLanes());
        Cd15StorageLaneAllocationResult second = this.laneAllocator.allocate(
                segment.result.getSteelStripCode(), branchCommittedQuantity,
                vehicleQuantity, working.getLanes(), segment.hardInsert);
        if (!first.isSuccess() || !second.isSuccess()) {
            return new LaneCommit(BigDecimal.ZERO,
                    Collections.emptyList(), 0, "STORAGE_LANE_LIMIT");
        }
        int totalVehicleCount = pairVehicleCount * 2;
        working.setLanes(second.getLanes());
        working.setOccupiedToolingCount(
                working.getOccupiedToolingCount() + totalVehicleCount);
        this.applyShiftState(state, working);
        List<Cd15StorageLaneAllocation> allocations =
                this.mergeSameSpecSplitAllocations(
                        first.getAllocations(), second.getAllocations());
        BigDecimal committedQuantity = branchCommittedQuantity
                .multiply(new BigDecimal("2"));
        String reason = committedQuantity.compareTo(requestedQuantity) < 0
                ? toolingQuantity.compareTo(requestedQuantity) < 0
                        ? "ROLL_TOOL_LIMIT" : "STORAGE_LANE_LIMIT"
                : null;
        return new LaneCommit(committedQuantity, allocations,
                totalVehicleCount, reason);
    }

    /** 将同规格两路分配合并为一条结果的库排明细。 */
    private List<Cd15StorageLaneAllocation> mergeSameSpecSplitAllocations(
            List<Cd15StorageLaneAllocation> first,
            List<Cd15StorageLaneAllocation> second) {
        Map<String, Integer> vehicleCountByLane = new LinkedHashMap<>();
        List<Cd15StorageLaneAllocation> source = new ArrayList<>();
        if (first != null) {
            source.addAll(first);
        }
        if (second != null) {
            source.addAll(second);
        }
        source.forEach(item -> vehicleCountByLane.merge(
                item.getLaneCode(), item.getVehicleCount(), Integer::sum));
        return vehicleCountByLane.entrySet().stream()
                .map(entry -> Cd15StorageLaneAllocation.builder()
                        .laneCode(entry.getKey())
                        .vehicleCount(entry.getValue()).build())
                .collect(Collectors.toList());
    }

    /** 将可排总量向下归整为完整的同规格一出二双片步长。 */
    private BigDecimal roundSingleSpecSplitDown(
            BigDecimal quantity, BigDecimal craftWidthMillimeter) {
        BigDecimal pairQuantity = craftWidthMillimeter
                .multiply(new BigDecimal("2"))
                .divide(new BigDecimal("1000"), 10, RoundingMode.HALF_UP);
        return quantity.divide(pairQuantity, 0, RoundingMode.FLOOR)
                .multiply(pairQuantity);
    }

    /** 插单滚动与自动排程提交层保持一致，最终计划量按整数米向上取整。 */
    private BigDecimal normalizeScheduledQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return quantity.setScale(0, RoundingMode.CEILING);
    }

    private List<Cd15InsertLaneAllocationDraft> toLaneDrafts(
            Segment segment, Cd15ShiftDescriptor shift, BigDecimal quantity,
            List<Cd15StorageLaneAllocation> allocations) {
        int totalVehicles = allocations.stream()
                .mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        BigDecimal[] remaining = {quantity};
        List<Cd15InsertLaneAllocationDraft> drafts = new ArrayList<>();
        for (int index = 0; index < allocations.size(); index++) {
            Cd15StorageLaneAllocation allocation = allocations.get(index);
            BigDecimal allocationQuantity = index == allocations.size() - 1
                    ? remaining[0] : quantity.multiply(BigDecimal.valueOf(allocation.getVehicleCount()))
                            .divide(BigDecimal.valueOf(totalVehicles), 10, RoundingMode.HALF_UP);
            remaining[0] = remaining[0].subtract(allocationQuantity);
            drafts.add(Cd15InsertLaneAllocationDraft.builder()
                    .scheduleResultId(segment.result.getId())
                    .insertResult(segment.result.getId() == null)
                    .newResultKey(segment.result.getId() == null
                            ? this.newResultKey(segment.result) : null)
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

    private Cd15InsertLaneAllocationDraft copyLaneDraft(Cd15ScheduleLaneAllocation source) {
        return Cd15InsertLaneAllocationDraft.builder()
                .scheduleResultId(source.getScheduleResultId()).insertResult(false)
                .classField(source.getClassField())
                .shiftScheduleDate(source.getShiftScheduleDate())
                .laneCode(source.getStorageLaneCode())
                .allocationQuantity(source.getAllocatedQty() == null
                        ? BigDecimal.ZERO : source.getAllocatedQty())
                .vehicleCount(source.getAllocatedCartCount() == null
                        ? 0 : source.getAllocatedCartCount())
                .allocationOrder(source.getAllocationOrder() == null
                        ? 1 : source.getAllocationOrder()).build();
    }

    private Map<String, BigDecimal> buildCurlLengthBySteelStrip(
            Cd15AutoScheduleInput input, Cd15AutoScheduleContext context) {
        if (input == null || input.getConstructionMaterials() == null) {
            return new HashMap<>();
        }
        BigDecimal fallback = context.getParameters().getRollCoilMeter();
        return input.getConstructionMaterials().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getSteelStripCode() != null)
                .collect(Collectors.toMap(Cd15ConstructionMaterial::getSteelStripCode,
                        item -> item.getCurlLength() == null
                                || item.getCurlLength().signum() <= 0
                                ? fallback : item.getCurlLength(),
                        (first, second) -> first));
    }

    /** 将插单钢带施工补入按成型计划加载的班次输入，供库排和工装试算使用。 */
    private void ensureInsertMaterial(Cd15AutoScheduleInput input,
                                      Cd15ConstructionMaterial insertMaterial) {
        if (input == null || insertMaterial == null) {
            return;
        }
        List<Cd15ConstructionMaterial> materials = new ArrayList<>(
                input.getConstructionMaterials() == null
                        ? Collections.emptyList() : input.getConstructionMaterials());
        boolean exists = materials.stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> Objects.equals(item.getSteelStripCode(), insertMaterial.getSteelStripCode())
                        && Objects.equals(item.getBigRollCode(), insertMaterial.getBigRollCode())
                        && Objects.equals(item.getCuttingAngle(), insertMaterial.getCuttingAngle())
                        && Objects.equals(item.getCraftWidth(), insertMaterial.getCraftWidth())
                        && Objects.equals(item.getUnitConsumeMillimeter(),
                                insertMaterial.getUnitConsumeMillimeter()));
        if (!exists) {
            materials.add(insertMaterial);
            input.setConstructionMaterials(materials);
        }
    }

    private Cd15ConstructionMaterial findMaterial(Cd15AutoScheduleInput input,
                                                   Cd15ScheduleResult result) {
        if (input == null || input.getConstructionMaterials() == null || result == null) {
            return null;
        }
        return input.getConstructionMaterials().stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(
                        result.getSteelStripCode(), item.getSteelStripCode()))
                .filter(item -> Objects.equals(
                        result.getBigRollCode(), item.getBigRollCode()))
                .filter(item -> Objects.equals(
                        result.getCuttingAngle(), item.getCuttingAngle()))
                .filter(item -> result.getCraftWidth() == null
                        || this.sameDecimal(result.getCraftWidth(), item.getCraftWidth()))
                .filter(item -> result.getUnitConsumeMillimeter() == null
                        || this.sameDecimal(result.getUnitConsumeMillimeter(),
                                item.getUnitConsumeMillimeter()))
                .findFirst().orElse(null);
    }

    private boolean sameDecimal(BigDecimal first, BigDecimal second) {
        return first != null && second != null && first.compareTo(second) == 0;
    }

    private void orderSegments(List<Segment> segments, Integer insertOrder,
                               Cd15MachineTailState previousTail) {
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

    private int continuityRank(Segment segment, Cd15MachineTailState previousTail) {
        if (segment == null || segment.result == null || previousTail == null) {
            return 3;
        }
        boolean sameSpec = Objects.equals(
                previousTail.getSteelStripCode(), segment.result.getSteelStripCode())
                && Objects.equals(
                        previousTail.getCuttingAngle(), segment.result.getCuttingAngle());
        boolean sameRoll = Objects.equals(
                previousTail.getBigRollCode(), segment.result.getBigRollCode());
        if (sameSpec && sameRoll) {
            return 0;
        }
        if (sameSpec) {
            return 1;
        }
        return sameRoll ? 2 : 3;
    }

    private void mergeCarryovers(List<Segment> segments,
                                 List<Cd15RollingPendingTask> carryovers,
                                 List<Cd15ScheduleResult> workingResults,
                                 Cd15ScheduleResult insertResult,
                                 int classIndex) {
        for (Cd15RollingPendingTask pending : carryovers) {
            Cd15ScheduleResult result = this.findPendingResult(
                    pending, workingResults, insertResult);
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

    private Cd15ScheduleResult findPendingResult(
            Cd15RollingPendingTask pending,
            List<Cd15ScheduleResult> workingResults,
            Cd15ScheduleResult insertResult) {
        if (pending.getSourceResultId() != null) {
            return workingResults.stream()
                    .filter(item -> Objects.equals(
                            item.getId(), pending.getSourceResultId()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "跨班待排任务找不到来源排程结果: "
                                    + pending.getTaskKey()));
        }
        List<Cd15ScheduleResult> candidates = new ArrayList<>(workingResults);
        if (insertResult != null && !candidates.contains(insertResult)) {
            candidates.add(insertResult);
        }
        return candidates.stream()
                .filter(item -> item.getId() == null)
                .filter(item -> Objects.equals(
                        pending.getMaterialKey(), this.resultMaterialKey(item)))
                .filter(item -> Objects.equals(
                        pending.getSplitGroupKey(), item.getGroupNo()))
                .filter(item -> Objects.equals(
                        pending.getSteelStripCode(), item.getSteelStripCode()))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "跨班待排任务找不到新增排程结果: "
                                + pending.getTaskKey()));
    }

    private void clearAdjustableClassFields(List<Segment> segments, int classIndex,
                                            Map<Long, Cd15ScheduleResult> changedById,
                                            Map<Long, List<Cd15InsertLaneAllocationDraft>> replacementLanes) {
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

    private void writeClass(Cd15ScheduleResult result, int classIndex,
                            Cd15ShiftDescriptor shift, BigDecimal quantity,
                            int produceOrder, String limitReason,
                            Cd15InsertOrderRequest request) {
        result.setFieldValueByFieldName(String.format("class%dScheduleDate", classIndex),
                Date.from(shift.getStartTime().atZone(ZoneId.systemDefault()).toInstant()));
        result.setFieldValueByFieldName(String.format("class%dPlanQty", classIndex),
                quantity.doubleValue());
        result.setFieldValueByFieldName(String.format("class%dProduceOrder", classIndex), produceOrder);
        String analysis = limitReason == null ? "插单滚动重排"
                : "插单后因" + limitReason + "部分顺延至下一班";
        result.setFieldValueByFieldName(String.format("class%dAnalysis", classIndex), analysis);
        if ("INSERT".equals(result.getSourceType())) {
            result.setFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex),
                    request.getFieldValueByFieldName(String.format("class%dAnalysisInput", classIndex)));
        }
    }

    private Cd15RollingPendingTask toPending(Segment segment, Cd15ShiftDescriptor shift,
                                              String targetClassField,
                                              BigDecimal remaining, String limitReason) {
        return Cd15RollingPendingTask.builder()
                .taskKey(taskKey(segment.result, shift.getClassField(), segment.order))
                .sourceResultId(segment.result.getId())
                .sourceBatchNo(segment.result.getCd15BatchNo())
                .sourceOrderNo(segment.result.getOrderNo())
                .originalClassField(segment.originalClassField)
                .originalProduceOrder(segment.order)
                .targetClassField(targetClassField)
                .materialKey(this.resultMaterialKey(segment.result))
                .steelStripCode(segment.result.getSteelStripCode())
                .bigRollCode(segment.result.getBigRollCode())
                .cuttingAngle(segment.result.getCuttingAngle())
                .craftWidth(segment.result.getCraftWidth())
                .unitConsumeMillimeter(
                        segment.result.getUnitConsumeMillimeter())
                .cordWidth(segment.result.getCordWidth())
                .curlLength(segment.result.getCurlLength())
                .bigRollConsumeQuantity(
                        segment.result.getBigRollConsumeQty())
                .cutMode(this.cutMode(segment.result))
                .splitGroupKey(segment.result.getGroupNo())
                .sourceMachineCode(segment.result.getMachineCode())
                .requiredMachineCode(segment.result.getMachineCode())
                .originalQuantity(segment.quantity)
                .scheduledQuantity(segment.quantity.subtract(remaining))
                .remainingQuantity(remaining)
                .hardInsert(segment.hardInsert).locked(false)
                .continueFromPreviousShift(true).lastLimitReason(limitReason).build();
    }

    /** 将任务级待排转换为前端确认使用的逐班影响明细。 */
    private Cd15InsertCarryoverImpact toCarryoverImpact(Cd15RollingPendingTask pendingTask,
                                                         String sourceClassField) {
        String reasonCode = pendingTask.getLastLimitReason() == null
                ? "SCHEDULE_WINDOW_LIMIT" : pendingTask.getLastLimitReason();
        return Cd15InsertCarryoverImpact.builder()
                .steelStripCode(pendingTask.getSteelStripCode())
                .affectedType(pendingTask.isHardInsert() ? "INSERT" : "EXISTING")
                .sourceClassField(sourceClassField)
                .targetClassField(pendingTask.getTargetClassField())
                .carryoverQty(pendingTask.getRemainingQuantity())
                .reasonCode(reasonCode)
                .build();
    }

    private String nextClassField(Cd15AutoScheduleContext context, int classIndex) {
        return context.getShifts().stream()
                .filter(item -> classIndex(item.getClassField()) > classIndex)
                .map(Cd15ShiftDescriptor::getClassField)
                .findFirst().orElse(null);
    }

    /** 判断插单结果是否至少有一个班次形成正排产量。 */
    private boolean hasScheduledQuantity(Cd15ScheduleResult result) {
        return IntStream.rangeClosed(1, 8)
                .mapToObj(classIndex -> (Double) result.getFieldValueByFieldName(
                        String.format("class%dPlanQty", classIndex)))
                .filter(Objects::nonNull)
                .anyMatch(quantity -> quantity > 0D);
    }

    private List<Cd15UnscheduleResult> toUnscheduled(Cd15InsertOrderRequest request,
                                                      String batchNo,
                                                      List<Cd15RollingPendingTask> carryovers) {
        return carryovers.stream().map(task -> {
            Cd15UnscheduleResult result = new Cd15UnscheduleResult();
            result.setFactoryCode(request.getFactoryCode());
            result.setScheduleDate(request.getScheduleDate());
            result.setSteelStripCode(task.getSteelStripCode());
            result.setBigRollCode(task.getBigRollCode());
            result.setDemandQty(task.getOriginalQuantity());
            result.setScheduledQty(task.getScheduledQuantity());
            result.setUnscheduledQty(task.getRemainingQuantity());
            result.setFailStage("SCHEDULE_WINDOW_END");
            String limitReason = task.getLastLimitReason() == null
                    ? "SCHEDULE_WINDOW_LIMIT" : task.getLastLimitReason();
            result.setUnscheduleReasonCode(this.persistedReasonCode(limitReason));
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
            return "插单钢带施工宽度或单耗数据不完整";
        }
        return "插单滚动至最后班次仍未完全容纳";
    }

    /** 解析人工调整涉及的一条单裁结果或完整的两条分裁结果。 */
    private List<Cd15ScheduleResult> resolveAdjustmentGroup(
            List<Cd15ScheduleResult> results,
            Cd15ScheduleResult selectedResult) {
        String cutMode = this.cutMode(selectedResult);
        if (Cd15CutMode.SINGLE.equals(cutMode)) {
            return Collections.singletonList(selectedResult);
        }
        if (!this.hasText(selectedResult.getGroupNo())) {
            throw new IllegalStateException("分裁排程结果缺少组号");
        }
        List<Cd15ScheduleResult> groupResults = results.stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(
                        selectedResult.getGroupNo(), item.getGroupNo()))
                .filter(item -> Objects.equals(
                        selectedResult.getMachineCode(), item.getMachineCode()))
                .filter(item -> Cd15CutMode.SPLIT.equals(this.cutMode(item)))
                .sorted(Comparator.comparing(Cd15ScheduleResult::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        if (groupResults.size() == 1) {
            return groupResults;
        }
        if (groupResults.size() != 2
                || groupResults.stream().map(Cd15ScheduleResult::getSteelStripCode)
                .filter(Objects::nonNull).distinct().count() != 2L
                || groupResults.stream().map(Cd15ScheduleResult::getBigRollCode)
                .distinct().count() != 1L
                || groupResults.stream().map(Cd15ScheduleResult::getCuttingAngle)
                .distinct().count() != 1L) {
            throw new IllegalStateException("分裁组合必须包含同机台、同大卷、同角度的两条不同钢带结果");
        }
        return groupResults;
    }

    /** 为转机台创建只含待转班次的新主结果副本。 */
    private Cd15ScheduleResult newTransferResult(
            Cd15ScheduleResult source,
            String targetMachineCode,
            String remark) {
        Cd15ScheduleResult target = this.copyResult(source);
        target.setId(null);
        target.setMachineCode(targetMachineCode);
        target.setSourceType("TRANSFER");
        target.setReleaseStatus("0");
        target.setPublishSuccessCount(0);
        target.setNewestPublishTime(null);
        target.setProductionStatus("0");
        target.setIsLocked("0");
        target.setStorageLaneCode(null);
        target.setBigRollConsumeQty(BigDecimal.ZERO);
        target.setRemark(remark);
        IntStream.rangeClosed(1, 8).forEach(classIndex -> {
            String fieldPrefix = String.format("class%d", classIndex);
            target.setFieldValueByFieldName(fieldPrefix + "ScheduleDate", null);
            target.setFieldValueByFieldName(fieldPrefix + "CxPlanQty", null);
            target.setFieldValueByFieldName(fieldPrefix + "PlanQty", null);
            target.setFieldValueByFieldName(fieldPrefix + "FinishQty", null);
            target.setFieldValueByFieldName(fieldPrefix + "ProduceOrder", null);
            target.setFieldValueByFieldName(fieldPrefix + "FinishRate", null);
            target.setFieldValueByFieldName(fieldPrefix + "Analysis", null);
            target.setFieldValueByFieldName(fieldPrefix + "AnalysisInput", null);
        });
        return target;
    }

    /** 按排程结果的完整施工尺寸定位唯一材料。 */
    private Cd15ConstructionMaterial findConstructionMaterial(
            Cd15ScheduleResult result) {
        if (result == null) {
            throw new IllegalStateException("人工调整排程结果不能为空");
        }
        return constructionMapper.selectList(
                        new LambdaQueryWrapper<MdmConstructionInfo>()
                                .eq(MdmConstructionInfo::getFactoryCode,
                                        result.getFactoryCode()))
                .stream().flatMap(item -> constructionMaterialMapper.map(item).stream())
                .filter(item -> Objects.equals(
                        result.getSteelStripCode(), item.getSteelStripCode()))
                .filter(item -> Objects.equals(
                        result.getBigRollCode(), item.getBigRollCode()))
                .filter(item -> Objects.equals(
                        result.getCuttingAngle(), item.getCuttingAngle()))
                .filter(item -> result.getCraftWidth() == null
                        || this.sameDecimal(result.getCraftWidth(), item.getCraftWidth()))
                .filter(item -> result.getUnitConsumeMillimeter() == null
                        || this.sameDecimal(result.getUnitConsumeMillimeter(),
                                item.getUnitConsumeMillimeter()))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "人工调整排程结果未匹配到完整施工材料: "
                                + result.getSteelStripCode()));
    }

    private Cd15ScheduleResult newInsertResult(Cd15InsertOrderRequest request, String batchNo,
                                                Cd15ConstructionMaterial material) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setFactoryCode(request.getFactoryCode());
        result.setScheduleDate(request.getScheduleDate());
        result.setCd15BatchNo(batchNo);
        result.setSteelStripCode(request.getSteelStripCode());
        result.setBigRollCode(material.getBigRollCode());
        result.setCuttingAngle(material.getCuttingAngle());
        result.setCraftWidth(material.getCraftWidth());
        result.setUnitConsumeMillimeter(
                material.getUnitConsumeMillimeter());
        result.setCurlLength(material.getCurlLength());
        result.setCordWidth(material.getCordWidth());
        result.setBigRollConsumeQty(BigDecimal.ZERO);
        result.setCutMode(Cd15CutMode.SINGLE);
        result.setMachineCode(request.getMachineCode());
        result.setSourceType("INSERT");
        result.setReleaseStatus("0");
        result.setPublishSuccessCount(0);
        result.setProductionStatus("0");
        result.setRemark(request.getRemark());
        return result;
    }

    private Cd15ConstructionMaterial findInsertMaterial(Cd15InsertOrderRequest request) {
        return constructionMapper.selectList(new LambdaQueryWrapper<MdmConstructionInfo>()
                        .eq(MdmConstructionInfo::getFactoryCode, request.getFactoryCode()))
                .stream().flatMap(item -> constructionMaterialMapper.map(item).stream())
                .filter(item -> request.getSteelStripCode().equals(item.getSteelStripCode()))
                .filter(item -> !this.hasText(request.getBigRollCode())
                        || request.getBigRollCode().equals(item.getBigRollCode()))
                .filter(item -> !this.hasText(request.getCuttingAngle())
                        || request.getCuttingAngle().equals(item.getCuttingAngle()))
                .filter(item -> item.getBigRollCode() != null && !item.getBigRollCode().trim().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "插单钢带未匹配到有效施工和大卷: " + request.getSteelStripCode()));
    }

    private Cd15ScheduleResult copyResult(Cd15ScheduleResult source) {
        Cd15ScheduleResult target = new Cd15ScheduleResult();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private Cd15MachineTailState tailState(
            Cd15ScheduleResult result, Cd15AutoScheduleInput input) {
        Cd15ConstructionMaterial material = this.findMaterial(input, result);
        return Cd15MachineTailState.builder()
                .materialKey(material == null
                        ? this.resultMaterialKey(result) : this.materialKey(material))
                .steelStripCode(result.getSteelStripCode())
                .bigRollCode(result.getBigRollCode())
                .cuttingAngle(result.getCuttingAngle())
                .build();
    }

    private String resultMaterialKey(Cd15ScheduleResult result) {
        return this.text(result.getSteelStripCode()) + "|"
                + this.text(result.getBigRollCode()) + "|"
                + this.text(result.getCuttingAngle()) + "|"
                + this.decimalText(result.getCraftWidth()) + "|"
                + this.decimalText(result.getUnitConsumeMillimeter()) + "|"
                + this.decimalText(result.getCurlLength());
    }

    private String materialKey(Cd15ConstructionMaterial material) {
        return this.text(material.getSteelStripCode()) + "|"
                + this.text(material.getBigRollCode()) + "|"
                + this.text(material.getCuttingAngle()) + "|"
                + this.decimalText(material.getCraftWidth()) + "|"
                + this.decimalText(material.getUnitConsumeMillimeter()) + "|"
                + this.decimalText(material.getCurlLength());
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isLocked(Cd15ScheduleResult result, int classIndex) {
        Double finish = (Double) result.getFieldValueByFieldName(
                String.format("class%dFinishQty", classIndex));
        Double plan = (Double) result.getFieldValueByFieldName(
                String.format("class%dPlanQty", classIndex));
        return "1".equals(result.getIsLocked())
                || (finish != null && finish > 0D)
                || ("1".equals(result.getProductionStatus())
                && plan != null && (finish == null || finish < plan));
    }

    private BigDecimal readPlan(Cd15ScheduleResult result, int classIndex) {
        Double value = (Double) result.getFieldValueByFieldName(
                String.format("class%dPlanQty", classIndex));
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private Integer readOrder(Cd15ScheduleResult result, int classIndex) {
        return (Integer) result.getFieldValueByFieldName(
                String.format("class%dProduceOrder", classIndex));
    }

    private Integer nextProduceOrder(List<Cd15ScheduleResult> results,
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

    private String taskKey(Cd15ScheduleResult result, String classField, Integer order) {
        String resultIdentity = result.getId() == null
                ? "NEW|" + this.resultMaterialKey(result)
                        + "|" + this.text(result.getGroupNo())
                : result.getId().toString();
        return resultIdentity + "|" + classField + "|" + order;
    }

    private String newResultKey(Cd15ScheduleResult result) {
        return this.resultMaterialKey(result) + "|"
                + this.text(result.getGroupNo()) + "|"
                + this.text(result.getMachineCode());
    }

    private static final class ChangeQtyPlan {
        private final Cd15ScheduleResult targetResult;
        private final Map<Integer, BigDecimal> targetQtyByClass;

        private ChangeQtyPlan(Cd15ScheduleResult targetResult,
                              Map<Integer, BigDecimal> targetQtyByClass) {
            this.targetResult = targetResult;
            this.targetQtyByClass = targetQtyByClass;
        }
    }

    private static final class TransferPlan {
        private final List<Cd15ScheduleResult> targetResults;
        private final List<Integer> classIndexes;

        private TransferPlan(List<Cd15ScheduleResult> targetResults,
                             List<Integer> classIndexes) {
            this.targetResults = targetResults;
            this.classIndexes = classIndexes;
        }
    }

    private static final class SplitRollingOutcome {
        private final int remainingSeconds;
        private final Cd15MachineTailState previousTail;
        private final int nextOrder;
        private final boolean deferSuffix;
        private final String deferReason;

        private SplitRollingOutcome(int remainingSeconds,
                                    Cd15MachineTailState previousTail,
                                    int nextOrder,
                                    boolean deferSuffix,
                                    String deferReason) {
            this.remainingSeconds = remainingSeconds;
            this.previousTail = previousTail;
            this.nextOrder = nextOrder;
            this.deferSuffix = deferSuffix;
            this.deferReason = deferReason;
        }
    }

    private static final class ShiftRollingResult {
        private final List<Cd15RollingPendingTask> carryovers;
        private final int taskCount;

        private ShiftRollingResult(List<Cd15RollingPendingTask> carryovers, int taskCount) {
            this.carryovers = carryovers;
            this.taskCount = taskCount;
        }
    }

    private static final class LaneCommit {
        private final BigDecimal quantity;
        private final List<Cd15StorageLaneAllocation> allocations;
        private final int vehicleCount;
        private final String limitReason;

        private LaneCommit(BigDecimal quantity,
                           List<Cd15StorageLaneAllocation> allocations,
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
        private final Cd15ScheduleResult result;
        private final String originalClassField;
        private BigDecimal quantity;
        private Integer order;
        private boolean locked;
        private boolean hardInsert;
        private boolean carryover;

        private Segment(Cd15ScheduleResult result, String originalClassField,
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

        private static Segment existing(Cd15ScheduleResult result, int classIndex,
                                        BigDecimal quantity, Integer order, boolean locked) {
            boolean hardAdjustment = result.getId() == null
                    && "TRANSFER".equals(result.getSourceType());
            return new Segment(result, "CLASS" + classIndex, quantity, order,
                    locked, hardAdjustment, false);
        }

        private static Segment insert(Cd15ScheduleResult result, int classIndex,
                                      BigDecimal quantity, Integer order) {
            return new Segment(result, "CLASS" + classIndex, quantity, order,
                    false, true, false);
        }

        private static Segment carryover(Cd15ScheduleResult result, int classIndex,
                                         Cd15RollingPendingTask pending) {
            return new Segment(result, pending.getOriginalClassField(),
                    pending.getRemainingQuantity(), pending.getOriginalProduceOrder(),
                    false, pending.isHardInsert(), true);
        }
    }
}
