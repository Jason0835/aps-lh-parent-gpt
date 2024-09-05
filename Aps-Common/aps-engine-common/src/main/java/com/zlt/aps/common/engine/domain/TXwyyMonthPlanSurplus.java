package com.zlt.aps.common.engine.domain;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 纤维压延工序计划量汇总表
 * @TableName T_XWYY_MONTH_PLAN_SURPLUS
 */
@Data
public class TXwyyMonthPlanSurplus extends MonthPlanSurplusBaseEntity {
//    /**
//     * 主键ID
//     */
//    private Long id;
//
//    /**
//     * 生产排程记录主计划版本号,年+月+日+01，02
//     */
//    private String monthPlanApsVersion;
//
//    /**
//     * 主计划版本号
//     */
//    private String monthPlanVersion;
//
//    /**
//     * 主计划所属年份
//     */
//    private String year;
//
//    /**
//     * 主计划所属月份
//     */
//    private String month;
//
//    /**
//     * 物料编码
//     */
//    private String materialCode;
//
//    /**
//     * 月度计划量
//     */
//    private BigDecimal monthPlanQty;
//
//    /**
//     * 月度计划调整量
//     */
//    private BigDecimal monthPlanModifyQty;
//
//    /**
//     * 月度完成量
//     */
//    private BigDecimal monthFinishQty;
//
//    /**
//     * 月剩余量
//     */
//    private BigDecimal monthRemainQty;

    /**
     * 月度计划量（个）
     */
    private BigDecimal monthPlanQty2 = BigDecimal.ZERO;

    /**
     * 月度计划调整量（个）
     */
    private BigDecimal monthPlanModifyQty2 = BigDecimal.ZERO;

    /**
     * 月度完成量（个）
     */
    private BigDecimal monthFinishQty2 = BigDecimal.ZERO;

    /**
     * 月剩余量（个）
     */
    private BigDecimal monthRemainQty2 = BigDecimal.ZERO;

    private static final long serialVersionUID = 1L;
}