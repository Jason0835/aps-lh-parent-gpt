package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 特殊大卷上机策略决策结果。 */
@Data
@Builder
public class Cd15SpecialBigRollDecision {

    /** 是否命中特殊大卷规则。 */
    private boolean special;
    /** 命中特殊大卷后是否按上机即耗尽处理。 */
    private boolean consumeAfterMounted;
    /** 后续可扩展使用的额外前瞻成型班次数。 */
    private int lookaheadShifts;
    /** 后续可扩展使用的额外备库上限。 */
    private BigDecimal extraStockLimit;
}
