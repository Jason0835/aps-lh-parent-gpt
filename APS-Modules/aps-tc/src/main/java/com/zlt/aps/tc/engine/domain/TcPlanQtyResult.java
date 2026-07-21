package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧计划量计算结果。
 *
 * <p>用于记录计划量分量和最终计划量，便于解释表落库和单元测试断言。
 * 本对象不直接修改数据库。</p>
 */
@Data
public class TcPlanQtyResult {

    /** 基础应排需求量 */
    private BigDecimal baseDemandQty;

    /** 损耗补偿量 */
    private BigDecimal lossAddQty;

    /** 工装限制调整量 */
    private BigDecimal toolLimitAdjustQty;

    /** 工装限制压掉的待顺延量 */
    private BigDecimal toolOverflowQty;

    /** 最小起排量调整量 */
    private BigDecimal minStartAdjustQty;

    /** 卷数取整调整量 */
    private BigDecimal tailRoundAdjustQty;

    /** 产能压缩调整量 */
    private BigDecimal capacityAdjustQty;

    /** 损耗前计划量 */
    private BigDecimal preLossPlanQty;

    /** 工装限制前计划量 */
    private BigDecimal planQtyBeforeToolLimit;

    /** 最终计划量 */
    private BigDecimal finalPlanQty;

    /** 计算说明 */
    private String calcFormulaDesc;
}
