package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * TM/TC 需求量计算公共输入模型。
 *
 * <p>产品编码不区分胎面或胎侧，统一使用 {@code processCode} 表示参与排程的工序产品编码。</p>
 */
@Data
public class ScheduleDemandQtyInputModel {

    /** 参与排程的工序产品编码。 */
    protected String processCode;

    /** 当前班成型需求量。 */
    protected BigDecimal currentShiftDemandQty;

    /** 库存最低保证范围需求量。 */
    protected BigDecimal guardDemandQty;

    /** 班初滚动库存量。 */
    protected BigDecimal rollingStockQty;

    /** 库存保证班数。 */
    protected Integer guardShiftCount;

    /** 库存保证范围对应的小时数。 */
    protected BigDecimal guardRangeHours;

    /** 各保证班次的需求量。 */
    protected Map<Integer, BigDecimal> formingGuardWindowQtyMap = new LinkedHashMap<>();

    /** 各保证班次的供应小时数。 */
    protected Map<Integer, BigDecimal> formingGuardWindowHoursMap = new LinkedHashMap<>();
}
