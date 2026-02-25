package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 物料匹配的模具信息查询条件对象
 *
 * @author ZLT
 * @date 20250403
 */
@Data
@ApiModel(value = "物料匹配的模具信息查询条件对象", description = "物料匹配的模具信息查询条件对象 ")
public class ProductMouldConfigurationParam implements Serializable {
    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;

    /**
     * 分厂编号
     */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;
}
