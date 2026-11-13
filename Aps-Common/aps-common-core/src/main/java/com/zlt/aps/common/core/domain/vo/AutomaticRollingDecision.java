package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 自动滚动阈值公式的计算结果及审计输入。
 */
@Data
public class AutomaticRollingDecision {

    /**
     * 调整方向：UP、DOWN 或 NONE。
     */
    private String direction;

    /**
     * 上修阈值对应的未来累计需求。
     */
    private BigDecimal upDemandQty;

    /**
     * 下修阈值对应的未来累计需求。
     */
    private BigDecimal downDemandQty;

    /**
     * 下修目标班数对应的未来累计需求。
     */
    private BigDecimal downTargetDemandQty;

    /**
     * 公式目标计划量，尚未应用计算侧完成量下限；双侧齐全时计算侧为主侧。
     */
    private BigDecimal formulaPlanQty;

    /**
     * 应用非负及计算侧完成量下限后的最终计划量。
     */
    private BigDecimal targetPlanQty;
}
