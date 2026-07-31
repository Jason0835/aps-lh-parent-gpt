package com.zlt.aps.tq.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.domain.TqPersistResult;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈自动排程质量指标汇总服务。
 *
 * <p>Phase 4 重构新增：对齐胎侧 {@code TcScheduleQualitySummaryService}，统一计算覆盖率、未排率、
 * 机台利用率、切换次数、库存保证率、收尾完成率和班产上限命中率，避免不同入口使用不同口径。</p>
 *
 * <p>与胎侧的差异：</p>
 * <ul>
 *   <li>胎侧基于 {@code TcTaskDraft} 任务草稿统计；胎圈基于 {@code TqScheduleResultVo} 排程结果统计</li>
 *   <li>胎侧的 {@code taskChainGroup} 在胎圈对应 {@code taskChainMap}</li>
 *   <li>胎侧的 {@code stockForecastMap} 在胎圈对应 {@code planStockMap}（预计库存）</li>
 *   <li>胎侧的 {@code PARAM_SHIFT_MAX_CAPACITY} 在胎圈对应 {@code TqScheduleParams.maxClassOutput}</li>
 * </ul>
 *
 * @author APS
 */
@Service
public class TqScheduleQualitySummaryService {

    /** 单班次最大产能默认值（与 TqScheduleParams.getMaxClassOutput 兜底值一致） */
    private static final double DEFAULT_SHIFT_MAX_CAPACITY = 3000D;

