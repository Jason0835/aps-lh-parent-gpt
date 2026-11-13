package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 自动滚动逐班需求窗口累计结果。
 */
@Data
public class AutomaticRollingDemandWindowResult {

    /**
     * 实际逐班累计需求。
     */
    private BigDecimal demandQty;

    /**
     * 所需班次数据完整时为 true。
     */
    private boolean complete;
}
