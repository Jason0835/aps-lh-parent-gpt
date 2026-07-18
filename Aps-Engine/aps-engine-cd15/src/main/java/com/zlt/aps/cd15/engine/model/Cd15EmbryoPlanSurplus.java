package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 自动排程使用的胎胚月计划余量。 */
@Data
@Builder
public class Cd15EmbryoPlanSurplus {
    private String embryoCode;
    private BigDecimal planSurplusQuantity;
}
