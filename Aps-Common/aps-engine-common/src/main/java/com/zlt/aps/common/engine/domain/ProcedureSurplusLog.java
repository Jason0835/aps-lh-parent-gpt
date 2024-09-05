package com.zlt.aps.common.engine.domain;

import lombok.Data;

/**
 * 工序计划量汇总日志表
 * @TableName T_PROCEDURE_SURPLUS_LOG
 */
@Data
public class ProcedureSurplusLog extends MonthPlanSurplusBaseEntity{

    /**
     * 工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延
     */
    private String procedureCode;

    /**
     * 数据来源：0>主计划；1>APS插单。主计划更新插单数据不删除
     */
    private String dataSource;

    /**
     * 胎胚月结库存，月结库存获取时更新到该字段
     */
    private Integer lastMonthStock;

    /**
     * 外胎不良数，若不良接口可以提供SAP+胎胚，则接口同步更新该字段,如果给不了由人为输入确认同步更新
     */
    private Integer embryoBadQty;

    private static final long serialVersionUID = 1L;
}