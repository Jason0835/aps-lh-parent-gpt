package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 候选机台损耗率规则。
 */
@Data
@Builder
public class Cd90LossRateRule {

    /** 帘布代码；为空表示不限定帘布。 */
    private String clothCode;
    /** 机台代码；为空表示不限定机台。 */
    private String machineCode;
    /** 损耗率百分数。 */
    private BigDecimal lossRatePercent;
}
