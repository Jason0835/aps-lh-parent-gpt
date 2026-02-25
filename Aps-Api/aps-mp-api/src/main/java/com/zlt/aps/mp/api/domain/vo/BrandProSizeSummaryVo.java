package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/17
 */
@Data
public class BrandProSizeSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 品牌名称
     */
    @ApiModelProperty(value = "品牌名称", name = "brandName")
    private String brandName;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private String proSize;

    /**
     * 期初库存
     */
    @ApiModelProperty(value = "期初库存", name = "stockQty")
    private BigDecimal stockQty;

    /**
     * 销量预测(销售计划)
     */
    @ApiModelProperty(value = "销量预测(销售计划)", name = "salePlanQty")
    private BigDecimal salePlanQty;

    /**
     * 累计销量(销售完成)
     */
    @ApiModelProperty(value = "累计销量(销售完成)", name = "saleFinishQty")
    private BigDecimal saleFinishQty;

    /**
     * 排产情况(生产计划)
     */
    @ApiModelProperty(value = "排产情况(生产计划)", name = "proPlanQty")
    private BigDecimal proPlanQty;

    /**
     * 累计产量(生产完成)
     */
    @ApiModelProperty(value = "累计产量(生产完成)", name = "proFinishQty")
    private BigDecimal proFinishQty;

    /**
     * 排产完成率
     */
    @ApiModelProperty(value = "排产完成率", name = "proFinishRate")
    private BigDecimal proFinishRate;

    /**
     * 订单完成率
     */
    @ApiModelProperty(value = "订单完成率", name = "saleFinishRate")
    private BigDecimal saleFinishRate;

    /**
     * 次月期初理论库存
     */
    @ApiModelProperty(value = "次月期初理论库存", name = "nextMonthStock")
    private BigDecimal nextMonthStock;

    /**
     * 期初库存SKU个数
     */
    @ApiModelProperty(value = "期初库存SKU个数", name = "stockCount")
    private Long stockCount;

    /**
     * 销量预测(销售计划)SKU个数
     */
    @ApiModelProperty(value = "销量预测(销售计划)SKU个数", name = "salePlanCount")
    private Long salePlanCount;

    /**
     * 累计销量(销售完成)SKU个数
     */
    @ApiModelProperty(value = "累计销量(销售完成)SKU个数", name = "saleFinishCount")
    private Long saleFinishCount;

    /**
     * 排产情况(生产计划)SKU个数
     */
    @ApiModelProperty(value = "排产情况(生产计划)SKU个数", name = "proPlanCount")
    private Long proPlanCount;

    /**
     * 累计产量(生产完成)SKU个数
     */
    @ApiModelProperty(value = "累计产量(生产完成)SKU个数", name = "proFinishCount")
    private Long proFinishCount;

    /**
     * 排产完成率(SKU个数)
     */
    @ApiModelProperty(value = "排产完成率", name = "proFinishCountRate")
    private BigDecimal proFinishCountRate;

    /**
     * 订单完成率(SKU个数)
     */
    @ApiModelProperty(value = "订单完成率", name = "saleFinishCountRate")
    private BigDecimal saleFinishCountRate;

}
