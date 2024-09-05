package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成型工序胎胚计划量汇总表
 * @TableName T_CX_EMBRYO_MONTH_PLAN_SURPLUS
 */
@Data
public class TCxEmbryoMonthPlanSurplus extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 生产排程记录主计划版本号,年+月+日+01，02
     */
    private String monthPlanApsVersion;

    /**
     * 主计划版本号
     */
    private String monthPlanVersion;

    /**
     * 主计划所属年份
     */
    private String year;

    /**
     * 主计划所属月份
     */
    private String month;

    /**
     * 物料编码(成型胎胚代码)
     */
    private String materialCode;

    /**
     * 月度计划量
     */
    private BigDecimal monthPlanQty = BigDecimal.ZERO;

    /**
     * 月度计划调整量
     */
    private BigDecimal monthPlanModifyQty = BigDecimal.ZERO;

    /**
     * 月结库存量
     */
    private BigDecimal lastMonthStock = BigDecimal.ZERO;

    /**
     * 不良量
     */
    private BigDecimal embryoBadQty = BigDecimal.ZERO;

    /**
     * 月度完成量
     */
    private BigDecimal monthFinishQty = BigDecimal.ZERO;

    /**
     * 月剩余量
     */
    private BigDecimal monthRemainQty = BigDecimal.ZERO;

    /**
     * 数据来源 0：主计划 ；1APS插单
     */
    private Integer dataSource;

    /**
     * 施工信息版本
     */
    private transient String bomDataVersion;

    private static final long serialVersionUID = 1L;
}