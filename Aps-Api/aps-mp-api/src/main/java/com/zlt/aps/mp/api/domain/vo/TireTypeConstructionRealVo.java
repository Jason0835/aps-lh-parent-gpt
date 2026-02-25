package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎类区分，施工关系Vo
 *
 * @author Chen
 * @date 2025/3/31
 */
@Data
public class TireTypeConstructionRealVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物料号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;

    /**
     * 规格代码
     */
    @ApiModelProperty(value = "规格代码", name = "specCode")
    private String specCode;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    private String embryoCode;

    /**
     * 施工版本
     */
    @ApiModelProperty(value = "施工版本", name = "bomVersion")
    private String bomVersion;

    /**
     * 胎面代码
     */
    @ApiModelProperty(value = "胎面代码", name = "treadCode")
    private String treadCode;

}
