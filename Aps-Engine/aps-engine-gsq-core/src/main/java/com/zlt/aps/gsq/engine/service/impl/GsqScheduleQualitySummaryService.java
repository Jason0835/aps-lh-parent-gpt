package com.zlt.aps.gsq.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.domain.GsqRuleTrace;
import com.zlt.aps.gsq.engine.domain.GsqRuleTraceItem;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 钢丝圈自动排程质量指标汇总服务。
 *
 * <p>Phase 4 重构新增：对齐胎圈 {@code TqScheduleQualitySummaryService}，统一计算覆盖率、未排率、
 * 机台利用率、切换次数、库存保证率、收尾完成率和班产上限命中率，避免不同入口使用不同口径。</p>
 *
 * <p>与胎圈的差异：</p>
 * <ul>
 *   <li>胎圈基于 {@code TqScheduleResultVo}，钢丝圈同样基于 {@code GsqScheduleResultVo}</li>
 *   <li>胎圈使用 {@code TqScheduleParams.maxClassOutput} 作为单班产能上限；钢丝圈使用
 *       {@code GsqScheduleParams.wrappingMachineQuota}（包布机单机班产上限，默认1500）</li>
 *   <li>钢丝圈6个班次可能分配不同机台，机台利用率按实际去重机台计算</li>
 *   <li>钢丝圈有收尾规格标记 {@code closeOutSpecFlag} 和保鲜期超期标记 {@code freshExpiredFlag}</li>
 * </ul>
 *
 * @author APS
 */
@Service
public class GsqScheduleQualitySummaryService {

    /** 单班次最大产能默认值（与 GsqScheduleParams.getWrappingMachineQuota 兜底值一致） */
    private static final double DEFAULT_SHIFT_MAX_CAPACITY = 1500D;

    /**
     * 构建自动排程质量摘要。
     *
     * @param context 排程运行上下文
     * @return 按固定顺序组织的质量摘要（taskCount/resultCount/unplannedCount/coverageRate/unplannedRate/
     *         machineUtilizationRate/switchCount/stockGuaranteeRate/tailCompletionRate/shiftCapacityHitRate）
     */
    public Map<String, Object> build(GsqScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("排程上下文不能为空");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        int taskCount = scheduleList == null ? 0 : scheduleList.size();
        int unplannedCount = countUnplanned(scheduleList);

        summary.put("taskCount", taskCount);
        summary.put("resultCount", taskCount - unplannedCount);
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
     * 统计未排任务数（6个班次均无计划量的规格数）。
     */
    private int countUnplanned(List<GsqScheduleResultVo> scheduleList) {
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0;
        }
        return (int) scheduleList.stream()
                .filter(this::isAllClassPlanEmpty)
                .count();
    }

    /**
     * 判断6个班次计划量是否全部为空或0。
     */
    private boolean isAllClassPlanEmpty(GsqScheduleResultVo vo) {
        return isPlanEmpty(vo.getClass1PlanQty())
                && isPlanEmpty(vo.getClass2PlanQty())
                && isPlanEmpty(vo.getClass3PlanQty())
                && isPlanEmpty(vo.getClass4PlanQty())
                && isPlanEmpty(vo.getClass5PlanQty())
                && isPlanEmpty(vo.getClass6PlanQty());
    }

    private boolean isPlanEmpty(Double planQty) {
        return planQty == null || planQty <= 0;
    }

    /**
     * 计算覆盖率 = (任务总数 - 未排数) / 任务总数。
     */
    private double calculateCoverageRate(int taskCount, int unplannedCount) {
        if (taskCount == 0) {
            return 1D;
        }
        return (double) (taskCount - unplannedCount) / taskCount;
    }

    /**
     * 计算未排率 = 未排数 / 任务总数。
     */
    private double calculateUnplannedRate(int taskCount, int unplannedCount) {
        if (taskCount == 0) {
            return 0D;
        }
        return (double) unplannedCount / taskCount;
    }

