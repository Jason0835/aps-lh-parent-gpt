package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面需求量计算结果。
 *
 * <p>用于承载需求量策略输出，供计划量策略继续计算。该对象不修改任务链。</p>
 */
@Data
public class TmDemandQtyResult {

    /** 最终需求量 */
    private BigDecimal demandQty;

    /** 计算说明 */
    private String calcDesc;
}
