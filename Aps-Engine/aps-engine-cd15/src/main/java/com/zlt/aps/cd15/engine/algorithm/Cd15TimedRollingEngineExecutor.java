package com.zlt.aps.cd15.engine.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingAdjustmentDraft;
import com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.model.Cd15UnscheduledResultModel;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleEngineService;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleCandidatePreparationService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import com.zlt.aps.cd15.engine.service.Cd15RollingShiftStockService;
import com.zlt.aps.cd15.engine.service.impl.Cd15NewSpecAdvanceInputPreparer;
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
public class Cd15TimedRollingEngineExecutor {

    private final Cd15AutoScheduleEngineService autoScheduleEngineService;
    private final Cd15AutoScheduleInputService inputService;
    private final Cd15EngineScheduleResultMapper scheduleResultMapper;
    private final Cd15EngineScheduleLaneAllocationMapper laneAllocationMapper;
    private final Cd15RollingShiftSlicer shiftSlicer;
    private final Cd15NewSpecAdvanceInputPreparer newSpecAdvanceInputPreparer;
    private final Cd15ShiftDemandProvider demandProvider;
    private final Cd15ScheduleCandidatePreparationService candidatePreparationService;
    private final Cd15RollingStableTaskPlanner stableTaskPlanner;
    private final Cd15RollingScheduleContextManager rollingContextManager;
    private final Cd15ExistingScheduleResourceReserver existingResourceReserver;
    private final Cd15SingleShiftScheduleExecutor singleShiftExecutor;
    private final Cd15UnscheduledResultAggregator unscheduledResultAggregator;
    private final Cd15BigRollMeterCalculator bigRollMeterCalculator;
    private final Cd15RollingShiftStockService rollingShiftStockService;