    /**
     * 计算机台利用率 = 已排产量 / (机台数 × 单班最大产能 × 6班次)。
     *
     * <p>口径说明：</p>
     * <ul>
     *   <li>已排产量：6个班次计划量之和</li>
     *   <li>总产能：实际分配机台数（6班次去重） × 单班最大产能 × 6（班次数）</li>
     * </ul>
     */
    private double calculateMachineUtilizationRate(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0D;
        }

        // 统计已分配机台数量（6个班次去重）
        long machineCount = collectAllMachineCodes(scheduleList).size();
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
     * 收集所有已分配的机台编码（6个班次去重）。
     */
    private java.util.Set<String> collectAllMachineCodes(List<GsqScheduleResultVo> scheduleList) {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (GsqScheduleResultVo vo : scheduleList) {
            addIfNotEmpty(codes, vo.getClass1MachineCode());
            addIfNotEmpty(codes, vo.getClass2MachineCode());
            addIfNotEmpty(codes, vo.getClass3MachineCode());
            addIfNotEmpty(codes, vo.getClass4MachineCode());
            addIfNotEmpty(codes, vo.getClass5MachineCode());
            addIfNotEmpty(codes, vo.getClass6MachineCode());
        }
        return codes;
    }

    private void addIfNotEmpty(java.util.Set<String> set, String value) {
        if (StringUtils.isNotEmpty(value)) {
            set.add(value);
        }
    }

