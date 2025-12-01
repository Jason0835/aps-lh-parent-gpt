package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/12
 */
@Data
@ApiModel(value = "sku汇总项目分类Vo", description = "sku汇总项目分类Vo")
public class ProduceSkuSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 总数
     */
    @ApiModelProperty(value = "计划-总数", name = "produceTotal")
    private BigDecimal produceTotal;

    /**
     * 计划生产天数
     */
    @ApiModelProperty(value = "计划生产天数", name = "planDay")
    private BigDecimal planDay;

    /**
     * 平均日产
     */
    @ApiModelProperty(value = "计划-平均日产", name = "avgDailyProduce")
    private BigDecimal avgDailyProduce;

    /**
     * sku总数
     */
    @ApiModelProperty(value = "计划-sku总数", name = "skuCount")
    private BigDecimal produceSkuCount;

    /**
     * 平均sku数(条/个)
     */
    @ApiModelProperty(value = "计划-平均sku数(条/个)", name = "avgSkuCount")
    private BigDecimal produceAvgSkuCount;

    /**
     * 总数
     */
    @ApiModelProperty(value = "完成-总数", name = "finishTotal")
    private BigDecimal finishTotal;

    /**
     * 计划生产天数
     */
    @ApiModelProperty(value = "实际生产天数", name = "actualDay")
    private BigDecimal actualDay;

    /**
     * 平均日产
     */
    @ApiModelProperty(value = "完成-平均日产", name = "avgDailyFinish")
    private BigDecimal avgDailyFinish;

    /**
     * sku总数
     */
    @ApiModelProperty(value = "完成-sku总数", name = "finishSkuCount")
    private BigDecimal finishSkuCount;

    /**
     * 平均sku数(条/个)
     */
    @ApiModelProperty(value = "完成-平均sku数(条/个)", name = "finishAvgSkuCount")
    private BigDecimal finishAvgSkuCount;

    /**
     * 天数是1-7的sku数量
     */
    @ApiModelProperty(value = "天数是1-7的sku数量", name = "day1To7Count")
    private BigDecimal day1To7Count;

    /**
     * 天数是8-14的sku数量
     */
    @ApiModelProperty(value = "天数是8-14的sku数量", name = "day8To14Count")
    private BigDecimal day8To14Count;

    /**
     * 天数是15-21的sku数量
     */
    @ApiModelProperty(value = "天数是15-21的sku数量", name = "day15To21Count")
    private BigDecimal day15To21Count;

    /**
     * 天数是22-31的sku数量
     */
    @ApiModelProperty(value = "天数是22-31的sku数量", name = "day22To31Count")
    private BigDecimal day22To31Count;
}
