package com.zlt.aps.monthplan.factory.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 年销量超过配置的需要新增最小批量的对象
 *
 * @author ZLT
 * @date 20250528
 */
@Data
public class YearSaleMinProdVo implements Serializable {
    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;

    /**
     * 规格描述
     */
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    private String productDesc;

    /**
     * 分厂编号
     */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    private String factoryCode;

}
