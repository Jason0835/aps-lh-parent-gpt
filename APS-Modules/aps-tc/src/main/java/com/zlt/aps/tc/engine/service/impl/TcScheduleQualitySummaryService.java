package com.zlt.aps.tc.engine.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.engine.domain.TcMachineCandidate;
import com.zlt.aps.tc.engine.domain.TcPersistResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        int taskCount = context.getTaskDraftList().size();
        int unplannedCount = persistResult.getUnplannedCount();
        summary.put("taskCount", taskCount);
        summary.put("resultCount", persistResult.getResultCount());
        summary.put("unplannedCount", unplannedCount);
        summary.put("coverageRate", taskCount == 0 ? 1D : (double) (taskCount - unplannedCount) / taskCount);
        summary.put("unplannedRate", taskCount == 0 ? 0D : (double) unplannedCount / taskCount);
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
        List<ScheduleTaskLinkedList<TcTaskDraft>> chainList = context.getTaskChainGroup().values().stream()
                .filter(chain -> chain != null && chain.getSize() > 0)
                .collect(Collectors.toList());
        if (chainList.isEmpty()) {
            return 0D;
        }
        BigDecimal assignedQty = chainList.stream()
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getPlanQty)
                .filter(this::isPositive)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> machineCapacityMap = context.getMachineCandidateList().stream()
                .filter(candidate -> candidate != null && candidate.getMachineCode() != null)
                .collect(Collectors.toMap(TcMachineCandidate::getMachineCode,
                        this::resolveMachineMaxCapacity, (existing, replacement) -> existing));
        BigDecimal defaultCapacity = new BigDecimal(TcScheduleConstants.DEFAULT_MACHINE_MAX_CAPACITY);
        BigDecimal totalCapacity = chainList.stream()
                .map(chain -> chain.toList().stream()
                        .map(ScheduleTaskNode::getTask)
                        .filter(task -> task != null && task.getMachineCode() != null)
                        .findFirst().orElse(null))
                .filter(task -> task != null)
                .map(task -> machineCapacityMap.getOrDefault(task.getMachineCode(), defaultCapacity))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!this.isPositive(totalCapacity)) {
            return 0D;
        }
        return assignedQty.min(totalCapacity).divide(totalCapacity, 6, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 统计任务链中实际发生规格或胶料切换的任务数量。
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
                        || this.isPositive(task.getPreviousGlueSwitchHours())))
                .count();
    }

    /**
     * 按已预测胎侧中最终滚动库存非负的比例计算库存保证率。
     *
     * @param context 排程运行上下文
     * @return 库存保证率
     */
    private double calculateStockGuaranteeRate(TcScheduleContext context) {
        if (context.getStockForecastMap().isEmpty()) {
            return 1D;
        }
        long guaranteedCount = context.getStockForecastMap().keySet().stream()
                .map(context.getRemainingStockMap()::get)
                .filter(stock -> stock != null && stock.signum() >= 0)
                .count();
        return (double) guaranteedCount / context.getStockForecastMap().size();
    }

    /**
     * 计算收尾任务完成率。
     *
     * @param context 排程运行上下文
     * @return 收尾任务完成率
     */
    private double calculateTailCompletionRate(TcScheduleContext context) {
        List<TcTaskDraft> tailTaskList = context.getTaskDraftList().stream()
                .filter(task -> "1".equals(task.getTailFlag()))
                .collect(Collectors.toList());
        if (tailTaskList.isEmpty()) {
            return 1D;
        }
        long assignedCount = tailTaskList.stream().filter(task -> !task.isUnassigned()).count();
        return (double) assignedCount / tailTaskList.size();
    }

    /**
     * 计算单班产能上限命中率。
     *
     * @param context 排程运行上下文
     * @return 产能上限命中率
     */
    private double calculateShiftCapacityHitRate(TcScheduleContext context) {
        if (context.getTaskDraftList().isEmpty()) {
            return 0D;
        }
        long capacityLimitedCount = context.getTaskDraftList().stream()
                .map(TcTaskDraft::getBusinessKeySuffix)
                .filter(suffix -> suffix != null
                        && suffix.startsWith(TcScheduleConstants.CAPACITY_OVERFLOW_BUSINESS_KEY_PREFIX))
                .count();
        return (double) capacityLimitedCount / context.getTaskDraftList().size();
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
}
