package com.zlt.aps.common.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 外胎计划量汇总表日志表
 * @TableName T_CX_MONTH_PLAN_SURPLUS_LOG
 */
@Data
public class CxMonthPlanSurplusLog extends ApsBaseEntity {
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
     * 月度计划量
     */
    private Integer monthPlanQty;

    /**
     * 外胎月结库存，月结库存获取时更新到该字段
     */
    private Integer lastMonthStock;

    /**
     * 外胎不良数，若不良接口可以提供SAP+胎胚，则接口同步更新该字段,如果给不了由人为输入确认同步更新
     */
    private Integer sapBadQty;

    /**
     * 成型计划调整量
     */
    private Integer planModifyQty;

    /**
     * 月度硫化完成量
     */
    private Integer monthFinishQty;

    /**
     * 外胎月剩余量
     */
    private Integer monthRemainQty;

    /**
     * 数据来源：0>主计划；1>APS插单。主计划更新插单数据不删除
     */
    private String dataSource;

    private static final long serialVersionUID = 1L;
}