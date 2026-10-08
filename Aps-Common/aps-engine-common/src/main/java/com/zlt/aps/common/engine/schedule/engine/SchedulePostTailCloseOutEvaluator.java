package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实际库存关账后的产品收尾确认组件。
 *
 * <p>组件只消费公共任务运行态、实际库存和短缺台账，不访问数据库，也不依赖 TM/TC
 * 领域类型。领域机台服务在本班实际分配及库存关账完成后调用它。</p>
 */
public final class SchedulePostTailCloseOutEvaluator {

    /**
     * 收尾来源数据不完整或发生冲突。
     */
    public static final String REASON_SOURCE_TAIL_UNRESOLVED = "SOURCE_TAIL_UNRESOLVED";

    /**
     * 收尾范围内仍有真实短缺。
     */
    public static final String REASON_REAL_SHORTAGE_NOT_COVERED = "REAL_SHORTAGE_NOT_COVERED";

    /**
     * 实际关账库存不足以覆盖收尾范围后续需求。
     */
    public static final String REASON_CLOSE_OUT_STOCK_NOT_ENOUGH = "CLOSE_OUT_STOCK_NOT_ENOUGH";

    /**
     * 实际收尾确认成功。
     */
    public static final String REASON_ACTUAL_INVENTORY_CLOSE_OUT_CONFIRMED =
            "ACTUAL_INVENTORY_CLOSE_OUT_CONFIRMED";

    /**
     * 尚未出现现有规则认定的有效收尾承接候选。
     */
    public static final String REASON_CLOSE_OUT_CANDIDATE_NOT_READY =
            "CLOSE_OUT_CANDIDATE_NOT_READY";

    private SchedulePostTailCloseOutEvaluator() {
    }

    /**
     * 在本班实际库存关账完成后更新产品收尾状态。
     *
     * @param taskList        本批次当前任务列表
     * @param shortageMap     已关账的产品班次短缺台账
     * @param runtimeStockMap 实际关账后的运行库存
     * @param stateMap        按产品保存的收尾状态
     * @param shiftOrder      已完成关账的逻辑班次
     */
    public static void confirmAfterInventoryClose(List<? extends ScheduleTaskDraftModel> taskList,
                                                  Map<String, BigDecimal> shortageMap,
                                                  Map<String, BigDecimal> runtimeStockMap,
                                                  Map<String, SchedulePostTailCloseOutState> stateMap,
                                                  Integer shiftOrder) {
        if (taskList == null || taskList.isEmpty() || shiftOrder == null || shiftOrder <= 0
                || stateMap == null) {
            return;
        }
        Map<String, List<ScheduleTaskDraftModel>> productTaskMap = taskList.stream()
                .filter(Objects::nonNull)
                .filter(task -> !Boolean.TRUE.equals(task.getSourceExplainTask()))
                .filter(task -> hasText(task.getProcessCode()))
                .collect(Collectors.groupingBy(task -> task.getProcessCode().trim(),
                        LinkedHashMap::new, Collectors.toList()));
        productTaskMap.forEach((processCode, productTaskList) -> {
            SchedulePostTailCloseOutState previousState = stateMap.get(processCode);
            if (previousState != null && Boolean.TRUE.equals(previousState.getConfirmed())) {
                return;
            }
            SchedulePostTailCloseOutState state = previousState == null
                    ? new SchedulePostTailCloseOutState() : previousState;
            state.setProcessCode(processCode);
            state.setConfirmationShiftOrder(shiftOrder);
            state.setSourceKeySummary(resolveSourceKeySummary(productTaskList));
            BigDecimal closedStockQty = resolveStock(runtimeStockMap, processCode);
            BigDecimal previousUnresolvedShortageQty = previousState == null
                    ? BigDecimal.ZERO : nvl(previousState.getUnresolvedShortageQty());
            BigDecimal currentShortageQty = resolveCurrentShortage(shortageMap, processCode, shiftOrder);
            BigDecimal shortageRecoveredQty = closedStockQty.min(previousUnresolvedShortageQty)
                    .max(BigDecimal.ZERO);
            state.setClosedStockQty(closedStockQty);
            state.setAvailableCloseOutStockQty(closedStockQty.subtract(shortageRecoveredQty)
                    .max(BigDecimal.ZERO));
            state.setUnresolvedShortageQty(previousUnresolvedShortageQty
                    .subtract(shortageRecoveredQty).add(currentShortageQty));
            state.setRemainingCloseOutDemandQty(resolveRemainingCloseOutDemand(productTaskList, shiftOrder));
            ScheduleTaskDraftModel candidateTask = productTaskList.stream()
                    .filter(task -> isCloseOutCandidate(task, shiftOrder))
                    .findFirst().orElse(null);
            if (candidateTask == null) {
                state.setConfirmed(Boolean.FALSE);
                state.setActualCloseOutShiftOrder(null);
                state.setConfirmationReason(REASON_CLOSE_OUT_CANDIDATE_NOT_READY);
                stateMap.put(processCode, state);
                return;
            }

            String validationReason = validateSourceTail(productTaskList);
            if (validationReason == null
                    && state.getUnresolvedShortageQty().compareTo(BigDecimal.ZERO) > 0) {
                validationReason = REASON_REAL_SHORTAGE_NOT_COVERED;
            }
            if (validationReason == null
                    && state.getAvailableCloseOutStockQty().compareTo(state.getRemainingCloseOutDemandQty()) < 0) {
                validationReason = REASON_CLOSE_OUT_STOCK_NOT_ENOUGH;
            }
            if (validationReason == null) {
                state.setConfirmed(Boolean.TRUE);
                state.setActualCloseOutShiftOrder(shiftOrder);
                state.setConfirmationReason(REASON_ACTUAL_INVENTORY_CLOSE_OUT_CONFIRMED);
            } else {
                state.setConfirmed(Boolean.FALSE);
                state.setActualCloseOutShiftOrder(null);
                state.setConfirmationReason(validationReason);
            }
            stateMap.put(processCode, state);
        });
    }

