package com.zlt.aps.tc.engine.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.engine.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎侧自动排程质量指标汇总服务。
 *
 * <p>统一计算覆盖率、未排率、机台利用率、切换次数、库存保证率、
 * 收尾完成率和班产上限命中率，避免任务表与接口响应使用不同口径。</p>
 */
@Service
public class TcScheduleQualitySummaryService {

    /**
     * 构建自动排程质量摘要。
     *
     * @param context 排程运行上下文
     * @param persistResult 落库汇总
     * @return 按固定顺序组织的质量摘要
     * @throws IllegalArgumentException 上下文或落库汇总为空时抛出
     */
    public Map<String, Object> build(TcScheduleContext context, TcPersistResult persistResult) {
        if (context == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.contextEmpty"));
        }
        if (persistResult == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.persistFailed"));
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        int taskCount = context.getPlanTaskGroupMap().isEmpty()
                ? context.getTaskDraftList().size() : context.getPlanTaskGroupMap().size();
        int unplannedCount = persistResult.getUnplannedCount();
        double coverageRate = this.calculateCoverageRate(context);
        summary.put("taskCount", taskCount);
        summary.put("resultCount", persistResult.getResultCount());
        summary.put("unplannedCount", unplannedCount);
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
     * 按非空机台班次链的已排米数除以有效班产定额计算机台利用率。
     *
     * @param context 排程运行上下文
     * @return 0 到 1 之间的机台利用率
     */
    private double calculateMachineUtilizationRate(TcScheduleContext context) {
        Map<String, BigDecimal> assignedQtyMap = this.buildMachineShiftAssignedQtyMap(context);
        Map<String, BigDecimal> switchDeductMap = this.buildMachineShiftSwitchDeductMap(context);
        if (assignedQtyMap.isEmpty()) {
            return 0D;
        }
        BigDecimal assignedQty = assignedQtyMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCapacity = context.getMachineCandidateList().stream()
                .filter(candidate -> candidate != null && !Boolean.FALSE.equals(candidate.getEnabled()))
                .flatMap(candidate -> java.util.stream.IntStream.rangeClosed(
                        1, TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                        .mapToObj(shiftOrder -> this.resolveEffectiveCapacity(
                                candidate, shiftOrder, switchDeductMap)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!this.isPositive(totalCapacity)) {
            return 0D;
        }
        return assignedQty.min(totalCapacity).divide(totalCapacity, 6, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 统计任务链中实际发生胶料或口型切换的任务数量(详设§14.11 胶料/口型口径)。
     *
     * @param context 排程运行上下文
     * @return 切换任务数量，同一任务同时发生两类切换只计一次
     */
    private long calculateSwitchCount(TcScheduleContext context) {
        return context.getTaskChainGroup().values().stream()
                .filter(chain -> chain != null && chain.getSize() > 0)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(task -> task != null && (this.isPositive(task.getPreviousSpecSwitchHours())
                        || this.isPositive(task.getPreviousGlueSwitchHours())
                        || Boolean.TRUE.equals(task.getPreviousMouthPlateSwitched())))
                .count();
    }

    /**
     * 按已预测胎侧中最终滚动库存非负的比例计算库存保证率。
     *
     * @param context 排程运行上下文
     * @return 库存保证率
     */
    private double calculateStockGuaranteeRate(TcScheduleContext context) {
        if (context.getProductShiftShortageMap().isEmpty()) {
            return 1D;
        }
        long guaranteedCount = context.getProductShiftShortageMap().values().stream()
                .filter(shortageQty -> !this.isPositive(shortageQty))
                .count();
        return (double) guaranteedCount / context.getProductShiftShortageMap().size();
    }

    /**
     * 计算收尾任务完成率(详设§14.11：收尾规格月计划剩余清零占比)。
     *
     * @param context 排程运行上下文
     * @return 收尾任务完成率
     */
    private double calculateTailCompletionRate(TcScheduleContext context) {
        List<TcPlanTaskGroup> tailTaskGroupList = context.getPlanTaskGroupMap().values().stream()
                .filter(taskGroup -> taskGroup != null && taskGroup.getAggregateTask() != null)
                .filter(taskGroup -> "1".equals(taskGroup.getAggregateTask().getTailFlag()))
                .collect(Collectors.toList());
        if (tailTaskGroupList.isEmpty()) {
            return 1D;
        }
        Map<String, BigDecimal> assignedQtyMap = this.buildPlanGroupAssignedQtyMap(context);
        long clearedCount = tailTaskGroupList.stream()
                .filter(taskGroup -> this.isTailSurplusCleared(taskGroup,
                        assignedQtyMap.getOrDefault(taskGroup.getPlanGroupKey(), BigDecimal.ZERO)))
                .count();
        return (double) clearedCount / tailTaskGroupList.size();
    }

    /**
     * 计算单班产能上限命中率。
     *
     * <p>口径对齐详设 §14.11：班次计划量 ≤ 对应机台 MAX_CAPACITY 的占比。
     * 当前以未触发产能溢出(未生成 OVERFLOW_SRC_ 顺延任务)的任务占比近似，
     * 即 1 - 产能溢出率。溢出任务指因剩余产能不足被拆分顺延的任务。</p>
     *
     * @param context 排程运行上下文
     * @return 产能上限命中率
     */
    private double calculateShiftCapacityHitRate(TcScheduleContext context) {
        List<TcMachineCandidate> candidateList = context.getMachineCandidateList().stream()
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
                        1, TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                        .mapToObj(shiftOrder -> assignedQtyMap.getOrDefault(
                                candidate.getMachineCode() + "|" + shiftOrder, BigDecimal.ZERO)
                                .compareTo(this.resolveEffectiveCapacity(
                                        candidate, shiftOrder, switchDeductMap)) <= 0))
                .filter(Boolean.TRUE::equals)
                .count();
        return (double) hitCount / (candidateList.size() * TcScheduleConstants.TC_MAX_SHIFT_ORDER);
    }

    /**
     * 按已排任务需求量/总需求量计算排程覆盖率(详设§14.11 QTY口径)。
     *
     * @param context 排程运行上下文
     * @return 排程覆盖率
     */
    private double calculateCoverageRate(TcScheduleContext context) {
        if (context.getPlanTaskGroupMap().isEmpty()) {
            return 1D;
        }
        Map<String, BigDecimal> assignedQtyMap = this.buildPlanGroupAssignedQtyMap(context);
        BigDecimal totalDemand = context.getPlanTaskGroupMap().values().stream()
                .map(TcPlanTaskGroup::getGroupFinalPlanQty)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDemand.compareTo(BigDecimal.ZERO) <= 0) {
            return 1D;
        }
        BigDecimal plannedDemand = context.getPlanTaskGroupMap().values().stream()
                .map(taskGroup -> assignedQtyMap.getOrDefault(taskGroup.getPlanGroupKey(), BigDecimal.ZERO)
                        .min(this.nvl(taskGroup.getGroupFinalPlanQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return plannedDemand.divide(totalDemand, 6, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 判断收尾任务月计划余量是否已清零：已排且计划量 >= 收尾余量×标准长度(详设§14.11)。
     *
     * @param task 收尾任务
     * @return true 表示月计划余量已清零
     */
    private boolean isTailSurplusCleared(TcPlanTaskGroup taskGroup, BigDecimal assignedQty) {
        TcTaskDraft task = taskGroup == null ? null : taskGroup.getAggregateTask();
        if (task == null) {
            return false;
        }
        if (Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())) {
            BigDecimal closeOutQty = this.nvl(task.getFormingShutdownCloseOutDemandQty());
            BigDecimal stockDeductQty = this.nvl(task.getStockDeductQty());
            BigDecimal requiredPlanQty = closeOutQty.subtract(stockDeductQty).max(BigDecimal.ZERO);
            return this.nvl(assignedQty).compareTo(requiredPlanQty) >= 0;
        }
        BigDecimal tailBalanceQty = task.getTailBalanceQty() == null ? BigDecimal.ZERO : task.getTailBalanceQty();
        BigDecimal sidewallLength = task.getSidewallLength() == null ? BigDecimal.ZERO : task.getSidewallLength();
        BigDecimal tailBaseQty = tailBalanceQty.multiply(sidewallLength);
        if (tailBaseQty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return this.nvl(assignedQty).compareTo(tailBaseQty) >= 0;
    }

    /**
     * 按计划组汇总任务链中的实际已排量。
     *
     * @param context 排程上下文
     * @return key=计划组业务键，value=实际已排量
     */
    private Map<String, BigDecimal> buildPlanGroupAssignedQtyMap(TcScheduleContext context) {
        return context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> task.getPlanGroupKey() != null)
                .collect(Collectors.groupingBy(TcTaskDraft::getPlanGroupKey, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPlanQty()), BigDecimal::add)));
    }

    /**
     * 按机台班次汇总实际已排量。
     *
     * @param context 排程上下文
     * @return key=机台编码|班次，value=实际已排量
     */
    private Map<String, BigDecimal> buildMachineShiftAssignedQtyMap(TcScheduleContext context) {
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
    private Map<String, BigDecimal> buildMachineShiftSwitchDeductMap(TcScheduleContext context) {
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
     * 计算机台班次有效容量，扣除停机和实际换型折算量。
     *
     * @param candidate  候选机台
     * @param shiftOrder 班次顺序
     * @param switchDeductMap 机台班次切换产能扣减
     * @return 有效容量
     */
    private BigDecimal resolveEffectiveCapacity(TcMachineCandidate candidate, int shiftOrder,
                                                Map<String, BigDecimal> switchDeductMap) {
        BigDecimal maxCapacity = this.resolveMachineMaxCapacity(candidate);
        BigDecimal maintenanceHours = this.nvl(candidate.getMaintenanceHoursByShift().get(shiftOrder));
        BigDecimal maintenanceDeduct = maintenanceHours.multiply(this.nvl(candidate.getMachineSpeed()));
        BigDecimal switchDeduct = switchDeductMap.getOrDefault(
                candidate.getMachineCode() + "|" + shiftOrder, BigDecimal.ZERO);
        return maxCapacity.subtract(maintenanceDeduct).subtract(switchDeduct).max(BigDecimal.ZERO);
    }

    /**
     * 读取候选机台最大班产，基础数据无效时使用固定兼容值。
     *
     * @param candidate 候选机台
     * @return 正数最大班产
     */
    private BigDecimal resolveMachineMaxCapacity(TcMachineCandidate candidate) {
        BigDecimal maxCapacity = candidate == null ? null : candidate.getMaxCapacity();
        return this.isPositive(maxCapacity)
                ? maxCapacity : new BigDecimal(TcScheduleConstants.DEFAULT_MACHINE_MAX_CAPACITY);
    }

    /**
     * 判断数值是否为正数。
     *
     * @param value 待判断数值
     * @return true 表示大于零
     */
    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /**
     * 空值转换为零。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
