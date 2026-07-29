package com.zlt.aps.tm.engine.service.impl;

import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.engine.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎面自动排程内部质量快照计算服务。
 *
 * <p>只读取计划组、机台班次任务链和库存短缺台账，不访问数据库，也不修改排程结果。</p>
 */
public class TmScheduleQualitySummaryService {

    /**
     * 构建与胎侧一致口径的内部质量快照。
     *
     * @param context 排程上下文
     * @param persistResult 落库汇总
     * @return 固定顺序的质量指标
     * @throws IllegalArgumentException 上下文或落库汇总为空时抛出
     */
    public Map<String, Object> build(TmScheduleContext context, TmPersistResult persistResult) {
        if (context == null || persistResult == null) {
            throw new IllegalArgumentException("胎面质量快照输入不能为空");
        }
        double coverageRate = this.calculateCoverageRate(context);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("taskCount", context.getPlanTaskGroupMap().isEmpty()
                ? context.getTaskDraftList().size() : context.getPlanTaskGroupMap().size());
        summary.put("resultCount", persistResult.getResultCount());
        summary.put("unplannedCount", persistResult.getUnplannedCount());
        summary.put("coverageRate", coverageRate);
        summary.put("unplannedRate", 1D - coverageRate);
        summary.put("machineUtilizationRate", this.calculateMachineUtilizationRate(context));
        summary.put("switchCount", this.calculateSwitchCount(context));
        summary.put("stockGuaranteeRate", this.calculateStockGuaranteeRate(context));
        summary.put("tailCompletionRate", this.calculateTailCompletionRate(context));
        summary.put("shiftCapacityHitRate", this.calculateShiftCapacityHitRate(context));
        return summary;
    }

