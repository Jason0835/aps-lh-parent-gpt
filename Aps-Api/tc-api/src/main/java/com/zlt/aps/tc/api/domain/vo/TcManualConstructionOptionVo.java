package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎侧人工插单施工选项。
 */
@Data
@ApiModel(value = "胎侧人工插单施工选项")
public class TcManualConstructionOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 胎侧编码。 */
    @ApiModelProperty(value = "胎侧编码")
    private String sidewallCode;

    /** 胎侧施工版本。 */
    @ApiModelProperty(value = "胎侧施工版本")
    private String constructionVersion;

    /** 胎侧工艺。 */
    @ApiModelProperty(value = "胎侧工艺")
    private String sidewallCraft;

    /** 胎侧长度。 */
    @ApiModelProperty(value = "胎侧长度")
    private BigDecimal sidewallLength;

    /** 主胶料编码。 */
    @ApiModelProperty(value = "主胶料编码")
    private String glueCode;

    /** 基部胶编码。 */
    @ApiModelProperty(value = "基部胶编码")
    private String baseGlueCode;

    /** 整条胶料组合。 */
    @ApiModelProperty(value = "整条胶料组合")
    private String wholeGlueCode;

    /** 口型板编码。 */
    @ApiModelProperty(value = "口型板编码")
    private String mouthPlateCode;

    /** 胎侧胶重量。 */
    @ApiModelProperty(value = "胎侧胶重量")
    private BigDecimal sidewallWeight;

    /** 胎侧耐磨胶重量。 */
    @ApiModelProperty(value = "胎侧耐磨胶重量")
    private BigDecimal sidewallWearpRubberWeight;
}