    /** 读取原批次，在内存副本上从目标班开始滚动并输出差异。 */
    public Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion,
                                          Cd15ScheduleProgressListener listener) {
        validate(target, inputVersion);
        Cd15AutoScheduleContext context = autoScheduleEngineService.prepare(
                target.getFactoryCode(), date(target));
        context.setResourceBaselineDate(target.getResourceBaselineDate());
        context.setResourceBaselineShiftCode(target.getTargetShiftCode());
        List<Cd15ShiftDescriptor> affectedShifts = shiftSlicer.slice(
                context.getShifts(), target.getTargetClassField());
        List<Cd15ScheduleResult> sourceResults = loadSourceResults(target);
        if (sourceResults.isEmpty()) {
            throw new IllegalStateException("滚动目标批次不存在排程结果: " + target.getBatchNo());
        }
        Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanes = loadSourceLanes(sourceResults);
        Map<Long, Cd15ScheduleResult> beforeById = sourceResults.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Cd15ScheduleResult::getId, this::copyResult,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, Cd15ScheduleResult> workingById = sourceResults.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(Cd15ScheduleResult::getId, this::copyResult,
                        (left, right) -> left, LinkedHashMap::new));
        affectedShifts.forEach(shift -> workingById.values().forEach(
                result -> clearAdjustableFields(result, shift.getClassField())));

        List<Cd15ScheduleAttemptTrace> traces = new ArrayList<>();
        List<Cd15RollingPendingTask> carryOver = new ArrayList<>();
        List<Cd15ConstructionMaterial> constructionMaterials = new ArrayList<>();
        List<Cd15StockSource> targetShiftStocks =
                rollingShiftStockService.loadRequired(target);
        Cd15RollingScheduleContext rolling = null;
        Cd15ScheduleProgressListener progress = listener == null
                ? Cd15ScheduleProgressListener.NO_OP : listener;
        int shiftCount = affectedShifts.size();
        for (int index = 0; index < shiftCount; index++) {
            Cd15ShiftDescriptor shift = affectedShifts.get(index);
            progress.onProgress(20 + index * 60 / shiftCount, "ROLLING_SHIFT",
                    shiftStageName(shift, "滚动开始"), shift);
            Cd15AutoScheduleInput input = loadInput(context, shift);
            // 定时滚动的全部后续班次统一从目标班次开始库存重新累计。
            input.setStocksAtSix(targetShiftStocks);
            if (input.getConstructionMaterials() != null) {
                constructionMaterials.addAll(input.getConstructionMaterials());
            }
            if (rolling == null) {
                Cd15NewSpecAdvanceResult advance = newSpecAdvanceInputPreparer.prepare(context, input);
                input.setPlanningDemandShifts(advance.getAdjustedDemandShifts());
                input.setNewSpecAdvanceInfoBySteelStrip(advance.getAdvanceInfoBySteelStrip());
                rolling = rollingContextManager.initialize(
                        input.getStorageLanesAtSix(), advance.getAdvanceInfoBySteelStrip());
            } else {
                newSpecAdvanceInputPreparer.applySnapshot(
                        input, rolling.getNewSpecAdvanceInfoBySteelStrip());
            }
            rollingContextManager.updateCumulativeConsumption(rolling,
                    demandProvider.cumulativeConsumptionBySteelStripBeforeShift(context, input, shift));
            Cd15ShiftResourceState state = rollingContextManager.openShift(
                    rolling, shift, curlLengthBySteelStrip(input, context),
                    context.getParameters().getRollCoilMeter(),
                    context.getParameters().getRollTotalCount(), Collections.emptyList());
            state.setBigRollAgingStocks(rollingContextManager.restoreBigRollAllocations(
                    rolling, input.getBigRollAgingStocks()));

            List<Cd15ScheduleResult> locked = sourceResults.stream()
                    .filter(item -> readPlan(item, shift.getClassField()).signum() > 0)
                    .filter(item -> isLocked(item, shift.getClassField()))
                    .collect(Collectors.toList());
            existingResourceReserver.reserve(context, shift, state, locked, sourceLanes, input);
            List<Cd15RollingPendingTask> originals = sourceResults.stream()
                    .filter(item -> readPlan(item, shift.getClassField()).signum() > 0)
                    .filter(item -> !isLocked(item, shift.getClassField()))
                    .map(item -> pending(item, shift)).collect(Collectors.toList());
            List<Cd15ScheduleCandidate> autoCandidates = candidatePreparationService.prepare(
                    context, input, shift.getClassField(), rolling);
            PlanningSet planning = planningSet(shift, carryOver, originals, autoCandidates);
            Map<String, BigDecimal> demandBySteelStrip = demandBySteelStrip(
                    context, input, shift, rolling, planning.candidates);
            mergeCarryOverDemand(demandBySteelStrip, carryOver);
            stableTaskPlanner.allocateRequestedQuantity(planning.orderedTasks,
                    demandBySteelStrip, planning.candidates);
            Cd15ShiftExecutionResult shiftResult = singleShiftExecutor.executePrepared(
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
                        ? Collections.emptyList() : rolling.getCommittedTasks(),
                sourceLanes, constructionMaterials);
        List<Cd15UnscheduleResult> unscheduled = toUnscheduled(target,
                unscheduledResultAggregator.aggregate(traces));
        return Cd15TimedRollingOutput.builder().batchNo(target.getBatchNo())
                .inputVersion(inputVersion).insertedResults(diff.inserted)
                .updatedResults(diff.updated).logicallyDeletedResults(diff.deleted)
                .replacementLaneAllocations(diff.lanes)
                .unscheduledResults(unscheduled).adjustments(diff.adjustments).build();
    }

    private PlanningSet planningSet(Cd15ShiftDescriptor shift,
                                    List<Cd15RollingPendingTask> carryOver,
                                    List<Cd15RollingPendingTask> originals,
                                    List<Cd15ScheduleCandidate> autoCandidates) {
        List<Cd15RollingPendingTask> existing = new ArrayList<>();
        existing.addAll(carryOver);
        existing.addAll(originals);
        List<Cd15RollingPendingTask> newTasks = autoCandidates.stream()
                .filter(item -> existing.stream().noneMatch(
                        task -> this.sameMaterial(task, item)))
                .map(item -> Cd15RollingPendingTask.builder()
                        .taskKey("NEW:" + shift.getClassField() + ":" + item.getMaterialKey())
                        .targetClassField(shift.getClassField())
                        .materialKey(item.getMaterialKey())
                        .steelStripCode(item.getSteelStripCode())
                        .bigRollCode(item.getBigRollCode())
                        .cuttingAngle(item.getCuttingAngle())
                        .craftWidth(item.getCraftWidth())
                        .unitConsumeMillimeter(item.getUnitConsumeMillimeter())
                        .cordWidth(item.getCordWidth())
                        .curlLength(item.getCurlLength())
                        .stableOrder(Integer.MAX_VALUE)
                        .urgentCurrentShiftShortage(item.isShortageInCurrentShift()).build())
                .collect(Collectors.toList());
        Map<String, String> newKeyByMaterial = newTasks.stream()
                .collect(Collectors.toMap(Cd15RollingPendingTask::getMaterialKey,
                        Cd15RollingPendingTask::getTaskKey, (left, right) -> left));
        autoCandidates.stream()
                .filter(item -> newKeyByMaterial.containsKey(item.getMaterialKey()))
                .forEach(item -> item.setRollingTaskKey(
                        newKeyByMaterial.get(item.getMaterialKey())));

        List<Cd15RollingPendingTask> ordered = stableTaskPlanner.plan(
                Collections.emptyList(), carryOver, originals, newTasks, autoCandidates);
        Map<String, Cd15ScheduleCandidate> candidatesByKey = new LinkedHashMap<>();
        ordered.forEach(task -> {
            Cd15ScheduleCandidate source = this.findAutoCandidate(task, autoCandidates);
            Cd15ScheduleCandidate candidate = new Cd15ScheduleCandidate();
            if (source != null) {
                BeanUtils.copyProperties(source, candidate);
                task.setMaterialKey(source.getMaterialKey());
                task.setCuttingAngle(source.getCuttingAngle());
                task.setCraftWidth(source.getCraftWidth());
                task.setUnitConsumeMillimeter(source.getUnitConsumeMillimeter());
                task.setCordWidth(source.getCordWidth());
                task.setCurlLength(source.getCurlLength());
            }
            candidate.setMaterialKey(source == null
                    ? task.getMaterialKey() : source.getMaterialKey());
            candidate.setSteelStripCode(task.getSteelStripCode());
            candidate.setBigRollCode(task.getBigRollCode());
            candidate.setCuttingAngle(source == null
                    ? task.getCuttingAngle() : source.getCuttingAngle());
            candidate.setCraftWidth(source == null
                    ? task.getCraftWidth() : source.getCraftWidth());
            candidate.setUnitConsumeMillimeter(source == null
                    ? task.getUnitConsumeMillimeter() : source.getUnitConsumeMillimeter());
            candidate.setCordWidth(source == null
                    ? task.getCordWidth() : source.getCordWidth());
            candidate.setCurlLength(source == null
                    ? task.getCurlLength() : source.getCurlLength());
            candidate.setCutMode(task.getCutMode());
            candidate.setSplitGroupKey(task.getSplitGroupKey());
            candidate.setSourceMachineCode(task.getSourceMachineCode());
            candidate.setContinueFromPreviousShift(task.isContinueFromPreviousShift());
            candidate.setRollingTaskKey(task.getTaskKey());
            candidatesByKey.put(task.getTaskKey(), candidate);
        });
        Map<String, Cd15RollingPendingTask> taskByKey = ordered.stream()
                .collect(Collectors.toMap(Cd15RollingPendingTask::getTaskKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return new PlanningSet(ordered, new ArrayList<>(candidatesByKey.values()), taskByKey);
    }

    private Cd15ScheduleCandidate findAutoCandidate(
            Cd15RollingPendingTask task,
            List<Cd15ScheduleCandidate> autoCandidates) {
        return autoCandidates.stream()
                .filter(item -> this.sameMaterial(task, item))
                .findFirst().orElse(null);
    }

    private boolean sameMaterial(Cd15RollingPendingTask task,
                                 Cd15ScheduleCandidate candidate) {
        if (task == null || candidate == null) {
            return false;
        }
        if (task.getMaterialKey() != null
                && task.getMaterialKey().equals(candidate.getMaterialKey())) {
            return true;
        }
        return Objects.equals(task.getSteelStripCode(), candidate.getSteelStripCode())
                && Objects.equals(task.getBigRollCode(), candidate.getBigRollCode())
                && Objects.equals(task.getCuttingAngle(), candidate.getCuttingAngle());
    }

    private String shiftStageName(Cd15ShiftDescriptor shift, String suffix) {
        String displayName = shift == null ? null : shift.getShiftDisplayName();
        String classField = shift == null ? "" : shift.getClassField();
        String shiftName = displayName == null || displayName.trim().isEmpty()
                ? classField : displayName;
        return shiftName + suffix;
    }
    private Map<String, BigDecimal> demandBySteelStrip(Cd15AutoScheduleContext context,
                                                   Cd15AutoScheduleInput input,
                                                   Cd15ShiftDescriptor shift,
                                                   Cd15RollingScheduleContext rolling,
                                                   List<Cd15ScheduleCandidate> candidates) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        candidates.stream().filter(item -> item.getSteelStripCode() != null).forEach(candidate -> {
            Cd15ShiftDemandDecision decision = demandProvider.resolve(
                    context, input, shift, candidate, rolling);
            BigDecimal quantity = decision == null || decision.getNetDemandQuantity() == null
                    ? BigDecimal.ZERO : decision.getNetDemandQuantity();
            result.merge(candidate.getMaterialKey(), quantity.max(BigDecimal.ZERO), BigDecimal::max);
        });
        return result;
    }

    private void mergeCarryOverDemand(Map<String, BigDecimal> demand,
                                      List<Cd15RollingPendingTask> carryOver) {
        carryOver.stream().filter(item -> item.getMaterialKey() != null)
                .forEach(item -> demand.merge(item.getMaterialKey(),
                        value(item.getRemainingQuantity()), BigDecimal::max));
    }

    private List<Cd15RollingPendingTask> buildCarryOver(
            PlanningSet planning, List<Cd15ShiftScheduleTask> tasks,
            Cd15ShiftDescriptor shift) {
        Map<String, BigDecimal> scheduledByKey = tasks.stream()
                .filter(item -> item.getSourceTaskKey() != null)
                .collect(Collectors.groupingBy(Cd15ShiftScheduleTask::getSourceTaskKey,
                        LinkedHashMap::new, Collectors.mapping(
                                Cd15ShiftScheduleTask::getPlanQuantity,
                                Collectors.reducing(BigDecimal.ZERO, this::value, BigDecimal::add))));
        Map<String, Cd15ScheduleCandidate> candidateByKey = planning.candidates.stream()
                .collect(Collectors.toMap(Cd15ScheduleCandidate::getRollingTaskKey,
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

    private ResultDiff buildResultDiff(Cd15RollingTarget target,
                                       Cd15AutoScheduleContext context,
                                       List<Cd15ShiftDescriptor> affectedShifts,
                                       Map<Long, Cd15ScheduleResult> beforeById,
                                       Map<Long, Cd15ScheduleResult> workingById,
                                       List<Cd15ShiftScheduleTask> committedTasks,
                                       Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanes,
                                       List<Cd15ConstructionMaterial> constructionMaterials) {
        Map<String, Cd15ScheduleResult> insertedByKey = new LinkedHashMap<>();
        List<Cd15ScheduleLaneAllocation> lanes = new ArrayList<>();
        committedTasks.stream().filter(task -> affectedShifts.stream().anyMatch(
                shift -> Objects.equals(shift.getClassField(), task.getClassField())))
                .forEach(task -> applyTask(target, workingById, insertedByKey, lanes, task));
        workingById.values().forEach(
                result -> this.fillMissingCordWidth(result, constructionMaterials));
        insertedByKey.values().forEach(
                result -> this.fillMissingCordWidth(result, constructionMaterials));
        workingById.values().forEach(this::recalculateBigRollConsumption);
        insertedByKey.values().forEach(this::recalculateBigRollConsumption);
        List<Cd15ScheduleResult> updated = workingById.values().stream()
                .filter(item -> !Objects.equals(item.getCordWidth(),
                                beforeById.get(item.getId()).getCordWidth())
                        || !affectedFingerprint(item, affectedShifts).equals(
                                affectedFingerprint(beforeById.get(item.getId()), affectedShifts)))
                .collect(Collectors.toList());
        List<Cd15ScheduleResult> deleted = updated.stream()
                .filter(item -> allPlansZero(item, context.getShifts()))
                .collect(Collectors.toList());
        List<Cd15ScheduleResult> inserted = new ArrayList<>(insertedByKey.values());
        List<Cd15RollingAdjustmentDraft> adjustments = updated.stream()
                .map(after -> adjustment(beforeById.get(after.getId()), after,
                        affectedShifts, sourceLanes.getOrDefault(after.getId(), Collections.emptyList()),
                        lanesForResult(lanes, after.getOrderNo(), after.getSteelStripCode())))
                .collect(Collectors.toCollection(ArrayList::new));
        inserted.stream().map(after -> insertedAdjustment(after, affectedShifts,
                        lanesForResult(lanes, after.getOrderNo(), after.getSteelStripCode())))
                .forEach(adjustments::add);
        return new ResultDiff(inserted, updated, deleted, lanes, adjustments);
    }

    private void applyTask(Cd15RollingTarget target,
                           Map<Long, Cd15ScheduleResult> workingById,
                           Map<String, Cd15ScheduleResult> insertedByKey,
                           List<Cd15ScheduleLaneAllocation> lanes,
                           Cd15ShiftScheduleTask task) {
        Cd15ScheduleResult result = task.getSourceResultId() == null
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
            Cd15StorageLaneAllocation source = task.getLaneAllocations().get(index);
            Cd15ScheduleLaneAllocation lane = new Cd15ScheduleLaneAllocation();
            lane.setFactoryCode(target.getFactoryCode());
            lane.setScheduleDate(date(target));
            lane.setBatchNo(target.getBatchNo());
            lane.setScheduleResultId(result.getId());
            lane.setOrderNo(orderNo);
            lane.setClassField(task.getClassField());
            lane.setShiftScheduleDate(Date.from(task.getExpectedStartTime()
                    .atZone(ZoneId.systemDefault()).toInstant()));
            lane.setStorageLaneCode(source.getLaneCode());
            lane.setSteelStripCode(task.getSteelStripCode());
            lane.setBigRollCode(task.getBigRollCode());
            lane.setCuttingAngle(task.getCuttingAngle());
            lane.setMachineCode(task.getMachineCode());
            lane.setGroupNo(result.getGroupNo());
            lane.setAllocatedQty(task.getPlanQuantity()
                    .divide(BigDecimal.valueOf(Math.max(1, task.getLaneAllocations().size())),
                            4, java.math.RoundingMode.HALF_UP));
            lane.setAllocatedCartCount(source.getVehicleCount());
            lane.setAllocationOrder(index + 1);
            lanes.add(lane);
        }
    }

    private Cd15ScheduleResult newResult(Cd15RollingTarget target,
                                         Cd15ShiftScheduleTask task, String key) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setFactoryCode(target.getFactoryCode());
        result.setScheduleDate(date(target));
        result.setCd15BatchNo(target.getBatchNo());
        // SPLIT 切胶模式下按 (splitGroupKey|machineCode) 生成滚动单号，其余模式按 key 生成
        String rollingOrderNo = "SPLIT".equals(task.getCutMode())
                && task.getSplitGroupKey() != null
                ? "ROLLING-" + Integer.toHexString(
                        (task.getSplitGroupKey() + "|" + task.getMachineCode()).hashCode())
                        .toUpperCase()
                : "ROLLING-" + Integer.toHexString(key.hashCode()).toUpperCase();
        result.setOrderNo(rollingOrderNo);
        // SPLIT 模式下 groupNo 与滚动单号一致，用于批次分组
        result.setGroupNo("SPLIT".equals(task.getCutMode()) ? rollingOrderNo : null);
        result.setSteelStripCode(task.getSteelStripCode());
        result.setBigRollCode(task.getBigRollCode());
        result.setMaterialKey(task.getMaterialKey());
        result.setCraftWidth(task.getCraftWidth());
        result.setUnitConsumeMillimeter(task.getUnitConsumeMillimeter());
        result.setCurlLength(task.getCurlLength());
        result.setCordWidth(task.getCordWidth());
        result.setBigRollConsumeQty(task.getBigRollConsumeQuantity());
        result.setCuttingAngle(task.getCuttingAngle());
        result.setCutMode(task.getCutMode());
        result.setMachineCode(task.getMachineCode());
        // 排程来源：AUTO 自动排程
        result.setSourceType("AUTO");
        // 发布状态：0 未发布
        result.setReleaseStatus("0");
        result.setPublishSuccessCount(0);
        // 锁定状态：0 未锁定
        result.setIsLocked("0");
        return result;
    }

    private void writeTask(Cd15ScheduleResult result, Cd15ShiftScheduleTask task) {
        BeanWrapper wrapper = wrapper(result);
        wrapper.setPropertyValue(property(task.getClassField(), "ScheduleDate"),
                Date.from(task.getExpectedStartTime()
                        .atZone(ZoneId.systemDefault()).toInstant()));
        wrapper.setPropertyValue(property(task.getClassField(), "PlanQty"),
                task.getPlanQuantity().doubleValue());
        wrapper.setPropertyValue(property(task.getClassField(), "ProduceOrder"),
                task.getProduceOrder());
        wrapper.setPropertyValue(property(task.getClassField(), "Analysis"),
                "TIMED_ROLLING");
    }

    private void clearAdjustableFields(Cd15ScheduleResult result, String classField) {
        if (isLocked(result, classField)) {
            return;
        }
        BeanWrapper wrapper = wrapper(result);
        wrapper.setPropertyValue(property(classField, "PlanQty"), 0D);
        wrapper.setPropertyValue(property(classField, "ProduceOrder"), null);
        wrapper.setPropertyValue(property(classField, "Analysis"), null);
    }

    /** 按结果全部班次计划量重算GDYY大卷消耗，避免滚动后保留旧汇总值。 */
    private void recalculateBigRollConsumption(Cd15ScheduleResult result) {
        if (result == null || result.getUnitConsumeMillimeter() == null
                || result.getUnitConsumeMillimeter().signum() <= 0
                || result.getCraftWidth() == null
                || result.getCraftWidth().signum() <= 0
                || result.getCordWidth() == null
                || result.getCordWidth().signum() <= 0) {
            throw new IllegalStateException("滚动结果缺少大卷消耗计算尺寸: "
                    + (result == null ? null : result.getId()));
        }
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (int classIndex = 1; classIndex <= 8; classIndex++) {
            totalQuantity = totalQuantity.add(
                    this.readPlan(result, "CLASS" + classIndex));
        }
        result.setBigRollConsumeQty(totalQuantity.signum() <= 0
                ? BigDecimal.ZERO
                : bigRollMeterCalculator.calculateForPlanQuantity(
                        totalQuantity, result.getUnitConsumeMillimeter(),
                        result.getCraftWidth(), result.getCordWidth(),
                        result.getSteelStripCode(), result.getBigRollCode()));
    }

    /**
     * 历史排程结果未保存大卷幅宽时，从本次滚动施工材料中补齐有效幅宽。
     */
    private void fillMissingCordWidth(
            Cd15ScheduleResult result,
            List<Cd15ConstructionMaterial> constructionMaterials) {
        if (result == null || (result.getCordWidth() != null
                && result.getCordWidth().signum() > 0)) {
            return;
        }
        Cd15ConstructionMaterial material = constructionMaterials.stream()
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
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "滚动结果未匹配到施工大卷幅宽: " + result.getId()));
        if (material.getCordWidth() == null
                || material.getCordWidth().signum() <= 0) {
            throw new IllegalStateException(
                    "滚动施工数据缺少有效大卷幅宽: " + result.getId());
        }
        result.setCordWidth(material.getCordWidth());
    }

    private boolean sameDecimal(BigDecimal first, BigDecimal second) {
        return first != null && second != null && first.compareTo(second) == 0;
    }

    private Cd15RollingPendingTask pending(Cd15ScheduleResult result,
                                           Cd15ShiftDescriptor shift) {
        BigDecimal quantity = readPlan(result, shift.getClassField());
        return Cd15RollingPendingTask.builder()
                .taskKey(result.getId() + ":" + shift.getClassField())
                .sourceResultId(result.getId()).sourceBatchNo(result.getCd15BatchNo())
                .sourceOrderNo(result.getOrderNo()).originalClassField(shift.getClassField())
                .originalProduceOrder(readOrder(result, shift.getClassField()))
                .targetClassField(shift.getClassField())
                .materialKey(this.resultMaterialKey(result))
                .steelStripCode(result.getSteelStripCode())
                .bigRollCode(result.getBigRollCode())
                .cuttingAngle(result.getCuttingAngle())
                .cutMode(result.getCutMode())
                .splitGroupKey(result.getGroupNo())
                .craftWidth(result.getCraftWidth())
                .unitConsumeMillimeter(result.getUnitConsumeMillimeter())
                .cordWidth(result.getCordWidth())
                .curlLength(result.getCurlLength())
                .bigRollConsumeQuantity(result.getBigRollConsumeQty())
                .sourceMachineCode(result.getMachineCode())
                .originalQuantity(quantity).remainingQuantity(quantity)
                .stableOrder(defaultOrder(readOrder(result, shift.getClassField()))).build();
    }

    private String resultMaterialKey(Cd15ScheduleResult result) {
        if (result.getMaterialKey() == null
                || result.getMaterialKey().trim().isEmpty()) {
            throw new IllegalStateException(
                    "原排程结果缺少MATERIAL_KEY，请先重新执行自动排程: "
                            + result.getId());
        }
        return result.getMaterialKey();
    }

    private List<Cd15UnscheduleResult> toUnscheduled(
            Cd15RollingTarget target, List<Cd15UnscheduledResultModel> models) {
        return models.stream().map(model -> {
            Cd15UnscheduleResult result = new Cd15UnscheduleResult();
            result.setFactoryCode(target.getFactoryCode());
            result.setScheduleDate(date(target));
            result.setBatchNo(target.getBatchNo());
            result.setSteelStripCode(model.getSteelStripCode());
            result.setBigRollCode(model.getBigRollCode());
            result.setCuttingAngle(model.getCuttingAngle());
            result.setDemandQty(value(model.getDemandQuantity()));
            result.setScheduledQty(value(model.getScheduledQuantity()));
            result.setUnscheduledQty(value(model.getUnscheduledQuantity()));
            result.setFailStage(model.getFailStage());
            result.setUnscheduleReasonCode(model.getReasonCode());
            result.setReasonOrder(model.getReasonOrder());
            result.setPrimaryReason(model.isPrimaryReason() ? "1" : "0");
            result.setUnscheduledReason(model.getReasonDescription());
            result.setCandidateMachineCodes(model.getCandidateMachineCodes());
            result.setDataSource("0");
            result.setProcessedTime(new Date());
            return result;
        }).collect(Collectors.toList());
    }

    private Cd15RollingAdjustmentDraft adjustment(
            Cd15ScheduleResult before, Cd15ScheduleResult after,
            List<Cd15ShiftDescriptor> shifts,
            List<Cd15ScheduleLaneAllocation> beforeLanes,
            List<Cd15ScheduleLaneAllocation> afterLanes) {
        String reason = shifts.stream().allMatch(
                shift -> readPlan(after, shift.getClassField()).signum() <= 0)
                ? "ZERO_DEMAND_REMOVE" : "ROLLING_REPLAN";
        String beforeClass = firstPlannedClass(before, shifts);
        String afterClass = firstPlannedClass(after, shifts);
        return Cd15RollingAdjustmentDraft.builder()
                .rollingItemKey(after.getId() + ":" + after.getSteelStripCode())
                .scheduleResultId(after.getId()).steelStripCode(after.getSteelStripCode())
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

    private Cd15RollingAdjustmentDraft insertedAdjustment(
            Cd15ScheduleResult after, List<Cd15ShiftDescriptor> shifts,
            List<Cd15ScheduleLaneAllocation> afterLanes) {
        String afterClass = firstPlannedClass(after, shifts);
        return Cd15RollingAdjustmentDraft.builder()
                .rollingItemKey(after.getOrderNo()).scheduleResultId(null)
                .steelStripCode(after.getSteelStripCode()).bigRollCode(after.getBigRollCode())
                .adjustType("ROLLING_INSERT").afterClassField(afterClass)
                .afterProduceOrder(readOrderOrNull(after, afterClass))
                .afterQuantity(readPlanOrZero(after, afterClass))
                .afterMachineCode(after.getMachineCode())
                .reasonCode("ROLLING_INSERT")
                .reasonDetail("最新需求产生新规格或新机台排程")
                .beforeSnapshot(snapshot(null, Collections.emptyList(), shifts))
                .afterSnapshot(snapshot(after, afterLanes, shifts)).build();
    }

    private List<Cd15ScheduleLaneAllocation> lanesForResult(
            List<Cd15ScheduleLaneAllocation> lanes,
            String orderNo,
            String steelStripCode) {
        return lanes.stream()
                .filter(item -> Objects.equals(orderNo, item.getOrderNo()))
                .filter(item -> Objects.equals(steelStripCode, item.getSteelStripCode()))
                .collect(Collectors.toList());
    }

    private String firstPlannedClass(Cd15ScheduleResult result,
                                     List<Cd15ShiftDescriptor> shifts) {
        if (result == null) {
            return null;
        }
        return shifts.stream().map(Cd15ShiftDescriptor::getClassField)
                .filter(classField -> readPlan(result, classField).signum() > 0)
                .findFirst().orElse(null);
    }

    private Integer readOrderOrNull(Cd15ScheduleResult result, String classField) {
        return result == null || classField == null ? null : readOrder(result, classField);
    }

    private BigDecimal readPlanOrZero(Cd15ScheduleResult result, String classField) {
        return result == null || classField == null
                ? BigDecimal.ZERO : readPlan(result, classField);
    }

    private Map<String, Object> snapshot(Cd15ScheduleResult result,
                                         List<Cd15ScheduleLaneAllocation> lanes,
                                         List<Cd15ShiftDescriptor> shifts) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("scheduleResult", result == null ? null : copyResult(result));
        snapshot.put("laneAllocations", new ArrayList<>(lanes));
        snapshot.put("affectedClasses", shifts.stream()
                .map(Cd15ShiftDescriptor::getClassField).collect(Collectors.toList()));
        snapshot.put("publishState", result == null ? null : result.getReleaseStatus());
        return snapshot;
    }

    private Map<Long, List<Cd15ScheduleLaneAllocation>> loadSourceLanes(
            List<Cd15ScheduleResult> sourceResults) {
        List<Long> ids = sourceResults.stream().map(Cd15ScheduleResult::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return laneAllocationMapper.selectList(
                        new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                                .in(Cd15ScheduleLaneAllocation::getScheduleResultId, ids))
                .stream().collect(Collectors.groupingBy(
                        Cd15ScheduleLaneAllocation::getScheduleResultId));
    }

    private List<Cd15ScheduleResult> loadSourceResults(Cd15RollingTarget target) {
        return scheduleResultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, target.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, date(target))
                .eq(Cd15ScheduleResult::getCd15BatchNo, target.getBatchNo()));
    }

    private Cd15AutoScheduleInput loadInput(Cd15AutoScheduleContext context,
                                            Cd15ShiftDescriptor shift) {
        return inputService.load(context.getFactoryCode(), context.getScheduleDate(),
                shift.getClassField(), shift.getShiftCode(),
                context.getResourceBaselineDate(),
                context.getResourceBaselineShiftCode(),
                context.getParameters().getAgingPeriodHours());
    }

    private Map<String, BigDecimal> curlLengthBySteelStrip(
            Cd15AutoScheduleInput input, Cd15AutoScheduleContext context) {
        return input.getConstructionMaterials().stream()
                .filter(Objects::nonNull).filter(item -> item.getSteelStripCode() != null)
                .collect(Collectors.toMap(Cd15ConstructionMaterial::getSteelStripCode,
                        item -> item.getCurlLength() == null || item.getCurlLength().signum() <= 0
                                ? context.getParameters().getRollCoilMeter() : item.getCurlLength(),
                        (left, right) -> left, HashMap::new));
    }

    private void appendTraces(List<Cd15ScheduleAttemptTrace> target,
                              List<Cd15ScheduleAttemptTrace> source) {
        if (source == null) {
            return;
        }
        source.forEach(trace -> {
            trace.setSequence(target.size() + 1);
            target.add(trace);
        });
    }

    private boolean isLocked(Cd15ScheduleResult result, String classField) {
        BigDecimal finish = readNumber(result, property(classField, "FinishQty"));
        return "1".equals(result.getIsLocked()) || finish.signum() > 0
                || ("1".equals(result.getProductionStatus())
                && readPlan(result, classField).signum() > 0);
    }

    private boolean hasPlanOutside(Cd15ScheduleResult result, String excludedClass) {
        for (int index = 1; index <= 8; index++) {
            String classField = "CLASS" + index;
            if (!classField.equals(excludedClass) && readPlan(result, classField).signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean allPlansZero(Cd15ScheduleResult result, List<Cd15ShiftDescriptor> shifts) {
        return shifts.stream().allMatch(
                shift -> readPlan(result, shift.getClassField()).signum() <= 0);
    }

    private String affectedFingerprint(Cd15ScheduleResult result,
                                       List<Cd15ShiftDescriptor> shifts) {
        if (result == null) {
            return "";
        }
        return shifts.stream().map(shift -> shift.getClassField() + ":"
                        + readPlan(result, shift.getClassField()) + ":"
                        + readOrder(result, shift.getClassField()))
                .collect(Collectors.joining("|")) + ":" + result.getMachineCode();
    }

    private BigDecimal readPlan(Cd15ScheduleResult result, String classField) {
        return readNumber(result, property(classField, "PlanQty"));
    }

    private Integer readOrder(Cd15ScheduleResult result, String classField) {
        Object value = wrapper(result).getPropertyValue(property(classField, "ProduceOrder"));
        return value == null ? null : ((Number) value).intValue();
    }

    private BigDecimal readNumber(Cd15ScheduleResult result, String property) {
        Object value = wrapper(result).getPropertyValue(property);
        return value == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private BeanWrapper wrapper(Cd15ScheduleResult result) {
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

    private Cd15ScheduleResult copyResult(Cd15ScheduleResult source) {
        Cd15ScheduleResult target = new Cd15ScheduleResult();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private Date date(Cd15RollingTarget target) {
        return Date.from(target.getScheduleDate().atStartOfDay(
                ZoneId.systemDefault()).toInstant());
    }

    private void validate(Cd15RollingTarget target, String inputVersion) {
        if (target == null || target.getScheduleDate() == null
                || target.getFactoryCode() == null || target.getBatchNo() == null
                || target.getTargetClassField() == null
                || target.getTargetShiftCode() == null
                || target.getResourceBaselineDate() == null
                || inputVersion == null) {
            throw new IllegalArgumentException("定时滚动排程目标和输入版本不能为空");
        }
    }

    @RequiredArgsConstructor
    private static class PlanningSet {
        private final List<Cd15RollingPendingTask> orderedTasks;
        private final List<Cd15ScheduleCandidate> candidates;
        private final Map<String, Cd15RollingPendingTask> taskByKey;
    }

    @RequiredArgsConstructor
    private static class ResultDiff {
        private final List<Cd15ScheduleResult> inserted;
        private final List<Cd15ScheduleResult> updated;
        private final List<Cd15ScheduleResult> deleted;
        private final List<Cd15ScheduleLaneAllocation> lanes;
        private final List<Cd15RollingAdjustmentDraft> adjustments;
    }
}
