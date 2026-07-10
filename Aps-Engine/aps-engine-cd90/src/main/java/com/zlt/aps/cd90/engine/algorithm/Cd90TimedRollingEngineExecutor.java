package com.zlt.aps.cd90.engine.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleLaneAllocationMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleResultMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90NewSpecAdvanceResult;
import com.zlt.aps.cd90.engine.model.Cd90RollingAdjustmentDraft;
import com.zlt.aps.cd90.engine.model.Cd90RollingPendingTask;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90TimedRollingOutput;
import com.zlt.aps.cd90.engine.model.Cd90UnscheduledResultModel;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleCandidatePreparationService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleProgressListener;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import com.zlt.aps.cd90.engine.service.impl.Cd90NewSpecAdvanceInputPreparer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/** 定时滚动排程逐班侧车执行器。 */
@Component
@RequiredArgsConstructor
public class Cd90TimedRollingEngineExecutor {

    private final Cd90AutoScheduleEngineService autoScheduleEngineService;
    private final Cd90AutoScheduleInputService inputService;
    private final Cd90EngineScheduleResultMapper scheduleResultMapper;
    private final Cd90EngineScheduleLaneAllocationMapper laneAllocationMapper;
    private final Cd90RollingShiftSlicer shiftSlicer;
    private final Cd90NewSpecAdvanceInputPreparer newSpecAdvanceInputPreparer;
    private final Cd90ShiftDemandProvider demandProvider;
    private final Cd90ScheduleCandidatePreparationService candidatePreparationService;
    private final Cd90RollingStableTaskPlanner stableTaskPlanner;
    private final Cd90RollingScheduleContextManager rollingContextManager;
    private final Cd90ExistingScheduleResourceReserver existingResourceReserver;
    private final Cd90SingleShiftScheduleExecutor singleShiftExecutor;
    private final Cd90UnscheduledResultAggregator unscheduledResultAggregator;

