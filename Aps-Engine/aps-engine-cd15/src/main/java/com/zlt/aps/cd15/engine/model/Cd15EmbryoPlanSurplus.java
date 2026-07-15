package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** CD15自动排程使用的胎胚月计划剩余量。 */
@Data
@Builder
public class Cd15EmbryoPlanSurplus {

    /** 胎胚代码。 */
    private String embryoCode;
    /** 月计划剩余量。 */
    private BigDecimal planSurplusQuantity;
}
