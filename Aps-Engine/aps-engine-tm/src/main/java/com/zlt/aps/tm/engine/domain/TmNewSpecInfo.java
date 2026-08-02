package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 胎面新规格判断与提前排产证据。
 *
 * <p>用于在数据加载阶段记录新规格判断依据，并在计划量计算、机台分配和解释表落库时复用。</p>
 */
@Data
public class TmNewSpecInfo {

    /** 是否为新规格 */
    private Boolean newSpec;

    /** 新规格回看天数 */
    private Integer lookbackDays;

    /** 回看天数参数来源 */
    private String lookbackDaysSource;

    /** 提前生产班次数 */
    private Integer advanceShiftCount;

    /** 提前生产班次数参数来源 */
    private String advanceShiftCountSource;

    /** 新规格扩窗前的库存保证班数 */
    private Integer baseGuardShiftCount;

    /** 新规格扩窗后的有效库存保证班数 */
    private Integer effectiveGuardShiftCount;

    /** 成型需求窗口起始班次 */
    private Integer formingWindowStartClass;

    /** 成型需求窗口结束班次，允许大于成型已加载的 CLASS8 */
    private Integer formingWindowEndClass;

    /** 成型需求窗口中超过 CLASS8、需要估算的班次数 */
    private Integer formingWindowEstimatedShiftCount;

    /** 前一天库存日期 */
    private Date previousStockDate;

    /** 前一天净库存，口径为库存数量减不良数量加调整数量 */
    private BigDecimal previousDayStockQty;

    /** 前一天是否存在有效库存 */
    private Boolean previousDayStockExists;

    /** 历史排程回看开始日期 */
    private Date historyStartDate;

    /** 历史排程回看结束日期 */
    private Date historyEndDate;

    /** 历史排程是否存在计划量 */
    private Boolean historySchedulePlanExists;

    /** 原正常目标班次 */
    private Integer normalTargetShift;

    /** 调整后的目标班次 */
    private Integer adjustedTargetShift;

    /** 调整后的目标排产窗口 */
    private List<Integer> adjustedTargetWindow;

    /** 需求来源班次 */
    private Integer demandShift;

    /** 需求量 */
    private BigDecimal demandQty;

    /** 判断说明 */
    private String reason;

    /**
     * 判断当前证据是否命中新规格。
     *
     * @return true 表示命中新规格提前排产规则
     */
    public boolean isNewSpecHit() {
        return Boolean.TRUE.equals(newSpec);
    }
}
