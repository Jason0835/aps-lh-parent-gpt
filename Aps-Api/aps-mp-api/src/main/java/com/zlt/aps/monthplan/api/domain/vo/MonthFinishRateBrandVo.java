package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 */
@ApiModel(value = "报表管理，T月完成率-品牌对象", description = "报表管理，T月完成率-品牌对象")
@Data
public class MonthFinishRateBrandVo implements Serializable {

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
     * 库存sku数量
     */
    @ApiModelProperty(value = "库存sku数量", name = "stockSkuCount")
    private Integer stockSkuCount;

    /**
     * 库存数量
     */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    private Integer stockQty;

    /**
     * 排产sku数
     */
    @ApiModelProperty(value = "排产sku数", name = "produceSkuCount")
    private Integer produceSkuCount;

    /**
     * 生产月度计划数(月度计划量)
     */
    @ApiModelProperty(value = "生产月度计划数(月度计划量)", name = "producePlanQty")
    private Integer producePlanQty;

    /**
     * 生产月度完成数(硫化完成量)
     */
    @ApiModelProperty(value = "生产月度完成数(硫化完成量)", name = "produceFinishPlanQty")
    private Integer produceFinishPlanQty;

    /**
     * 生产月度完成率(%)
     */
    @ApiModelProperty(value = "生产月度完成率，小数", name = "produceFinishRate")
    private BigDecimal produceFinishRate;

    /**
     * 销售需求sku数
     */
    @ApiModelProperty(value = "销售需求sku数", name = "salePlanSkuCount")
    private Integer salePlanSkuCount;

    /**
     * 销售月度计划数(接口提报量)
     */
    @ApiModelProperty(value = "销售月度计划数(接口提报量)", name = "salePlanQty")
    private Integer salePlanQty;

    /**
     * 实际销售sku数
     */
    @ApiModelProperty(value = "实际销售sku数", name = "saleFinishSkuCount")
    private Integer saleFinishSkuCount;

    /**
     * 销售月度完成数(接口完成量)
     */
    @ApiModelProperty(value = "销售月度完成数(接口完成量)", name = "saleFinishPlanQty")
    private Integer saleFinishPlanQty;

    /**
     * 准确率
     */
    @ApiModelProperty(value = "准确率，小数", name = "accuracyRate")
    private BigDecimal accuracyRate;

    /**
     * 排产满足率
     */
    @ApiModelProperty(value = "排产满足率，小数", name = "produceSatisfyRate")
    private BigDecimal produceSatisfyRate;

    /**
     * 完成满足率
     */
    @ApiModelProperty(value = "完成满足率，小数", name = "finishSatisfyRate")
    private BigDecimal finishSatisfyRate;
}
