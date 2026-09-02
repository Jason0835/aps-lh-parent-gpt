package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 自动排程质量摘要公共引擎。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 * @param <G> 计划组类型
 * @param <M> 候选机台类型
 * @param <P> 落库摘要类型
 */
public final class ScheduleQualityEngine<C, T extends ScheduleQualityTask,
        G extends ScheduleQualityPlanGroup<T>, M extends ScheduleQualityMachineCandidate,
        P extends ScheduleQualityPersistSummary> {

    /**
     * 构建质量摘要。
     *
     * @param context 排程上下文
     * @param persistSummary 落库摘要
     * @param modelAccess 模型访问端口
     * @param policy 领域差异策略
     * @return 固定顺序的质量指标
     */
    public Map<String, Object> build(C context, P persistSummary,
                                     ScheduleQualityModelAccess<C, T, G, M> modelAccess,
                                     ScheduleQualityPolicy<C, P, M> policy) {
        policy.validate(context, persistSummary);
        Map<String, G> planGroupMap = modelAccess.getPlanTaskGroupMap(context);
        List<T> scheduledTasks = modelAccess.getScheduledTasks(context);
        List<M> candidateList = modelAccess.getMachineCandidates(context).stream()
                .filter(Objects::nonNull)
                .filter(candidate -> !Boolean.FALSE.equals(candidate.getEnabled()))
                .collect(Collectors.toList());
        Map<String, BigDecimal> assignedByGroup = this.buildPlanGroupAssignedQtyMap(scheduledTasks);
        Map<String, BigDecimal> assignedByMachineShift = new LinkedHashMap<>();
        Map<String, BigDecimal> switchDeductByMachineShift = new LinkedHashMap<>();
        this.populateMachineShiftTaskSummary(scheduledTasks, assignedByMachineShift,
                switchDeductByMachineShift);

        double coverageRate = this.calculateCoverageRate(planGroupMap, assignedByGroup);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("taskCount", planGroupMap.isEmpty() ? modelAccess.getTaskDraftCount(context) : planGroupMap.size());
        summary.put("resultCount", persistSummary.getResultCount());
        summary.put("unplannedCount", persistSummary.getUnplannedCount());
        summary.put("coverageRate", coverageRate);
        summary.put("unplannedRate", 1D - coverageRate);
        summary.put("machineUtilizationRate", this.calculateMachineUtilizationRate(candidateList,
                assignedByMachineShift, switchDeductByMachineShift, policy));
        summary.put("switchCount", this.calculateSwitchCount(scheduledTasks, policy));
        summary.put("stockGuaranteeRate", this.calculateStockGuaranteeRate(
                modelAccess.getProductShiftShortageMap(context)));
        summary.put("tailCompletionRate", this.calculateTailCompletionRate(planGroupMap, assignedByGroup));
        summary.put("shiftCapacityHitRate", this.calculateShiftCapacityHitRate(candidateList,
                assignedByMachineShift, switchDeductByMachineShift, policy));
        return summary;
    }

    private double calculateCoverageRate(Map<String, G> planGroupMap,
                                         Map<String, BigDecimal> assignedQtyMap) {
        if (planGroupMap.isEmpty()) {
            return 1D;
        }
        BigDecimal totalQty = planGroupMap.values().stream()
                .map(ScheduleQualityPlanGroup::getGroupFinalPlanQty)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!this.isPositive(totalQty)) {
            return 1D;
        }
        BigDecimal assignedQty = planGroupMap.values().stream()
                .map(group -> assignedQtyMap.getOrDefault(group.getPlanGroupKey(), BigDecimal.ZERO)
                        .min(this.nvl(group.getGroupFinalPlanQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return assignedQty.divide(totalQty, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateMachineUtilizationRate(List<M> candidateList,
                                                   Map<String, BigDecimal> assignedQtyMap,
                                                   Map<String, BigDecimal> switchDeductMap,
                                                   ScheduleQualityPolicy<C, P, M> policy) {
        if (policy.isAssignedTaskRequiredForUtilization() && assignedQtyMap.isEmpty()) {
            return 0D;
        }
        BigDecimal assignedQty = assignedQtyMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCapacity = candidateList.stream()
                .flatMap(candidate -> java.util.stream.IntStream.rangeClosed(1, policy.getMaxShiftOrder())
                        .mapToObj(shiftOrder -> this.resolveEffectiveCapacity(
                                candidate, shiftOrder, switchDeductMap, policy)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!this.isPositive(totalCapacity)) {
            return 0D;
        }
        return assignedQty.min(totalCapacity).divide(totalCapacity, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private long calculateSwitchCount(List<T> scheduledTasks, ScheduleQualityPolicy<C, P, M> policy) {
        return scheduledTasks.stream()
                .filter(task -> this.isPositive(task.getPreviousSpecSwitchHours())
                        || this.isPositive(task.getPreviousGlueSwitchHours())
                        || policy.isMouthPlateSwitchCounted()
                        && Boolean.TRUE.equals(task.getQualityMouthPlateSwitched()))
                .count();
    }

    private double calculateStockGuaranteeRate(Map<String, BigDecimal> shortageMap) {
        if (shortageMap.isEmpty()) {
            return 1D;
        }
        long guaranteedCount = shortageMap.values().stream().filter(value -> !this.isPositive(value)).count();
        return (double) guaranteedCount / shortageMap.size();
    }

    private double calculateTailCompletionRate(Map<String, G> planGroupMap,
                                               Map<String, BigDecimal> assignedQtyMap) {
        List<G> tailGroupList = planGroupMap.values().stream()
                .filter(Objects::nonNull)
                .filter(group -> group.getAggregateTask() != null)
                .filter(group -> "1".equals(group.getAggregateTask().getTailFlag()))
                .collect(Collectors.toList());
        if (tailGroupList.isEmpty()) {
            return 1D;
        }
        long completedCount = tailGroupList.stream()
                .filter(group -> this.isTailCompleted(group,
                        assignedQtyMap.getOrDefault(group.getPlanGroupKey(), BigDecimal.ZERO)))
                .count();
        return (double) completedCount / tailGroupList.size();
    }

    private double calculateShiftCapacityHitRate(List<M> candidateList,
                                                 Map<String, BigDecimal> assignedQtyMap,
                                                 Map<String, BigDecimal> switchDeductMap,
                                                 ScheduleQualityPolicy<C, P, M> policy) {
        if (candidateList.isEmpty()) {
            return 0D;
        }
        long hitCount = candidateList.stream()
                .flatMap(candidate -> java.util.stream.IntStream.rangeClosed(1, policy.getMaxShiftOrder())
                        .mapToObj(shiftOrder -> assignedQtyMap.getOrDefault(
                                        candidate.getMachineCode() + "|" + shiftOrder, BigDecimal.ZERO)
                                .compareTo(this.resolveEffectiveCapacity(
                                        candidate, shiftOrder, switchDeductMap, policy)) <= 0))
                .filter(Boolean.TRUE::equals)
                .count();
        return (double) hitCount / (candidateList.size() * policy.getMaxShiftOrder());
    }

    private boolean isTailCompleted(G group, BigDecimal assignedQty) {
        T task = group == null ? null : group.getAggregateTask();
        if (task == null) {
            return false;
        }
        if (Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())) {
            BigDecimal requiredPlanQty = this.nvl(task.getFormingShutdownCloseOutDemandQty())
                    .subtract(this.nvl(task.getStockDeductQty())).max(BigDecimal.ZERO);
            return this.nvl(assignedQty).compareTo(requiredPlanQty) >= 0;
        }
        BigDecimal tailQty = this.nvl(task.getTailBalanceQty())
                .multiply(this.nvl(task.getQualityProductLength()));
        return !this.isPositive(tailQty) || this.nvl(assignedQty).compareTo(tailQty) >= 0;
    }

    private Map<String, BigDecimal> buildPlanGroupAssignedQtyMap(List<T> scheduledTasks) {
        return scheduledTasks.stream()
                .filter(task -> task.getPlanGroupKey() != null)
                .collect(Collectors.groupingBy(ScheduleQualityTask::getPlanGroupKey, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPlanQty()), BigDecimal::add)));
    }

    private void populateMachineShiftTaskSummary(List<T> scheduledTasks,
                                                 Map<String, BigDecimal> assignedQtyMap,
                                                 Map<String, BigDecimal> switchDeductMap) {
        scheduledTasks.stream()
                .filter(task -> task.getMachineCode() != null && task.getShiftOrder() != null)
                .forEach(task -> {
                    String key = task.getMachineCode() + "|" + task.getShiftOrder();
                    assignedQtyMap.merge(key, this.nvl(task.getPlanQty()), BigDecimal::add);
                    BigDecimal switchDeduct = this.nvl(task.getPreviousSpecSwitchHours())
                            .multiply(this.nvl(task.getMachineSpeed()))
                            .add(this.nvl(task.getPreviousGlueSwitchCapacityDeduct()));
                    switchDeductMap.merge(key, switchDeduct, BigDecimal::add);
                });
    }

    private BigDecimal resolveEffectiveCapacity(M candidate, int shiftOrder,
                                                Map<String, BigDecimal> switchDeductMap,
                                                ScheduleQualityPolicy<C, P, M> policy) {
        BigDecimal maintenanceHours = this.nvl(candidate.getMaintenanceHoursByShift().get(shiftOrder));
        BigDecimal switchDeduct = switchDeductMap.getOrDefault(
                candidate.getMachineCode() + "|" + shiftOrder, BigDecimal.ZERO);
        return policy.resolveMachineMaxCapacity(candidate)
                .subtract(maintenanceHours.multiply(this.nvl(candidate.getMachineSpeed())))
                .subtract(switchDeduct).max(BigDecimal.ZERO);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

