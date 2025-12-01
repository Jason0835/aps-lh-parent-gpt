package com.zlt.aps.monthplan.api.domain.vo;

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
public class SpecCodeAndProductCodeVO implements Serializable {

    private static final long serialVersionUID = -3236028375314344094L;


    @ApiModelProperty(value = "产品编号", name = "productCode")
    private String productCode;

    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;

    @ApiModelProperty(value = "施工关系编码", name = "constructionCode")
    private String constructionCode;

    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    private String embryoCode;

    @ApiModelProperty(value = "规格描述", name = "productDesc")
    private String productDesc;


}