    /**
     * 构建自动排程质量摘要。
     *
     * @param context      排程运行上下文
     * @param persistResult 落库汇总
     * @return 按固定顺序组织的质量摘要（taskCount/resultCount/unplannedCount/coverageRate/unplannedRate/
     *         machineUtilizationRate/switchCount/stockGuaranteeRate/tailCompletionRate/shiftCapacityHitRate）
     */
    public Map<String, Object> build(TqScheduleContext context, TqPersistResult persistResult) {
        if (context == null) {
            throw new IllegalArgumentException("排程上下文不能为空");
        }
        if (persistResult == null) {
            throw new IllegalArgumentException("落库汇总结果不能为空");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        int taskCount = scheduleList == null ? 0 : scheduleList.size();
        int unplannedCount = persistResult.getUnplannedCount();

        summary.put("taskCount", taskCount);
        summary.put("resultCount", persistResult.getResultCount());
        summary.put("unplannedCount", unplannedCount);
        summary.put("coverageRate", calculateCoverageRate(taskCount, unplannedCount));
        summary.put("unplannedRate", calculateUnplannedRate(taskCount, unplannedCount));
        summary.put("machineUtilizationRate", calculateMachineUtilizationRate(context));
        summary.put("switchCount", calculateSwitchCount(context));
        summary.put("stockGuaranteeRate", calculateStockGuaranteeRate(context));
        summary.put("tailCompletionRate", calculateTailCompletionRate(context));
        summary.put("shiftCapacityHitRate", calculateShiftCapacityHitRate(context));
        return summary;
    }

    /**
     * 计算覆盖率 = (任务总数 - 未排数) / 任务总数。
     *
     * @param taskCount      任务总数
     * @param unplannedCount 未排任务数
     * @return 0 到 1 之间的覆盖率，任务总数为 0 时返回 1.0
     */
    private double calculateCoverageRate(int taskCount, int unplannedCount) {
        if (taskCount == 0) {
            return 1D;
        }
        return (double) (taskCount - unplannedCount) / taskCount;
    }

    /**
     * 计算未排率 = 未排数 / 任务总数。
     *
     * @param taskCount      任务总数
     * @param unplannedCount 未排任务数
     * @return 0 到 1 之间的未排率，任务总数为 0 时返回 0
     */
    private double calculateUnplannedRate(int taskCount, int unplannedCount) {
        if (taskCount == 0) {
            return 0D;
        }
        return (double) unplannedCount / taskCount;
    }

    /**
     * 计算机台利用率 = 已排产量 / (机台数 × 单班最大产能)。
     *
     * <p>口径说明：</p>
     * <ul>
     *   <li>已排产量：6个班次计划量之和</li>
     *   <li>总产能：实际分配机台数 × 单班最大产能 × 6（班次数）</li>
     * </ul>
     *
     * @param context 排程上下文
     * @return 0 到 1 之间的机台利用率
     */
    private double calculateMachineUtilizationRate(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0D;
        }

        // 统计已分配机台数量（去重）
        long machineCount = scheduleList.stream()
                .map(TqScheduleResultVo::getMachineCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .count();
        if (machineCount == 0) {
            return 0D;
        }

        // 统计已排产量（6个班次计划量之和）
        double assignedQty = scheduleList.stream()
                .mapToDouble(this::sumClassPlanQty)
                .sum();

        // 总产能 = 机台数 × 6班次 × 单班最大产能
        double maxClassOutput = resolveMaxClassOutput(context);
        double totalCapacity = machineCount * 6 * maxClassOutput;
        if (totalCapacity <= 0) {
            return 0D;
        }

        return Math.min(assignedQty, totalCapacity) / totalCapacity;
    }

    /**
     * 统计任务链中实际发生规格切换的机台数量。
     *
     * <p>胎圈按机台分配规格，切换次数 = 实际分配多规格的机台数。
     * 即一个机台同时分配了多个胎圈规格时，记为 1 次切换。</p>
     *
     * @param context 排程上下文
     * @return 切换机台数量
     */
    private long calculateSwitchCount(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0L;
        }
        // 按机台分组，统计一个机台分配了多少个不同胎圈规格
        Map<String, Long> machineSpecCountMap = new java.util.HashMap<>();
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            String machineCode = scheduleVo.getMachineCode();
            String beadCode = scheduleVo.getBeadCode();
            if (StringUtils.isNotEmpty(machineCode) && StringUtils.isNotEmpty(beadCode)) {
                machineSpecCountMap.merge(machineCode, 1L, (a, b) -> a + 1L);
            }
        }
        // 切换次数 = 分配了多个规格的机台数
        return machineSpecCountMap.values().stream()
                .filter(count -> count > 1)
                .count();
    }

    /**
     * 计算库存保证率 = 库存满足供应时长的规格数 / 总规格数。
     *
     * <p>口径说明：</p>
     * <ul>
     *   <li>供应时长 ≥ 0 表示库存可保证；供应时长 < 0 表示库存缺口</li>
     *   <li>供应时长为 null 时视为未计算（不计入分母）</li>
     * </ul>
     *
     * @param context 排程上下文
     * @return 0 到 1 之间的库存保证率
     */
    private double calculateStockGuaranteeRate(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 1D;
        }
        // 仅统计有供应时长数据的规格
        List<TqScheduleResultVo> withSupplyTimeList = scheduleList.stream()
                .filter(s -> s.getSupplyTime() != null)
                .collect(java.util.stream.Collectors.toList());
        if (withSupplyTimeList.isEmpty()) {
            return 1D;
        }
        long guaranteedCount = withSupplyTimeList.stream()
                .filter(s -> s.getSupplyTime() != null && s.getSupplyTime() >= 0)
                .count();
        return (double) guaranteedCount / withSupplyTimeList.size();
    }

    /**
     * 计算收尾任务完成率。
     *
     * <p>胎圈收尾规格通过 {@code closeOutSpecFlag} 标记：
     * "0"=收尾规格，"1"=非收尾（可切换下一规格）。</p>
     *
     * @param context 排程上下文
     * @return 0 到 1 之间的收尾完成率
     */
    private double calculateTailCompletionRate(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 1D;
        }
        // 收尾规格：closeOutSpecFlag = "0"
        List<TqScheduleResultVo> tailSpecList = scheduleList.stream()
                .filter(s -> "0".equals(s.getCloseOutSpecFlag()))
                .collect(java.util.stream.Collectors.toList());
        if (tailSpecList.isEmpty()) {
            return 1D;
        }
        // 已完成：6个班次至少有一个班次有计划量
        long completedCount = tailSpecList.stream()
                .filter(s -> sumClassPlanQty(s) > 0)
                .count();
        return (double) completedCount / tailSpecList.size();
    }

    /**
     * 计算单班产能上限命中率。
     *
     * <p>命中产能上限的规格数 = 触发 MACHINE_QUOTA_LIMIT 或 QUOTA_EXCEED_DEFER 规则的规格数。
     * 命中率 = 命中规格数 / 总规格数。</p>
     *
     * @param context 排程上下文
     * @return 0 到 1 之间的产能上限命中率
     */
    private double calculateShiftCapacityHitRate(TqScheduleContext context) {
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0D;
        }
        // 统计触发过定额限制的规格数（从规则证据中提取）
        long capacityLimitedCount = scheduleList.stream()
                .map(TqScheduleResultVo::getBeadCode)
                .filter(beadCode -> hasQuotaLimitEvidence(context, beadCode))
                .distinct()
                .count();
        // 分母 = 去重后的规格数
        long totalSpecCount = scheduleList.stream()
                .map(TqScheduleResultVo::getBeadCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .count();
        if (totalSpecCount == 0) {
            return 0D;
        }
        return (double) capacityLimitedCount / totalSpecCount;
    }

    /**
     * 判断指定胎圈规格是否触发过定额限制规则。
     *
     * @param context  排程上下文
     * @param beadCode 胎圈编码
     * @return true 表示触发过定额限制
     */
    private boolean hasQuotaLimitEvidence(TqScheduleContext context, String beadCode) {
        if (StringUtils.isEmpty(beadCode)) {
            return false;
        }
        com.zlt.aps.tq.engine.domain.TqRuleTrace trace = context.getRuleTraceMap().get(beadCode);
        if (trace == null || trace.getRuleHits() == null || trace.getRuleHits().isEmpty()) {
            return false;
        }
        return trace.getRuleHits().stream().anyMatch(item ->
                com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum.MACHINE_QUOTA_LIMIT.getCode().equals(item.getRuleCode())
                        || com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum.QUOTA_EXCEED_DEFER.getCode().equals(item.getRuleCode())
                        || com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum.QUOTA_CONSTRAINT_ADJUST.getCode().equals(item.getRuleCode()));
    }

    /**
     * 汇总 6 个班次的计划量。
     *
     * @param scheduleVo 排程结果
     * @return 6 个班次计划量之和
     */
    private double sumClassPlanQty(TqScheduleResultVo scheduleVo) {
        double total = 0D;
        if (scheduleVo.getClass1PlanQty() != null) {
            total += scheduleVo.getClass1PlanQty();
        }
        if (scheduleVo.getClass2PlanQty() != null) {
            total += scheduleVo.getClass2PlanQty();
        }
        if (scheduleVo.getClass3PlanQty() != null) {
            total += scheduleVo.getClass3PlanQty();
        }
        if (scheduleVo.getClass4PlanQty() != null) {
            total += scheduleVo.getClass4PlanQty();
        }
        if (scheduleVo.getClass5PlanQty() != null) {
            total += scheduleVo.getClass5PlanQty();
        }
        if (scheduleVo.getClass6PlanQty() != null) {
            total += scheduleVo.getClass6PlanQty();
        }
        return total;
    }

    /**
     * 解析单班次最大可排量。
     *
     * <p>优先使用工序参数 {@code TqScheduleParams.maxClassOutput}，
     * 缺失或非法时使用默认值 {@link #DEFAULT_SHIFT_MAX_CAPACITY}。</p>
     *
     * @param context 排程上下文
     * @return 单班次最大产能
     */
    private double resolveMaxClassOutput(TqScheduleContext context) {
        if (context.getParams() == null || context.getParams().getMaxClassOutput() == null) {
            return DEFAULT_SHIFT_MAX_CAPACITY;
        }
        double value = context.getParams().getMaxClassOutput();
        return value > 0 ? value : DEFAULT_SHIFT_MAX_CAPACITY;
    }

    /**
     * 计算总计划量（来自 TqTotalPlanQtyVo 的6个班次汇总）。
     *
     * <p>辅助方法：供外部调用方查询排程总量，作为质量汇总的补充信息。</p>
     *
     * @param totalPlanQtyVo 总计划量统计
     * @return 总计划量
     */
    public double calculateTotalPlanQty(TqTotalPlanQtyVo totalPlanQtyVo) {
        if (totalPlanQtyVo == null) {
            return 0D;
        }
        double total = 0D;
        if (totalPlanQtyVo.getTotalClass1PlanQty() != null) {
            total += totalPlanQtyVo.getTotalClass1PlanQty();
        }
        if (totalPlanQtyVo.getTotalClass2PlanQty() != null) {
            total += totalPlanQtyVo.getTotalClass2PlanQty();
        }
        if (totalPlanQtyVo.getTotalClass3PlanQty() != null) {
            total += totalPlanQtyVo.getTotalClass3PlanQty();
        }
        if (totalPlanQtyVo.getTotalClass4PlanQty() != null) {
            total += totalPlanQtyVo.getTotalClass4PlanQty();
        }
        if (totalPlanQtyVo.getTotalClass5PlanQty() != null) {
            total += totalPlanQtyVo.getTotalClass5PlanQty();
        }
        if (totalPlanQtyVo.getTotalClass6PlanQty() != null) {
            total += totalPlanQtyVo.getTotalClass6PlanQty();
        }
        return BigDecimal.valueOf(total).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
