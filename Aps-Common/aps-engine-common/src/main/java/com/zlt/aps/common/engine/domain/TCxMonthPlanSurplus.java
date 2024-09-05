package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成型工序外胎计划量汇总表
 * @TableName T_CX_MONTH_PLAN_SURPLUS
 */
@Data
public class TCxMonthPlanSurplus extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 生产排程记录主计划版本号,年+月+日+01，02
     */
    private String monthPlanApsVersion;

    /**
     * 月度计划版本
     */
    private String monthPlanVersion;

    /**
     * 月度计划所属年份
     */
    private String year;

    /**
     * 月度计划所属月份
     */
    private String month;

    /**
     * SAP品号
     */
    private String sapCode;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 月度计划量
     */
    private Integer monthPlanQty = 0;

    /**
     * 成型计划调整量
     */
    private Integer planModifyQty = 0;

    /**
     * 月结库存量
     */
    private Integer lastMonthStock = 0;

    /**
     * 不良量
     */
    private Integer sapBadQty = 0;

    /**
     * 月度胎胚完成量
     */
    private Integer monthFinishQty = 0;

    /**
     * 成型月剩余量
     */
    private Integer monthRemainQty = 0;

    /**
     * 数据来源 0：主计划 ；1APS插单
     */
    private Integer dataSource;

    private static final long serialVersionUID = 1L;

}