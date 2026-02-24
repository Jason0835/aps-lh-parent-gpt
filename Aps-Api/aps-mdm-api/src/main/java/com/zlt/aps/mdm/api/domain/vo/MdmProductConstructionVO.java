package com.zlt.aps.mdm.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/3/27
 */
@Data
public class MdmProductConstructionVO implements Serializable {

    private static final long serialVersionUID = -3236028375314344094L;

    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    private String factoryCode;

    @ApiModelProperty(value = "产品编号", name = "productCode")
    private String productCode;

    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;

    @ApiModelProperty(value = "施工关系编码", name = "constructionCode")
    private String constructionCode;

    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    private String embryoCode;

    @ApiModelProperty(value = "生产版本", name = "productionVersion")
    private String productionVersion;

    @ApiModelProperty(value = "成型方法", name = "mouldMethod")
    private String mouldMethod;

    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    private String bomVersion;

    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    private String moldCavity;

    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    private BigDecimal mouldClampingPressure;

    @ApiModelProperty(value = "夏季机械硫化时间(秒)", name = "curingTime")
    private Integer curingTime;

    @ApiModelProperty(value = "夏季液压硫化时间(秒)", name = "hydraulicPressureCuringTime")
    private Integer hydraulicPressureCuringTime;

    @ApiModelProperty(value = "冬季机械硫化时间(秒)", name = "curingTime2")
    private Integer curingTime2;

    @ApiModelProperty(value = "冬季液压硫化时间(秒)", name = "hydraulicPressureCuringTime2")
    private Integer hydraulicPressureCuringTime2;
}

