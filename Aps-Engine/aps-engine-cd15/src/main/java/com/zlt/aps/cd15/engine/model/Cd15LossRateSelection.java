package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 最终损耗率命中结果。
 */
@Data
@Builder
public class Cd15LossRateSelection {

    /** 最终损耗率百分数。 */
    private BigDecimal lossRatePercent;
    /** 命中层级：STEEL_STRIP_MACHINE、STEEL_STRIP、MACHINE或GENERAL。 */
    private String matchedLevel;
}
