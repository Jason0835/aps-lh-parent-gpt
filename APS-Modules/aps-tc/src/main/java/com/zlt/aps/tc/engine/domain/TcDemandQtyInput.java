package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎侧需求量计算输入。
 *
 * <p>用于传入当前班需求、保证范围需求和 当前班初滚动库存。
 * 本对象不读取数据库，不修改任务链。</p>
 */
@Data
public class TcDemandQtyInput {

    /** 胎侧规格编码 */
    private String sidewallCode;

    /** 当前班成型胎侧需求量，单位米 */
    private BigDecimal currentShiftDemandQty;

    /** 保证范围内成型胎侧需求量，单位米 */
    private BigDecimal guardDemandQty;

    /** 当前班开始滚动库存，单位米 */
    private BigDecimal rollingStockQty;

    /** 库存最低保证班数 */
    private Integer guardShiftCount;

    /** 保证范围总小时数 */
    private BigDecimal guardRangeHours;

    /** 保证窗口逐班成型需求量。 */
    private Map<Integer, BigDecimal> formingGuardWindowQtyMap = new LinkedHashMap<>();

    /** 保证窗口逐班实际班次时长。 */
    private Map<Integer, BigDecimal> formingGuardWindowHoursMap = new LinkedHashMap<>();
}