    /** 读取原批次，在内存副本上从目标班开始滚动并输出差异。 */
    public Cd90TimedRollingOutput execute(Cd90RollingTarget target, String inputVersion,
                                          Cd90ScheduleProgressListener listener) {
        validate(target, inputVersion);
        Cd90AutoScheduleContext context = autoScheduleEngineService.prepare(
                target.getFactoryCode(), date(target));
        List<Cd90ShiftDescriptor> affectedShifts = shiftSlicer.slice(
                context.getShifts(), target.getTargetClassField());
        List<Cd90ScheduleResult> sourceResults = loadSourceResults(target);
        if (sourceResults.isEmpty()) {
            throw new IllegalStateException("滚动目标批次不存在排程结果: " + target.getBatchNo());
        }
        Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanes = loadSourceLanes(sourceResults);
        Map<Long, Cd90ScheduleResult> beforeById = sourceResults.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Cd90ScheduleResult::getId, this::copyResult,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, Cd90ScheduleResult> workingById = sourceResults.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Cd90ScheduleResult::getId, this::copyResult,
                        (left, right) -> left, LinkedHashMap::new));
        affectedShifts.forEach(shift -> workingById.values().forEach(
                result -> clearAdjustableFields(result, shift.getClassField())));

        List<Cd90ScheduleAttemptTrace> traces = new ArrayList<>();
        List<Cd90RollingPendingTask> carryOver = new ArrayList<>();
        Cd90RollingScheduleContext rolling = null;
        Cd90ScheduleProgressListener progress = listener == null
                ? Cd90ScheduleProgressListener.NO_OP : listener;
        int shiftCount = affectedShifts.size();
        for (int index = 0; index < shiftCount; index++) {
            Cd90ShiftDescriptor shift = affectedShifts.get(index);
            progress.onProgress(20 + index * 60 / shiftCount, "ROLLING_SHIFT",
                    shiftStageName(shift, "滚动开始"), shift);
            Cd90AutoScheduleInput input = loadInput(context, shift);
            if (rolling == null) {
                Cd90NewSpecAdvanceResult advance = newSpecAdvanceInputPreparer.prepare(context, input);
                input.setPlanningDemandShifts(advance.getAdjustedDemandShifts());
                input.setNewSpecAdvanceInfoByCloth(advance.getAdvanceInfoByCloth());
                rolling = rollingContextManager.initialize(
                        input.getStorageLanesAtSix(), advance.getAdvanceInfoByCloth());
            } else {
                newSpecAdvanceInputPreparer.applySnapshot(
                        input, rolling.getNewSpecAdvanceInfoByCloth());
            }
            rollingContextManager.updateCumulativeConsumption(rolling,
                    demandProvider.cumulativeConsumptionByClothBeforeShift(context, input, shift));
            Cd90ShiftResourceState state = rollingContextManager.openShift(
                    rolling, shift, curlLengthByCloth(input, context),
                    context.getParameters().getRollCoilMeter(),
                    context.getParameters().getRollTotalCount(), Collections.emptyList());
            state.setBigRollAgingStocks(rollingContextManager.restoreBigRollAllocations(
                    rolling, input.getBigRollAgingStocks()));

            List<Cd90ScheduleResult> locked = sourceResults.stream()
                    .filter(item -> readPlan(item, shift.getClassField()).signum() > 0)
                    .filter(item -> isLocked(item, shift.getClassField()))
                    .collect(Collectors.toList());
            existingResourceReserver.reserve(context, shift, state, locked, sourceLanes);
            List<Cd90RollingPendingTask> originals = sourceResults.stream()
                    .filter(item -> readPlan(item, shift.getClassField()).signum() > 0)
                    .filter(item -> !isLocked(item, shift.getClassField()))
                    .map(item -> pending(item, shift)).collect(Collectors.toList());
            List<Cd90ScheduleCandidate> autoCandidates = candidatePreparationService.prepare(
                    context, input, shift.getClassField(), rolling);
            PlanningSet planning = planningSet(shift, carryOver, originals, autoCandidates);
            Map<String, BigDecimal> demandByCloth = demandByCloth(
                    context, input, shift, rolling, planning.candidates);
            mergeCarryOverDemand(demandByCloth, carryOver);
            stableTaskPlanner.allocateRequestedQuantity(planning.orderedTasks,
                    demandByCloth, planning.candidates);
            Cd90ShiftExecutionResult shiftResult = singleShiftExecutor.executePrepared(
                    context, input, shift, state, rolling, planning.candidates,
                    planning.taskByKey, true);
            rollingContextManager.completeShift(rolling, shiftResult.getState());
            appendTraces(traces, shiftResult.getAttemptTraces());
            carryOver = buildCarryOver(planning, shiftResult.getTasks(), shift);
            progress.onProgress(20 + (index + 1) * 60 / shiftCount, "ROLLING_SHIFT",
                    shiftStageName(shift, "滚动完成"), shift);
        }

        ResultDiff diff = buildResultDiff(target, context, affectedShifts,
                beforeById, workingById, rolling == null
                        ? Collections.emptyList() : rolling.getCommittedTasks(), sourceLanes);
        List<Cd90UnscheduleResult> unscheduled = toUnscheduled(target,
                unscheduledResultAggregator.aggregate(traces));
        return Cd90TimedRollingOutput.builder().batchNo(target.getBatchNo())
                .inputVersion(inputVersion).insertedResults(diff.inserted)
                .updatedResults(diff.updated).logicallyDeletedResults(diff.deleted)
                .replacementLaneAllocations(diff.lanes)
                .unscheduledResults(unscheduled).adjustments(diff.adjustments).build();
    }

    private PlanningSet planningSet(Cd90ShiftDescriptor shift,
                                    List<Cd90RollingPendingTask> carryOver,
                                    List<Cd90RollingPendingTask> originals,
                                    List<Cd90ScheduleCandidate> autoCandidates) {
        Map<String, Cd90ScheduleCandidate> autoByCloth = autoCandidates.stream()
                .filter(item -> item.getClothCode() != null)
                .collect(Collectors.toMap(Cd90ScheduleCandidate::getClothCode,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Cd90RollingPendingTask> existing = new ArrayList<>();
        existing.addAll(carryOver);
        existing.addAll(originals);
        List<Cd90RollingPendingTask> newTasks = autoCandidates.stream()
                .filter(item -> existing.stream().noneMatch(
                        task -> Objects.equals(task.getClothCode(), item.getClothCode())))
                .map(item -> Cd90RollingPendingTask.builder()
                        .taskKey("NEW:" + shift.getClassField() + ":" + item.getClothCode())
                        .targetClassField(shift.getClassField()).clothCode(item.getClothCode())
                        .bigRollCode(item.getBigRollCode()).stableOrder(Integer.MAX_VALUE)
                        .urgentCurrentShiftShortage(item.isShortageInCurrentShift()).build())
                .collect(Collectors.toList());
        Map<String, String> newKeyByCloth = newTasks.stream()
                .collect(Collectors.toMap(Cd90RollingPendingTask::getClothCode,
                        Cd90RollingPendingTask::getTaskKey, (left, right) -> left));
        autoCandidates.stream()
                .filter(item -> newKeyByCloth.containsKey(item.getClothCode()))
                .forEach(item -> item.setRollingTaskKey(
                        newKeyByCloth.get(item.getClothCode())));

        List<Cd90RollingPendingTask> ordered = stableTaskPlanner.plan(
                Collections.emptyList(), carryOver, originals, newTasks, autoCandidates);
        Map<String, Cd90ScheduleCandidate> candidatesByKey = new LinkedHashMap<>();
        ordered.forEach(task -> {
            Cd90ScheduleCandidate source = autoByCloth.get(task.getClothCode());
            Cd90ScheduleCandidate candidate = new Cd90ScheduleCandidate();
            if (source != null) {
                BeanUtils.copyProperties(source, candidate);
            }
            candidate.setClothCode(task.getClothCode());
            candidate.setBigRollCode(task.getBigRollCode());
            candidate.setSourceMachineCode(task.getSourceMachineCode());
            candidate.setContinueFromPreviousShift(task.isContinueFromPreviousShift());
            candidate.setRollingTaskKey(task.getTaskKey());
            candidatesByKey.put(task.getTaskKey(), candidate);
        });
        Map<String, Cd90RollingPendingTask> taskByKey = ordered.stream()
                .collect(Collectors.toMap(Cd90RollingPendingTask::getTaskKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return new PlanningSet(ordered, new ArrayList<>(candidatesByKey.values()), taskByKey);
    }

    private String shiftStageName(Cd90ShiftDescriptor shift, String suffix) {
        String displayName = shift == null ? null : shift.getShiftDisplayName();
        String classField = shift == null ? "" : shift.getClassField();
        String shiftName = displayName == null || displayName.trim().isEmpty()
                ? classField : displayName;
        return shiftName + suffix;
    }
    private Map<String, BigDecimal> demandByCloth(Cd90AutoScheduleContext context,
                                                   Cd90AutoScheduleInput input,
                                                   Cd90ShiftDescriptor shift,
                                                   Cd90RollingScheduleContext rolling,
                                                   List<Cd90ScheduleCandidate> candidates) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        candidates.stream().filter(item -> item.getClothCode() != null).forEach(candidate -> {
            Cd90ShiftDemandDecision decision = demandProvider.resolve(
                    context, input, shift, candidate, rolling);
            BigDecimal quantity = decision == null || decision.getNetDemandQuantity() == null
                    ? BigDecimal.ZERO : decision.getNetDemandQuantity();
            result.merge(candidate.getClothCode(), quantity.max(BigDecimal.ZERO), BigDecimal::max);
        });
        return result;
    }

    private void mergeCarryOverDemand(Map<String, BigDecimal> demand,
                                      List<Cd90RollingPendingTask> carryOver) {
        carryOver.stream().filter(item -> item.getClothCode() != null)
                .forEach(item -> demand.merge(item.getClothCode(),
                        value(item.getRemainingQuantity()), BigDecimal::max));
    }

    private List<Cd90RollingPendingTask> buildCarryOver(
            PlanningSet planning, List<Cd90ShiftScheduleTask> tasks,
            Cd90ShiftDescriptor shift) {
        Map<String, BigDecimal> scheduledByKey = tasks.stream()
                .filter(item -> item.getSourceTaskKey() != null)
                .collect(Collectors.groupingBy(Cd90ShiftScheduleTask::getSourceTaskKey,
                        LinkedHashMap::new, Collectors.mapping(
                                Cd90ShiftScheduleTask::getPlanQuantity,
                                Collectors.reducing(BigDecimal.ZERO, this::value, BigDecimal::add))));
        Map<String, Cd90ScheduleCandidate> candidateByKey = planning.candidates.stream()
                .collect(Collectors.toMap(Cd90ScheduleCandidate::getRollingTaskKey,
                        Function.identity(), (left, right) -> left));
        return planning.orderedTasks.stream().map(task -> {
                    BigDecimal requested = value(candidateByKey.get(task.getTaskKey())
                            .getRollingRequestedQuantity());
                    BigDecimal remaining = requested.subtract(scheduledByKey.getOrDefault(
                            task.getTaskKey(), BigDecimal.ZERO)).max(BigDecimal.ZERO);
                    if (remaining.signum() <= 0) {
                        return null;
                    }
                    task.setOriginalClassField(shift.getClassField());
                    task.setContinueFromPreviousShift(true);
                    task.setRemainingQuantity(remaining);
                    task.setStableOrder(0);
                    return task;
                }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ResultDiff buildResultDiff(Cd90RollingTarget target,
                                       Cd90AutoScheduleContext context,
                                       List<Cd90ShiftDescriptor> affectedShifts,
                                       Map<Long, Cd90ScheduleResult> beforeById,
                                       Map<Long, Cd90ScheduleResult> workingById,
                                       List<Cd90ShiftScheduleTask> committedTasks,
                                       Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanes) {
        Map<String, Cd90ScheduleResult> insertedByKey = new LinkedHashMap<>();
        List<Cd90ScheduleLaneAllocation> lanes = new ArrayList<>();
        committedTasks.stream().filter(task -> affectedShifts.stream().anyMatch(
                shift -> Objects.equals(shift.getClassField(), task.getClassField())))
                .forEach(task -> applyTask(target, workingById, insertedByKey, lanes, task));
        List<Cd90ScheduleResult> updated = workingById.values().stream()
                .filter(item -> !affectedFingerprint(item, affectedShifts).equals(
                        affectedFingerprint(beforeById.get(item.getId()), affectedShifts)))
                .collect(Collectors.toList());
        List<Cd90ScheduleResult> deleted = updated.stream()
                .filter(item -> allPlansZero(item, context.getShifts()))
                .collect(Collectors.toList());
        List<Cd90ScheduleResult> inserted = new ArrayList<>(insertedByKey.values());
        List<Cd90RollingAdjustmentDraft> adjustments = updated.stream()
                .map(after -> adjustment(beforeById.get(after.getId()), after,
                        affectedShifts, sourceLanes.getOrDefault(after.getId(), Collections.emptyList()),
                        lanesForOrder(lanes, after.getOrderNo())))
                .collect(Collectors.toCollection(ArrayList::new));
        inserted.stream().map(after -> insertedAdjustment(after, affectedShifts,
                        lanesForOrder(lanes, after.getOrderNo())))
                .forEach(adjustments::add);
        return new ResultDiff(inserted, updated, deleted, lanes, adjustments);
    }

    private void applyTask(Cd90RollingTarget target,
                           Map<Long, Cd90ScheduleResult> workingById,
                           Map<String, Cd90ScheduleResult> insertedByKey,
                           List<Cd90ScheduleLaneAllocation> lanes,
                           Cd90ShiftScheduleTask task) {
        Cd90ScheduleResult result = task.getSourceResultId() == null
                ? null : workingById.get(task.getSourceResultId());
        if (result == null || (!Objects.equals(result.getMachineCode(), task.getMachineCode())
                && hasPlanOutside(result, task.getClassField()))) {
            String key = task.getSourceTaskKey() + ":" + task.getMachineCode();
            result = insertedByKey.computeIfAbsent(key,
                    ignored -> newResult(target, task, key));
        }
        result.setMachineCode(task.getMachineCode());
        writeTask(result, task);
        String orderNo = result.getOrderNo();
        for (int index = 0; index < task.getLaneAllocations().size(); index++) {
            Cd90StorageLaneAllocation source = task.getLaneAllocations().get(index);
            Cd90ScheduleLaneAllocation lane = new Cd90ScheduleLaneAllocation();
            lane.setFactoryCode(target.getFactoryCode());
            lane.setScheduleDate(date(target));
            lane.setBatchNo(target.getBatchNo());
            lane.setScheduleResultId(result.getId());
            lane.setOrderNo(orderNo);
            lane.setClassField(task.getClassField());
            lane.setShiftScheduleDate(Date.from(task.getExpectedStartTime()
                    .atZone(ZoneId.systemDefault()).toInstant()));
            lane.setStorageLaneCode(source.getLaneCode());
            lane.setClothCode(task.getClothCode());
            lane.setAllocatedQty(task.getPlanQuantity().doubleValue()
                    / Math.max(1, task.getLaneAllocations().size()));
            lane.setAllocatedCartCount(source.getVehicleCount());
            lane.setAllocationOrder(index + 1);
            lanes.add(lane);
        }
    }

    private Cd90ScheduleResult newResult(Cd90RollingTarget target,
                                         Cd90ShiftScheduleTask task, String key) {
        Cd90ScheduleResult result = new Cd90ScheduleResult();
        result.setFactoryCode(target.getFactoryCode());
        result.setScheduleDate(date(target));
        result.setBatchNo(target.getBatchNo());
        result.setOrderNo("ROLLING-" + Integer.toHexString(key.hashCode()).toUpperCase());
        result.setClothCode(task.getClothCode());
        result.setBigRollCode(task.getBigRollCode());
        result.setMachineCode(task.getMachineCode());
        result.setDataSource("0");
        return result;
    }

    private void writeTask(Cd90ScheduleResult result, Cd90ShiftScheduleTask task) {
        BeanWrapper wrapper = wrapper(result);
        wrapper.setPropertyValue(property(task.getClassField(), "PlanQty"),
                task.getPlanQuantity().doubleValue());
        wrapper.setPropertyValue(property(task.getClassField(), "ProduceOrder"),
                task.getProduceOrder());
        wrapper.setPropertyValue(property(task.getClassField(), "Analysis"),
                "TIMED_ROLLING");
    }

    private void clearAdjustableFields(Cd90ScheduleResult result, String classField) {
        if (isLocked(result, classField)) {
            return;
        }
        BeanWrapper wrapper = wrapper(result);
        wrapper.setPropertyValue(property(classField, "PlanQty"), 0D);
        wrapper.setPropertyValue(property(classField, "ProduceOrder"), null);
        wrapper.setPropertyValue(property(classField, "Analysis"), null);
    }

    private Cd90RollingPendingTask pending(Cd90ScheduleResult result,
                                           Cd90ShiftDescriptor shift) {
        BigDecimal quantity = readPlan(result, shift.getClassField());
        return Cd90RollingPendingTask.builder()
                .taskKey(result.getId() + ":" + shift.getClassField())
                .sourceResultId(result.getId()).sourceBatchNo(result.getBatchNo())
                .sourceOrderNo(result.getOrderNo()).originalClassField(shift.getClassField())
                .originalProduceOrder(readOrder(result, shift.getClassField()))
                .targetClassField(shift.getClassField()).clothCode(result.getClothCode())
                .bigRollCode(result.getBigRollCode()).sourceMachineCode(result.getMachineCode())
                .originalQuantity(quantity).remainingQuantity(quantity)
                .stableOrder(defaultOrder(readOrder(result, shift.getClassField()))).build();
    }

    private List<Cd90UnscheduleResult> toUnscheduled(
            Cd90RollingTarget target, List<Cd90UnscheduledResultModel> models) {
        return models.stream().map(model -> {
            Cd90UnscheduleResult result = new Cd90UnscheduleResult();
            result.setFactoryCode(target.getFactoryCode());
            result.setScheduleDate(date(target));
            result.setBatchNo(target.getBatchNo());
            result.setClothCode(model.getClothCode());
            result.setBigRollCode(model.getBigRollCode());
            result.setDemandQty(value(model.getDemandQuantity()).doubleValue());
            result.setScheduledQty(value(model.getScheduledQuantity()).doubleValue());
            result.setUnscheduledQty(value(model.getUnscheduledQuantity()).doubleValue());
            result.setFailStage(model.getFailStage());
            result.setReasonCode(model.getReasonCode());
            result.setReasonOrder(model.getReasonOrder());
            result.setPrimaryReason(model.isPrimaryReason() ? "1" : "0");
            result.setUnscheduledReason(model.getReasonDescription());
            result.setCandidateMachineCodes(model.getCandidateMachineCodes());
            result.setDataSource("0");
            result.setProcessedTime(new Date());
            return result;
        }).collect(Collectors.toList());
    }

    private Cd90RollingAdjustmentDraft adjustment(
            Cd90ScheduleResult before, Cd90ScheduleResult after,
            List<Cd90ShiftDescriptor> shifts,
            List<Cd90ScheduleLaneAllocation> beforeLanes,
            List<Cd90ScheduleLaneAllocation> afterLanes) {
        String reason = shifts.stream().allMatch(
                shift -> readPlan(after, shift.getClassField()).signum() <= 0)
                ? "ZERO_DEMAND_REMOVE" : "ROLLING_REPLAN";
        String beforeClass = firstPlannedClass(before, shifts);
        String afterClass = firstPlannedClass(after, shifts);
        return Cd90RollingAdjustmentDraft.builder()
                .rollingItemKey(after.getId() + ":" + after.getClothCode())
                .scheduleResultId(after.getId()).clothCode(after.getClothCode())
                .bigRollCode(after.getBigRollCode()).adjustType(reason)
                .beforeClassField(beforeClass)
                .beforeProduceOrder(readOrderOrNull(before, beforeClass))
                .beforeQuantity(readPlanOrZero(before, beforeClass))
                .beforeMachineCode(before.getMachineCode())
                .afterClassField(afterClass)
                .afterProduceOrder(readOrderOrNull(after, afterClass))
                .afterQuantity(readPlanOrZero(after, afterClass))
                .afterMachineCode(after.getMachineCode())
                .reasonCode(reason).reasonDetail("定时滚动按最新需求和资源状态重排")
                .beforeSnapshot(snapshot(before, beforeLanes, shifts))
                .afterSnapshot(snapshot(after, afterLanes, shifts)).build();
    }

    private Cd90RollingAdjustmentDraft insertedAdjustment(
            Cd90ScheduleResult after, List<Cd90ShiftDescriptor> shifts,
            List<Cd90ScheduleLaneAllocation> afterLanes) {
        String afterClass = firstPlannedClass(after, shifts);
        return Cd90RollingAdjustmentDraft.builder()
                .rollingItemKey(after.getOrderNo()).scheduleResultId(null)
                .clothCode(after.getClothCode()).bigRollCode(after.getBigRollCode())
                .adjustType("ROLLING_INSERT").afterClassField(afterClass)
                .afterProduceOrder(readOrderOrNull(after, afterClass))
                .afterQuantity(readPlanOrZero(after, afterClass))
                .afterMachineCode(after.getMachineCode())
                .reasonCode("ROLLING_INSERT")
                .reasonDetail("最新需求产生新规格或新机台排程")
                .beforeSnapshot(snapshot(null, Collections.emptyList(), shifts))
                .afterSnapshot(snapshot(after, afterLanes, shifts)).build();
    }

    private List<Cd90ScheduleLaneAllocation> lanesForOrder(
            List<Cd90ScheduleLaneAllocation> lanes, String orderNo) {
        return lanes.stream().filter(item -> Objects.equals(orderNo, item.getOrderNo()))
                .collect(Collectors.toList());
    }

    private String firstPlannedClass(Cd90ScheduleResult result,
                                     List<Cd90ShiftDescriptor> shifts) {
        if (result == null) {
            return null;
        }
        return shifts.stream().map(Cd90ShiftDescriptor::getClassField)
                .filter(classField -> readPlan(result, classField).signum() > 0)
                .findFirst().orElse(null);
    }

    private Integer readOrderOrNull(Cd90ScheduleResult result, String classField) {
        return result == null || classField == null ? null : readOrder(result, classField);
    }

    private BigDecimal readPlanOrZero(Cd90ScheduleResult result, String classField) {
        return result == null || classField == null
                ? BigDecimal.ZERO : readPlan(result, classField);
    }

    private Map<String, Object> snapshot(Cd90ScheduleResult result,
                                         List<Cd90ScheduleLaneAllocation> lanes,
                                         List<Cd90ShiftDescriptor> shifts) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("scheduleResult", result == null ? null : copyResult(result));
        snapshot.put("laneAllocations", new ArrayList<>(lanes));
        snapshot.put("affectedClasses", shifts.stream()
                .map(Cd90ShiftDescriptor::getClassField).collect(Collectors.toList()));
        snapshot.put("publishState", result.getIsRelease());
        return snapshot;
    }

    private Map<Long, List<Cd90ScheduleLaneAllocation>> loadSourceLanes(
            List<Cd90ScheduleResult> sourceResults) {
        List<Long> ids = sourceResults.stream().map(Cd90ScheduleResult::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return laneAllocationMapper.selectList(
                        new LambdaQueryWrapper<Cd90ScheduleLaneAllocation>()
                                .in(Cd90ScheduleLaneAllocation::getScheduleResultId, ids))
                .stream().collect(Collectors.groupingBy(
                        Cd90ScheduleLaneAllocation::getScheduleResultId));
    }

    private List<Cd90ScheduleResult> loadSourceResults(Cd90RollingTarget target) {
        return scheduleResultMapper.selectList(new LambdaQueryWrapper<Cd90ScheduleResult>()
                .eq(Cd90ScheduleResult::getFactoryCode, target.getFactoryCode())
                .eq(Cd90ScheduleResult::getScheduleDate, date(target))
                .eq(Cd90ScheduleResult::getBatchNo, target.getBatchNo()));
    }

    private Cd90AutoScheduleInput loadInput(Cd90AutoScheduleContext context,
                                            Cd90ShiftDescriptor shift) {
        return inputService.load(context.getFactoryCode(), context.getScheduleDate(),
                shift.getClassField(), shift.getShiftCode(),
                context.getParameters().getAgingPeriodHours());
    }

    private Map<String, BigDecimal> curlLengthByCloth(
            Cd90AutoScheduleInput input, Cd90AutoScheduleContext context) {
        return input.getConstructionMaterials().stream()
                .filter(Objects::nonNull).filter(item -> item.getClothCode() != null)
                .collect(Collectors.toMap(Cd90ConstructionMaterial::getClothCode,
                        item -> item.getCurlLength() == null || item.getCurlLength().signum() <= 0
                                ? context.getParameters().getRollCoilMeter() : item.getCurlLength(),
                        (left, right) -> left, HashMap::new));
    }

    private void appendTraces(List<Cd90ScheduleAttemptTrace> target,
                              List<Cd90ScheduleAttemptTrace> source) {
        if (source == null) {
            return;
        }
        source.forEach(trace -> {
            trace.setSequence(target.size() + 1);
            target.add(trace);
        });
    }

    private boolean isLocked(Cd90ScheduleResult result, String classField) {
        BigDecimal finish = readNumber(result, property(classField, "FinishQty"));
        return Integer.valueOf(1).equals(result.getIsLocked()) || finish.signum() > 0
                || ("1".equals(result.getProductionStatus())
                && readPlan(result, classField).signum() > 0);
    }

    private boolean hasPlanOutside(Cd90ScheduleResult result, String excludedClass) {
        for (int index = 1; index <= 8; index++) {
            String classField = "CLASS" + index;
            if (!classField.equals(excludedClass) && readPlan(result, classField).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean allPlansZero(Cd90ScheduleResult result, List<Cd90ShiftDescriptor> shifts) {
        return shifts.stream().allMatch(
                shift -> readPlan(result, shift.getClassField()).signum() <= 0);
    }

    private String affectedFingerprint(Cd90ScheduleResult result,
                                       List<Cd90ShiftDescriptor> shifts) {
        if (result == null) {
            return "";
        }
        return shifts.stream().map(shift -> shift.getClassField() + ":"
                        + readPlan(result, shift.getClassField()) + ":"
                        + readOrder(result, shift.getClassField()))
                .collect(Collectors.joining("|")) + ":" + result.getMachineCode();
    }

    private BigDecimal readPlan(Cd90ScheduleResult result, String classField) {
        return readNumber(result, property(classField, "PlanQty"));
    }

    private Integer readOrder(Cd90ScheduleResult result, String classField) {
        Object value = wrapper(result).getPropertyValue(property(classField, "ProduceOrder"));
        return value == null ? null : ((Number) value).intValue();
    }

    private BigDecimal readNumber(Cd90ScheduleResult result, String property) {
        Object value = wrapper(result).getPropertyValue(property);
        return value == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private BeanWrapper wrapper(Cd90ScheduleResult result) {
        return PropertyAccessorFactory.forBeanPropertyAccess(result);
    }

    private String property(String classField, String suffix) {
        return classField.toLowerCase() + suffix;
    }

    private int defaultOrder(Integer value) {
        return value == null || value <= 0 ? Integer.MAX_VALUE : value;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Cd90ScheduleResult copyResult(Cd90ScheduleResult source) {
        Cd90ScheduleResult target = new Cd90ScheduleResult();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private Date date(Cd90RollingTarget target) {
        return Date.from(target.getScheduleDate().atStartOfDay(
                ZoneId.systemDefault()).toInstant());
    }

    private void validate(Cd90RollingTarget target, String inputVersion) {
        if (target == null || target.getScheduleDate() == null
                || target.getFactoryCode() == null || target.getBatchNo() == null
                || target.getTargetClassField() == null || inputVersion == null) {
            throw new IllegalArgumentException("定时滚动排程目标和输入版本不能为空");
        }
    }

    @RequiredArgsConstructor
    private static class PlanningSet {
        private final List<Cd90RollingPendingTask> orderedTasks;
        private final List<Cd90ScheduleCandidate> candidates;
        private final Map<String, Cd90RollingPendingTask> taskByKey;
    }

    @RequiredArgsConstructor
    private static class ResultDiff {
        private final List<Cd90ScheduleResult> inserted;
        private final List<Cd90ScheduleResult> updated;
        private final List<Cd90ScheduleResult> deleted;
        private final List<Cd90ScheduleLaneAllocation> lanes;
        private final List<Cd90RollingAdjustmentDraft> adjustments;
    }
}
