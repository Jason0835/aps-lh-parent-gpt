package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/7/30
 */
@Data
public class SaleMonthPlanRequireReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SKU个数
     */
    @ApiModelProperty(value = "SKU个数", name = "skuCount")
    private Double skuCount = 0D;

    /**
     * SKU总量
     */
    @ApiModelProperty(value = "SKU总量", name = "skuQty")
    private Double skuQty = 0D;

    /**
     * 净需求量
     */
    @ApiModelProperty(value = "净需求量", name = "netDemandQty")
    private Double netDemandQty = 0D;

    /**
     * 缺口
     */
    @ApiModelProperty(value = "缺口", name = "gapQty")
    private Double gapQty = 0D;

    /**
     * 备库量
     */
    @ApiModelProperty(value = "备库量", name = "stockUpQty")
    private Double stockUpQty = 0D;

    /**
     * 必保SKU个数
     */
    @ApiModelProperty(value = "必保SKU个数", name = "ensurePlanSkuCount")
    private Double ensurePlanSkuCount = 0D;

    /**
     * 必保SKU总量
     */
    @ApiModelProperty(value = "必保SKU总量", name = "ensurePlanSkuQty")
    private Double ensurePlanSkuQty = 0D;

    /**
     * 有交期SKU个数
     */
    @ApiModelProperty(value = "有交期SKU个数", name = "deliveryDateSkuCount")
    private Double deliveryDateSkuCount = 0D;

    /**
     * 有交期SKU总量
     */
    @ApiModelProperty(value = "有交期SKU总量", name = "deliveryDateSkuQty")
    private Double deliveryDateSkuQty = 0D;
}
