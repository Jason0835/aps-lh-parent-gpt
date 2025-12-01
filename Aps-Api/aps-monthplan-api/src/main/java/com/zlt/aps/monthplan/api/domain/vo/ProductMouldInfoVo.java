package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * SAP-模具信息对象
 *
 * @author ZLT
 * @date 20250403
 */
@Data
@ApiModel(value = "SAP-模具信息对象", description = "SAP-模具信息对象 ")
public class ProductMouldInfoVo implements Serializable {
    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌：biz_brand_type", name = "brand")
    private String brand;
    /**
     * 模具配置
     */
    @ApiModelProperty(value = "模具配置", name = "mouldConfigurationList")
    private List<ProductMouldConfigurationVo> mouldConfigurationList;
}
