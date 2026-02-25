package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 物料配置模具信息-参数对象
 *
 * @author ZLT
 * @date 20250912
 */
@Data
@ApiModel(value = "物料配置模具信息-参数对象", description = "物料配置模具信息-参数对象")
public class ProductMouldRelationConfigurationParam {
    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;
    /**
     * 物料编码-SAP代码
     */
    @ApiModelProperty(value = "物料编码", name = "productCode")
    private String productCode;
    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;
    /**
     * 模具号
     */
    @ApiModelProperty(value = "模具号", name = "mouldNo")
    private String mouldNo;
    /**
     * 模具数量
     */
    @ApiModelProperty(value = "模具数量", name = "mouldNumber")
    private Integer mouldNumber;

}