    private static boolean isCloseOutCandidate(ScheduleTaskDraftModel task, Integer shiftOrder) {
        if (task == null || !Objects.equals(task.getShiftOrder(), shiftOrder)) {
            return false;
        }
        if (Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())) {
            return true;
        }
        ScheduleProductTailDecisionModel productDecision = task.getProductTailDecision();
        if (productDecision == null || !Boolean.TRUE.equals(productDecision.getDecisionAvailable())
                || productDecision.getFinalFormingTailShiftKey() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(productDecision.getFutureTailDemandPresent())) {
            ScheduleFormingShiftKey lastReceivingShiftKey = productDecision.getLastTailReceivingShiftKey();
            return lastReceivingShiftKey != null
                    && Objects.equals(lastReceivingShiftKey.getShiftOrder(), shiftOrder)
                    && (task.getFutureDemandAllocationList() == null
                    ? java.util.stream.Stream.<ScheduleFutureDemandAllocationModel>empty()
                    : task.getFutureDemandAllocationList().stream())
                    .filter(SchedulePostTailCloseOutEvaluator::isPositiveAllocation)
                    .filter(allocation -> lastReceivingShiftKey.equals(allocation.getTargetProductionShiftKey()))
                    .anyMatch(allocation -> allocation.getSourceDemandShiftKeys() != null
                            && allocation.getSourceDemandShiftKeys()
                            .contains(productDecision.getFinalFormingTailShiftKey()));
        }
        return task.getFormingCoverageShiftKeySet() != null
                && task.getFormingCoverageShiftKeySet().contains(productDecision.getFinalFormingTailShiftKey());
    }

    private static String validateSourceTail(List<ScheduleTaskDraftModel> taskList) {
        List<ScheduleFormingTailSourceModel> sourceList = taskList.stream()
                .filter(Objects::nonNull)
                .flatMap(task -> task.getFormingTailSourceList() == null
                        ? java.util.stream.Stream.<ScheduleFormingTailSourceModel>empty()
                        : task.getFormingTailSourceList().stream())
                .collect(Collectors.toList());
        boolean hasShutdownCloseOut = taskList.stream()
                .anyMatch(task -> Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag()));
        if (sourceList.isEmpty() && hasShutdownCloseOut) {
            return null;
        }
        if (sourceList.isEmpty()) {
            return REASON_SOURCE_TAIL_UNRESOLVED;
        }
        Map<String, ScheduleFormingTailSourceModel> sourceSnapshotMap = new LinkedHashMap<>();
        for (ScheduleFormingTailSourceModel source : sourceList) {
            if (source == null || !hasText(source.getSourceKey())
                    || !isValidTailShiftKey(source.getTailShiftKey())
                    || hasText(source.getUnresolvedReason())) {
                return REASON_SOURCE_TAIL_UNRESOLVED;
            }
            ScheduleFormingTailSourceModel previousSource = sourceSnapshotMap.putIfAbsent(
                    source.getSourceKey().trim(), source);
            if (previousSource != null && !isSameSourceSnapshot(previousSource, source)) {
                return REASON_SOURCE_TAIL_UNRESOLVED;
            }
        }
        return null;
    }

    /**
     * 比较相同来源唯一键的快照，防止复制任务携带相互矛盾的收尾边界。
     *
     * @param first  先出现的来源快照
     * @param second 后出现的来源快照
     * @return true表示两个来源快照一致
     */
    private static boolean isSameSourceSnapshot(ScheduleFormingTailSourceModel first,
                                                ScheduleFormingTailSourceModel second) {
        return Objects.equals(first.getSourceScheduleDate(), second.getSourceScheduleDate())
                && Objects.equals(first.getProcessCode(), second.getProcessCode())
                && Objects.equals(first.getEmbryoCode(), second.getEmbryoCode())
                && Objects.equals(first.getSourceOrderNo(), second.getSourceOrderNo())
                && Objects.equals(first.getOriginalClassQtyMap(), second.getOriginalClassQtyMap())
                && Objects.equals(first.getFormingTailRemainQty(), second.getFormingTailRemainQty())
                && Objects.equals(first.getTailShiftKey(), second.getTailShiftKey());
    }

    /**
     * 校验来源收尾班次是否具备可比较的日期和正数班次。
     *
     * @param tailShiftKey 来源收尾班次
     * @return true表示收尾边界有效
     */
    private static boolean isValidTailShiftKey(ScheduleFormingShiftKey tailShiftKey) {
        return tailShiftKey != null && tailShiftKey.getProductionDate() != null
                && tailShiftKey.getShiftOrder() != null && tailShiftKey.getShiftOrder() > 0;
    }

    private static BigDecimal resolveCurrentShortage(Map<String, BigDecimal> shortageMap,
                                                     String processCode, Integer shiftOrder) {
        if (shortageMap == null || shortageMap.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return nvl(shortageMap.get(processCode + "|" + shiftOrder)).max(BigDecimal.ZERO);
    }

    private static BigDecimal resolveRemainingCloseOutDemand(
            List<ScheduleTaskDraftModel> taskList, Integer shiftOrder) {
        ScheduleProductTailDecisionModel productDecision = taskList.stream()
                .map(ScheduleTaskDraftModel::getProductTailDecision)
                .filter(Objects::nonNull)
                .filter(decision -> decision.getFinalFormingTailShiftKey() != null)
                .findFirst().orElse(null);
        if (productDecision == null || productDecision.getFinalFormingTailShiftKey() == null) {
            return BigDecimal.ZERO;
        }
        ScheduleFormingShiftKey finalTailShiftKey = productDecision.getFinalFormingTailShiftKey();
        if (Boolean.TRUE.equals(productDecision.getFutureTailDemandPresent())) {
            return deduplicateAllocations(taskList).stream()
                    .filter(SchedulePostTailCloseOutEvaluator::isPositiveAllocation)
                    .filter(allocation -> allocation.getSourceDemandShiftKeys() != null
                            && allocation.getSourceDemandShiftKeys().contains(finalTailShiftKey))
                    .filter(allocation -> allocation.getTargetProductionShiftKey() != null
                            && allocation.getTargetProductionShiftKey().getShiftOrder() != null
                            && allocation.getTargetProductionShiftKey().getShiftOrder() > shiftOrder)
                    .map(ScheduleFutureDemandAllocationModel::getAllocatedQty)
                    .map(SchedulePostTailCloseOutEvaluator::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // 普通覆盖窗口没有前移分配记录，必须按当前产品后续仍在收尾范围内的有效需求校验库存。
        return taskList.stream()
                .filter(task -> isFutureNormalCloseOutDemandTask(task, shiftOrder, finalTailShiftKey))
                .map(ScheduleTaskDraftModel::getCurrentShiftDemandQty)
                .map(SchedulePostTailCloseOutEvaluator::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 判断任务是否属于普通覆盖窗口的后续收尾需求。
     *
     * @param task              产品任务
     * @param currentShiftOrder 当前已关账班次
     * @param finalTailShiftKey 产品最终成型收尾班次
     * @return true表示任务是当前班之后且仍覆盖最终收尾班次的有效需求
     */
    private static boolean isFutureNormalCloseOutDemandTask(ScheduleTaskDraftModel task,
                                                            Integer currentShiftOrder,
                                                            ScheduleFormingShiftKey finalTailShiftKey) {
        if (task == null || task.getShiftOrder() == null || currentShiftOrder == null
                || task.getShiftOrder() <= currentShiftOrder
                || nvl(task.getCurrentShiftDemandQty()).compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (Boolean.TRUE.equals(task.getSourceExplainTask())) {
            return false;
        }
        return task.getFormingCoverageShiftKeySet() != null
                && task.getFormingCoverageShiftKeySet().contains(finalTailShiftKey);
    }

    private static List<ScheduleFutureDemandAllocationModel> deduplicateAllocations(
            List<ScheduleTaskDraftModel> taskList) {
        Map<String, ScheduleFutureDemandAllocationModel> allocationMap = new LinkedHashMap<>();
        taskList.stream()
                .filter(Objects::nonNull)
                .flatMap(task -> task.getFutureDemandAllocationList() == null
                        ? java.util.stream.Stream.<ScheduleFutureDemandAllocationModel>empty()
                        : task.getFutureDemandAllocationList().stream())
                .filter(Objects::nonNull)
                .forEach(allocation -> allocationMap.putIfAbsent(resolveAllocationIdentity(allocation), allocation));
        return allocationMap.values().stream().collect(Collectors.toList());
    }

    private static String resolveAllocationIdentity(ScheduleFutureDemandAllocationModel allocation) {
        return String.valueOf(allocation.getSourceKey()) + "|"
                + String.valueOf(allocation.getSourceDemandShiftKeys()) + "|"
                + String.valueOf(allocation.getTargetProductionShiftKey()) + "|"
                + String.valueOf(allocation.getAllocationStatus()) + "|"
                + String.valueOf(allocation.getAllocatedQty());
    }

    private static String resolveSourceKeySummary(List<ScheduleTaskDraftModel> taskList) {
        Set<String> sourceKeySet = new LinkedHashSet<>();
        taskList.stream().filter(Objects::nonNull).forEach(task -> {
            if (task.getFormingTailSourceList() != null) {
                task.getFormingTailSourceList().stream()
                        .filter(Objects::nonNull)
                        .map(ScheduleFormingTailSourceModel::getSourceKey)
                        .filter(SchedulePostTailCloseOutEvaluator::hasText)
                        .forEach(sourceKeySet::add);
            }
            if (hasText(task.getFormingSourceKey())) {
                sourceKeySet.add(task.getFormingSourceKey());
            }
        });
        return String.join(",", sourceKeySet);
    }

    private static BigDecimal resolveStock(Map<String, BigDecimal> runtimeStockMap, String processCode) {
        return runtimeStockMap == null ? BigDecimal.ZERO
                : nvl(runtimeStockMap.get(processCode));
    }

    private static boolean isPositiveAllocation(ScheduleFutureDemandAllocationModel allocation) {
        return allocation != null
                && ScheduleFutureDemandAllocationModel.STATUS_ALLOCATED.equals(allocation.getAllocationStatus())
                && allocation.getAllocatedQty() != null
                && allocation.getAllocatedQty().compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
