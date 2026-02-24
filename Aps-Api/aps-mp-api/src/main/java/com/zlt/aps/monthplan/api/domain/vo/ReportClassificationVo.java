package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/19
 */
@Data
public class ReportClassificationVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类字典值，如果是查渠道分类，就是渠道，如果是品牌就是品牌
     * 如果是根据品牌+库位、寸别+渠道，字典值根据|分隔
     */
    @ApiModelProperty(value = "分类字典值，如果是查渠道分类，就是渠道的字典值，如果是品牌就是品牌的字典值" +
            "，如果是根据品牌+库位、寸别+渠道，字典值根据|分隔", name = "classificationValue")
    private String classificationValue;

    /**
     * 分类字典标签值，如果是查渠道分类，就是渠道，如果是品牌就是品牌
     */
    @ApiModelProperty(value = "分类字典标签值，如果是查渠道分类，就是渠道，如果是品牌就是品牌", name = "classificationName")
    private String classificationName;

    /**
     * 分类字典标签值1，如果是查渠道分类，就是渠道，如果是品牌就是品牌
     */
    @ApiModelProperty(value = "分类字典标签值1，如果是根据品牌+库位、寸别+渠道，此值就是库位、渠道", name = "classificationName1")
    private String classificationName1;

    /**
     * 订单sku数量
     */
    @ApiModelProperty(value = "订单sku数量", name = "saleSkuCount")
    private BigDecimal saleSkuCount;

    /**
     * 订单-单sku平均需求量
     */
    @ApiModelProperty(value = "订单-单sku平均需求量", name = "saleAvgSingleSkuQty")
    private BigDecimal saleAvgSingleSkuQty;

    /**
     * 订单数量
     */
    @ApiModelProperty(value = "订单数量", name = "salePlanQty")
    private BigDecimal salePlanQty;

    /**
     * 订单占比
     */
    @ApiModelProperty(value = "订单占比", name = "saleProportion")
    private BigDecimal saleProportion;

    /**
     * 期初库存
     */
    @ApiModelProperty(value = "期初库存", name = "stockQty")
    private BigDecimal stockQty;

    /**
     * 生产sku数量
     */
    @ApiModelProperty(value = "生产sku数量", name = "produceSkuCount")
    private BigDecimal produceSkuCount;

    /**
     * 生产-单sku平均需求量
     */
    @ApiModelProperty(value = "生产-单sku平均需求量", name = "produceAvgSingleSkuQty")
    private BigDecimal produceAvgSingleSkuQty;

    /**
     * 排产数量
     */
    @ApiModelProperty(value = "排产数量", name = "producePlanQty")
    private BigDecimal producePlanQty;

    /**
     * 排产占比
     */
    @ApiModelProperty(value = "排产占比", name = "produceProportion")
    private BigDecimal produceProportion;

    /**
     * 缺口sku数量
     */
    @ApiModelProperty(value = "缺口sku数量", name = "gapSkuCount")
    private BigDecimal gapSkuCount;

    /**
     * 订单缺口数量
     */
    @ApiModelProperty(value = "订单缺口数量", name = "orderGapQty")
    private BigDecimal orderGapQty;

    /**
     * 缺口占比
     */
    @ApiModelProperty(value = "缺口占比", name = "gapProportion")
    private BigDecimal gapProportion;

    /**
     * 规格完成率
     */
    @ApiModelProperty(value = "规格完成率", name = "specFinishRate")
    private BigDecimal specFinishRate;

    /**
     * 计划完成率
     */
    @ApiModelProperty(value = "计划完成率", name = "planFinishRate")
    private BigDecimal planFinishRate;

}
