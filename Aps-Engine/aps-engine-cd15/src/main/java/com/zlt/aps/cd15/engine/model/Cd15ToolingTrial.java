package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工装约束纯试算结果。
 */
@Data
@Builder
public class Cd15ToolingTrial {

    /** 可用工装数量。 */
    private int availableToolingCount;
    /** 可用工装总米数。 */
    private BigDecimal availableToolingQuantity;
    /** 本轮工装约束后可排量。 */
    private BigDecimal schedulableQuantity;
    /** 因工装限制暂未排入的数量。 */
    private BigDecimal limitedQuantity;
}
