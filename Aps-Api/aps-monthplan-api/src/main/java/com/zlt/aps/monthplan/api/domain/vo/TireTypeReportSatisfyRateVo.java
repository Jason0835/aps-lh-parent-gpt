package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排产受限影响满足率Vo对象
 *
 * @author Chen
 * @date 2025/3/24
 */
@Data
public class TireTypeReportSatisfyRateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    private String productDesc;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /**
     * 品牌，字典：biz_brand_type
     */
    @ApiModelProperty(value = "品牌，字典：biz_brand_type", name = "brand")
    private String brand;

    /**
     * 轮胎类型，字典：TIRE_TYPE
     */
    @ApiModelProperty(value = "轮胎类型，字典：TIRE_TYPE", name = "tireType")
    private String tireType;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /**
     * 库位
     */
    @ApiModelProperty(value = "库位", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 分类名称
     */
    @ApiModelProperty(value = "分类名称", name = "typeName")
    private String typeName;

    /**
     * 胶种
     */
    @ApiModelProperty(value = "胶种", name = "glue")
    private String glue;

    /**
     * 销售sku数量
     */
    @ApiModelProperty(value = "销售sku数量", name = "saleSkuCount")
    private BigDecimal saleSkuCount;

    /**
     * 销售需求计划量
     */
    @ApiModelProperty(value = "销售需求计划量", name = "salePlanQty")
    private BigDecimal salePlanQty;

    /**
     * 库存sku数量
     */
    @ApiModelProperty(value = "库存sku数量", name = "stockSkuCount")
    private BigDecimal stockSkuCount;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", name = "stockQty")
    private BigDecimal stockQty;

    /**
     * 生产计划sku数量
     */
    @ApiModelProperty(value = "生产计划sku数量", name = "produceSkuCount")
    private BigDecimal produceSkuCount;

    /**
     * 生产计划量
     */
    @ApiModelProperty(value = "生产计划量", name = "producePlanQty")
    private BigDecimal producePlanQty;

    /**
     * 有缺口的sku数量
     */
    @ApiModelProperty(value = "有缺口的sku数量", name = "gapSkuCount")
    private BigDecimal gapSkuCount;

    /**
     * 总缺口
     */
    @ApiModelProperty(value = "总缺口", name = "gapQty")
    private BigDecimal gapQty;

    /**
     * 未满足原因JSON
     */
    @ApiModelProperty(value = "未满足原因JSON", name = "notSatisfiedReason")
    private String notSatisfiedReason;

    /**
     * 未满足原因
     */
    @ApiModelProperty(value = "未满足原因", name = "notSatisfiedReasonI18n")
    private String notSatisfiedReasonI18n;

    /**
     * 未满足原因
     */
    @ApiModelProperty(value = "未满足原因", name = "notSatisfiedReasonI18n_zh_CN")
    private String notSatisfiedReasonI18n_zh_CN;
}
