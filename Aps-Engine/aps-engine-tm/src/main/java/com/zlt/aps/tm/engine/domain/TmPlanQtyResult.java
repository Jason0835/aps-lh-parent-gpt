package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面计划量计算结果。
 *
 * <p>用于承载计划量分量和最终计划量，后续解释快照会读取这些字段写入解释表。</p>
 */
@Data
public class TmPlanQtyResult {

    /** 基础需求量 */
    private BigDecimal baseDemandQty;

    /** 库存抵扣量 */
    private BigDecimal stockDeductQty;

    /** 损耗补偿量 */
    private BigDecimal lossAddQty;

    /** 最终计划量 */
    private BigDecimal finalPlanQty;

    /** 计算公式说明 */
    private String calcFormulaDesc;
}
