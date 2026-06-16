package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面需求量计算结果。
 *
 * <p>用于返回当前班需求、库存保证缺口和最终基础应排需求。
 * 该对象只做结构化返回，不修改任务链。</p>
 */
@Data
public class TmDemandQtyResult {

    /** 当前班成型胎面需求量，单位米 */
    private BigDecimal currentShiftDemandQty;

    /** 保证范围内成型胎面需求量，单位米 */
    private BigDecimal guardDemandQty;

    /** 当前班开始滚动库存，单位米 */
    private BigDecimal rollingStockQty;

    /** 库存保证缺口，单位米 */
    private BigDecimal stockGapQty;

    /** 基础应排需求量，单位米 */
    private BigDecimal demandQty;

    /** 库存最低保证班数 */
    private Integer guardShiftCount;

    /** 供应时长，单位小时；未来需求为 0 时为空 */
    private BigDecimal supplyHours;

    /** 计算说明 */
    private String calcDesc;
}