    /**
     * 按计划组原始计划量与实际已排量计算覆盖率。
     *
     * @param context 排程上下文
     * @return 覆盖率
     */
    private double calculateCoverageRate(TmScheduleContext context) {
        if (context.getPlanTaskGroupMap().isEmpty()) {
            return 1D;
        }
        Map<String, BigDecimal> assignedQtyMap = this.buildPlanGroupAssignedQtyMap(context);
        BigDecimal totalQty = context.getPlanTaskGroupMap().values().stream()
                .map(TmPlanTaskGroup::getGroupFinalPlanQty)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!this.isPositive(totalQty)) {
            return 1D;
        }
        BigDecimal assignedQty = context.getPlanTaskGroupMap().values().stream()
                .map(group -> assignedQtyMap.getOrDefault(group.getPlanGroupKey(), BigDecimal.ZERO)
                        .min(this.nvl(group.getGroupFinalPlanQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return assignedQty.divide(totalQty, 6, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 计算全部启用机台班次的有效容量利用率，空闲机台班次进入分母。
     *
     * @param context 排程上下文
     * @return 利用率
     */
    private double calculateMachineUtilizationRate(TmScheduleContext context) {
        Map<String, BigDecimal> assignedQtyMap = this.buildMachineShiftAssignedQtyMap(context);
        Map<String, BigDecimal> switchDeductMap = this.buildMachineShiftSwitchDeductMap(context);
        BigDecimal assignedQty = assignedQtyMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCapacity = context.getMachineCandidateList().stream()
                .filter(Objects::nonNull)
                .filter(candidate -> !Boolean.FALSE.equals(candidate.getEnabled()))
                .flatMap(candidate -> java.util.stream.IntStream.rangeClosed(
                                1, TmScheduleConstants.TM_MAX_SHIFT_ORDER)
                        .mapToObj(shiftOrder -> this.resolveEffectiveCapacity(
                                candidate, shiftOrder, switchDeductMap)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!this.isPositive(totalCapacity)) {
            return 0D;
        }
        return assignedQty.min(totalCapacity).divide(totalCapacity, 6, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 统计实际任务链中的规格、胶料或口型切换次数。
     *
     * @param context 排程上下文
     * @return 切换任务数
     */
    private long calculateSwitchCount(TmScheduleContext context) {
        return context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> this.isPositive(task.getPreviousSpecSwitchHours())
                        || this.isPositive(task.getPreviousGlueSwitchHours()))
                .count();
    }

    /**
     * 按产品班次短缺台账计算库存保证率。
     *
     * @param context 排程上下文
     * @return 库存保证率
     */
    private double calculateStockGuaranteeRate(TmScheduleContext context) {
        if (context.getProductShiftShortageMap().isEmpty()) {
            return 1D;
        }
        long guaranteedCount = context.getProductShiftShortageMap().values().stream()
                .filter(value -> !this.isPositive(value))
                .count();
        return (double) guaranteedCount / context.getProductShiftShortageMap().size();
    }

    /**
     * 按计划组聚合收尾完成率，避免拆分和顺延重复累计。
     *
     * @param context 排程上下文
     * @return 收尾完成率
     */
    private double calculateTailCompletionRate(TmScheduleContext context) {
        List<TmPlanTaskGroup> tailGroupList = context.getPlanTaskGroupMap().values().stream()
                .filter(Objects::nonNull)
                .filter(group -> group.getAggregateTask() != null)
                .filter(group -> "1".equals(group.getAggregateTask().getTailFlag()))
                .collect(Collectors.toList());
        if (tailGroupList.isEmpty()) {
            return 1D;
        }
        Map<String, BigDecimal> assignedQtyMap = this.buildPlanGroupAssignedQtyMap(context);
        long completedCount = tailGroupList.stream()
                .filter(group -> this.isTailCompleted(group,
                        assignedQtyMap.getOrDefault(group.getPlanGroupKey(), BigDecimal.ZERO)))
                .count();
        return (double) completedCount / tailGroupList.size();
    }

    /**
     * 计算全部启用机台班次未超过有效容量的比例。
     *
     * @param context 排程上下文
     * @return 容量命中率
     */
    private double calculateShiftCapacityHitRate(TmScheduleContext context) {
        List<TmMachineCandidate> candidateList = context.getMachineCandidateList().stream()
                .filter(Objects::nonNull)
                .filter(candidate -> !Boolean.FALSE.equals(candidate.getEnabled()))
                .collect(Collectors.toList());
        if (candidateList.isEmpty()) {
            return 0D;
        }
        Map<String, BigDecimal> assignedQtyMap = this.buildMachineShiftAssignedQtyMap(context);
        Map<String, BigDecimal> switchDeductMap = this.buildMachineShiftSwitchDeductMap(context);
        long hitCount = candidateList.stream()
                .flatMap(candidate -> java.util.stream.IntStream.rangeClosed(
                                1, TmScheduleConstants.TM_MAX_SHIFT_ORDER)
                        .mapToObj(shiftOrder -> assignedQtyMap.getOrDefault(
                                        candidate.getMachineCode() + "|" + shiftOrder, BigDecimal.ZERO)
                                .compareTo(this.resolveEffectiveCapacity(
                                        candidate, shiftOrder, switchDeductMap)) <= 0))
                .filter(Boolean.TRUE::equals)
                .count();
        return (double) hitCount / (candidateList.size() * TmScheduleConstants.TM_MAX_SHIFT_ORDER);
    }

    /**
     * 判断收尾组是否已完成。
     *
     * @param group 收尾计划组
     * @param assignedQty 实际已排量
     * @return true 表示已清零
     */
    private boolean isTailCompleted(TmPlanTaskGroup group, BigDecimal assignedQty) {
        TmTaskDraft task = group == null ? null : group.getAggregateTask();
        if (task == null) {
            return false;
        }
        BigDecimal tailQty = this.nvl(task.getTailBalanceQty())
                .multiply(this.nvl(task.getTreadShoulderLength()));
        return !this.isPositive(tailQty) || this.nvl(assignedQty).compareTo(tailQty) >= 0;
    }

    /**
     * 按计划组汇总实际已排量。
     *
     * @param context 排程上下文
     * @return 计划组已排量
     */
    private Map<String, BigDecimal> buildPlanGroupAssignedQtyMap(TmScheduleContext context) {
        return context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> task.getPlanGroupKey() != null)
                .collect(Collectors.groupingBy(TmTaskDraft::getPlanGroupKey, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPlanQty()), BigDecimal::add)));
    }

    /**
     * 按机台班次汇总实际已排量。
     *
     * @param context 排程上下文
     * @return 机台班次已排量
     */
    private Map<String, BigDecimal> buildMachineShiftAssignedQtyMap(TmScheduleContext context) {
        return context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> task.getMachineCode() != null && task.getShiftOrder() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getMachineCode() + "|" + task.getShiftOrder(),
                        LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPlanQty()), BigDecimal::add)));
    }

    /**
     * 按机台班次汇总实际发生的规格、胶料切换产能扣减。
     *
     * @param context 排程上下文
     * @return 机台班次切换产能扣减
     */
    private Map<String, BigDecimal> buildMachineShiftSwitchDeductMap(TmScheduleContext context) {
        return context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> task.getMachineCode() != null && task.getShiftOrder() != null)
                .collect(Collectors.groupingBy(
                        task -> task.getMachineCode() + "|" + task.getShiftOrder(),
                        LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPreviousSpecSwitchHours())
                                        .multiply(this.nvl(task.getMachineSpeed()))
                                        .add(this.nvl(task.getPreviousGlueSwitchCapacityDeduct())),
                                BigDecimal::add)));
    }

    /**
     * 计算扣除停机及实际换型折算量后的有效班产。
     *
     * @param candidate 候选机台
     * @param shiftOrder 班次顺序
     * @param switchDeductMap 机台班次切换产能扣减
     * @return 有效班产
     */
    private BigDecimal resolveEffectiveCapacity(TmMachineCandidate candidate, int shiftOrder,
                                                Map<String, BigDecimal> switchDeductMap) {
        BigDecimal maxCapacity = this.nvl(candidate.getMaxCapacity());
        BigDecimal maintenanceHours = this.nvl(candidate.getMaintenanceHoursByShift().get(shiftOrder));
        BigDecimal switchDeduct = switchDeductMap.getOrDefault(
                candidate.getMachineCode() + "|" + shiftOrder, BigDecimal.ZERO);
        return maxCapacity.subtract(maintenanceHours.multiply(this.nvl(candidate.getMachineSpeed())))
                .subtract(switchDeduct)
                .max(BigDecimal.ZERO);
    }

    /**
     * 判断数值是否为正数。
     *
     * @param value 数值
     * @return true 表示大于零
     */
    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /**
     * 空值转换为零。
     *
     * @param value 原始值
     * @return 非空值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
