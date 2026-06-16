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
    /** 胎胚施工版本，对应成型排程CLASSn_RECIPE_NO。 */
    private String constructionVersion;
    /** 帘布代码。 */
    private String clothCode;
    /** 大卷代码，对应施工CORD_SPEC。 */
    private String bigRollCode;
    /** 任务规格兼容字段，当前固定等于clothCode，后续调用链统一改用clothCode。 */
    private String cordSpec;
    /** 施工层位，取1至3。 */
    private int layerNo;
    /** 帘布单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 大卷幅宽，同时作为单片直裁长度。 */
    private BigDecimal cordWidth;
    /** 当前层位单片直裁宽度。 */
    private BigDecimal craftWidth;
    /** 当前层位直裁宽度原始值，用于解析失败时复盘。 */
    private String craftWidthRaw;
}
