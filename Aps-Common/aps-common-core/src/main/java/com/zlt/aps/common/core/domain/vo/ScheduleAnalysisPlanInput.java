package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 公共报表计算器使用的单条排程结果输入。
 */
@Data
public class ScheduleAnalysisPlanInput implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编码。
     */
    private String productCode;

    /**
     * 当前结果行物料名称。
     */
    private String productName;

    /**
     * 排程结果记录ID。
     */
    private Long resultId;

    /**
     * CLASS1计划量。
     */
    private BigDecimal class1PlanQty;

    /**
     * CLASS2计划量。
     */
    private BigDecimal class2PlanQty;

    /**
     * CLASS3计划量。
     */
    private BigDecimal class3PlanQty;

    /**
     * CLASS4计划量。
     */
    private BigDecimal class4PlanQty;

    /**
     * CLASS5计划量。
     */
    private BigDecimal class5PlanQty;

    /**
     * CLASS6计划量。
     */
    private BigDecimal class6PlanQty;
}
