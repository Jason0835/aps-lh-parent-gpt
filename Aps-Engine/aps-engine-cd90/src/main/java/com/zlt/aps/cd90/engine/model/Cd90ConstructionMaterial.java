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
    /** 单片直裁长度/胎体长度，单位毫米/条，对应TIRE_FABRIC_LENGTH1/2/3。 */
    private BigDecimal unitConsumeMillimeter;
    /** 大卷幅宽，保留给大卷面积换算等其他口径使用。 */
    private BigDecimal cordWidth;
    /** 当前层位单片直裁宽度，对应TIRE_FABRIC_CRAFT1/2/3。 */
    private BigDecimal craftWidth;
    /** 当前层位直裁宽度原始值，用于解析失败时复盘。 */
    private String craftWidthRaw;
    /** 标准卷曲长度，单位米；优先取t_cd90_curl_length.CURL_LENGTH，缺失时取参数CRIMP_LENGTH。 */
    private BigDecimal curlLength;
}
