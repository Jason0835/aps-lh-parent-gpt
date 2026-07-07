package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 施工信息拆解后的斜裁材料层位。
 */
@Data
@Builder
public class Cd15ConstructionMaterial {

    /** 胎胚施工代码。 */
    private String constructionCode;
    /** 胎胚施工版本，对应成型排程 CLASSn_RECIPE_NO。 */
    private String constructionVersion;
    /** 钢带或加强层代码。 */
    private String steelStripCode;
    /** 大卷代码，对应施工 CORD_SPEC。 */
    private String bigRollCode;
    /** 大卷幅宽，对应施工 CORD_WIDTH，入口不做必填拦截。 */
    private BigDecimal cordWidth;
    /** 裁断角度，对应施工 BELT_CUTTING_ANGLE。 */
    private String cuttingAngle;
    /** 层位，1至3为主钢带，101/102表示左右加强层。 */
    private int layerNo;
    /** 是否加强层。 */
    private boolean reinforcement;
    /** 单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 斜裁宽度。 */
    private BigDecimal craftWidth;
    /** 标准卷曲长度，单位米。 */
    private BigDecimal curlLength;
}