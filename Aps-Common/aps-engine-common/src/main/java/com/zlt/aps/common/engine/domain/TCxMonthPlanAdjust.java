package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成型计划修正表 2021-11-03添加修正表，用于解决同外胎、同胎胚汇总计算问题
 * @TableName T_CX_MONTH_PLAN_ADJUST
 */
@Data
public class TCxMonthPlanAdjust extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 生产排程记录主计划版本号,年+月+日+01，02
     */
    private String monthPlanApsVersion;

    /**
     * SAP品号
     */
    private String sapCode;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 成型计划调整量
     */
    private Integer planModifyQty;

    /**
     * 调整源头：0：投产列表；1：成型排程
     */
    private String adjustSource;

    /**
     * 施工信息版本
     */
    private String bomDataVersion;

    private static final long serialVersionUID = 1L;
}