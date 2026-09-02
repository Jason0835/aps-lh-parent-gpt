package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * TM/TC 库存预测数量查询行。
 *
 * <p>需求量和前一班计划量的 SQL 返回结构一致，使用工序产品编码统一承接两类查询结果。</p>
 */
@Data
public class InventoryPredictQtyRow {

    /** 参与排程的工序产品编码。 */
    private String processCode;

    /** 汇总数量，需求量查询表示早班需求量，计划量查询表示早班计划量。 */
    private BigDecimal qty;
}
