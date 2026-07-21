package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧库存预测数量查询结果。
 *
 * <p>用于接收库存预测跨表汇总 SQL 的单行结果，避免使用 Map 按字符串字段名取值。</p>
 */
@Data
public class TcInventoryPredictQtyVo {

    /** 胎侧编码 */
    private String sidewallCode;

    /** 汇总数量，需求量查询表示早班需求量，计划量查询表示早班计划量 */
    private BigDecimal qty;
}
