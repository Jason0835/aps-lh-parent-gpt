package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 施工信息中的单层帘布代码及单耗。
 */
@Data
@Builder
public class Cd90ConstructionMaterial {

    /** 胎胚施工代码。 */
    private String constructionCode;
    /** 帘布代码。 */
    private String clothCode;
    /** 施工层位，取1至3。 */
    private int layerNo;
    /** 帘布单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
}