    /**
     * 统计任务链中实际发生规格切换的机台数量。
     *
     * <p>钢丝圈按机台分配规格，切换次数 = 实际分配多规格的机台数。
     * 即一个机台同时分配了多个钢丝圈规格时，记为 1 次切换。</p>
     */
    private long calculateSwitchCount(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0L;
        }
        // 按机台分组，统计一个机台分配了多少个不同钢丝圈规格
        Map<String, Long> machineSpecCountMap = new HashMap<>();
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            // 钢丝圈6个班次可能分配不同机台，遍历6个班次的机台编码
            String steelRingCode = scheduleVo.getSteelRingCode();
            java.util.Set<String> machineCodes = new java.util.HashSet<>();
            addIfNotEmpty(machineCodes, scheduleVo.getClass1MachineCode());
            addIfNotEmpty(machineCodes, scheduleVo.getClass2MachineCode());
            addIfNotEmpty(machineCodes, scheduleVo.getClass3MachineCode());
            addIfNotEmpty(machineCodes, scheduleVo.getClass4MachineCode());
            addIfNotEmpty(machineCodes, scheduleVo.getClass5MachineCode());
            addIfNotEmpty(machineCodes, scheduleVo.getClass6MachineCode());
            for (String machineCode : machineCodes) {
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
     */
    private double calculateStockGuaranteeRate(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 1D;
        }
        // 仅统计有供应时长数据的规格
        List<GsqScheduleResultVo> withSupplyTimeList = scheduleList.stream()
                .filter(s -> s.getSupplyTime() != null)
                .collect(Collectors.toList());
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
     * <p>钢丝圈收尾规格通过 {@code closeOutSpecFlag} 标记：
     * "0"=收尾规格，"1"=非收尾（可切换下一规格）。</p>
     */
    private double calculateTailCompletionRate(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 1D;
        }
        // 收尾规格：closeOutSpecFlag = "0"
        List<GsqScheduleResultVo> tailSpecList = scheduleList.stream()
                .filter(s -> "0".equals(s.getCloseOutSpecFlag()))
                .collect(Collectors.toList());
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
     */
    private double calculateShiftCapacityHitRate(GsqScheduleContext context) {
        List<GsqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return 0D;
        }
        // 统计触发过定额限制的规格数（从规则证据中提取）
        long capacityLimitedCount = scheduleList.stream()
                .map(GsqScheduleResultVo::getSteelRingCode)
                .filter(steelRingCode -> hasQuotaLimitEvidence(context, steelRingCode))
                .distinct()
                .count();
        // 分母 = 去重后的规格数
        long totalSpecCount = scheduleList.stream()
                .map(GsqScheduleResultVo::getSteelRingCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .count();
        if (totalSpecCount == 0) {
            return 0D;
        }
        return (double) capacityLimitedCount / totalSpecCount;
    }

    /**
     * 判断指定钢丝圈规格是否触发过定额限制规则。
     */
    private boolean hasQuotaLimitEvidence(GsqScheduleContext context, String steelRingCode) {
        if (StringUtils.isEmpty(steelRingCode)) {
            return false;
        }
        GsqRuleTrace trace = context.getRuleTraceMap().get(steelRingCode);
        if (trace == null || trace.getRuleHits() == null || trace.getRuleHits().isEmpty()) {
            return false;
        }
        return trace.getRuleHits().stream().anyMatch(item ->
                GsqScheduleRuleCodeEnum.MACHINE_QUOTA_LIMIT.getCode().equals(item.getRuleCode())
                        || GsqScheduleRuleCodeEnum.QUOTA_EXCEED_DEFER.getCode().equals(item.getRuleCode())
                        || GsqScheduleRuleCodeEnum.QUOTA_CONSTRAINT_ADJUST.getCode().equals(item.getRuleCode()));
    }

    /**
     * 汇总 6 个班次的计划量。
     */
    private double sumClassPlanQty(GsqScheduleResultVo vo) {
        double total = 0D;
        if (vo.getClass1PlanQty() != null) total += vo.getClass1PlanQty();
        if (vo.getClass2PlanQty() != null) total += vo.getClass2PlanQty();
        if (vo.getClass3PlanQty() != null) total += vo.getClass3PlanQty();
        if (vo.getClass4PlanQty() != null) total += vo.getClass4PlanQty();
        if (vo.getClass5PlanQty() != null) total += vo.getClass5PlanQty();
        if (vo.getClass6PlanQty() != null) total += vo.getClass6PlanQty();
        return total;
    }

    /**
     * 解析单班次最大可排量。
     *
     * <p>优先使用工序参数 {@code GsqScheduleParams.wrappingMachineQuota}（包布机单机班产上限），
     * 缺失或非法时使用默认值 {@link #DEFAULT_SHIFT_MAX_CAPACITY}。</p>
     */
    private double resolveMaxClassOutput(GsqScheduleContext context) {
        GsqScheduleParams params = context.getParams();
        if (params == null || params.getWrappingMachineQuota() == null) {
            return DEFAULT_SHIFT_MAX_CAPACITY;
        }
        double value = params.getWrappingMachineQuota();
        return value > 0 ? value : DEFAULT_SHIFT_MAX_CAPACITY;
    }

    /**
     * 计算总计划量（来自 GsqTotalPlanQtyVo 的6个班次汇总）。
     *
     * <p>辅助方法：供外部调用方查询排程总量，作为质量汇总的补充信息。</p>
     */
    public double calculateTotalPlanQty(GsqTotalPlanQtyVo totalPlanQtyVo) {
        if (totalPlanQtyVo == null) {
            return 0D;
        }
        double total = 0D;
        if (totalPlanQtyVo.getTotalClass1PlanQty() != null) total += totalPlanQtyVo.getTotalClass1PlanQty();
        if (totalPlanQtyVo.getTotalClass2PlanQty() != null) total += totalPlanQtyVo.getTotalClass2PlanQty();
        if (totalPlanQtyVo.getTotalClass3PlanQty() != null) total += totalPlanQtyVo.getTotalClass3PlanQty();
        if (totalPlanQtyVo.getTotalClass4PlanQty() != null) total += totalPlanQtyVo.getTotalClass4PlanQty();
        if (totalPlanQtyVo.getTotalClass5PlanQty() != null) total += totalPlanQtyVo.getTotalClass5PlanQty();
        if (totalPlanQtyVo.getTotalClass6PlanQty() != null) total += totalPlanQtyVo.getTotalClass6PlanQty();
        return BigDecimal.valueOf(total).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
