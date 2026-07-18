package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 单个胎胚的收尾比较明细。 */
@Data
@Builder
public class Cd15EmbryoCloseOutItem {
    private String embryoCode;
    private BigDecimal calculatedPlanQuantity;
    private BigDecimal planSurplusQuantity;
    private boolean reached;
}
