package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 候选机台损耗率规则。
 */
@Data
@Builder
public class Cd15LossRateRule {

    /** 钢带代码；为空表示不限定钢带。 */
    private String steelStripCode;
    /** 机台代码；为空表示不限定机台。 */
    private String machineCode;
    /** 损耗率百分数。 */
    private BigDecimal lossRatePercent;
}
