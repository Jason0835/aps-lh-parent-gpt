package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 生产销售计划数据结果Vo
 *
 * @author Chen
 * @date 2025/4/2
 */
@Data
@ApiModel(value = "生产销售计划数据结果Vo", description = "生产销售计划数据结果Vo")
public class ProduceSalePlanResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 展示使用，包含合计
     */
    @ApiModelProperty(value = "展示使用，包含合计", name = "showProSize")
    private String showProSize;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /**
     * 渠道，字典：biz_channel_type
     */
    @ApiModelProperty(value = "渠道，字典：biz_channel_type", name = "channel")
    private String channel;

    /**
     * 渠道名称
     */
    @ApiModelProperty(value = "渠道名称", name = "channelName")
    private String channelName;

    /**
     * 单胎重量
     */
    @ApiModelProperty(value = "单胎重量", name = "singleTireWeight")
    private BigDecimal singleTireWeight;

    /**
     * 月生产量
     */
    @ApiModelProperty(value = "月生产量", name = "monthProduceQty")
    private BigDecimal monthProduceQty;

    /**
     * 月生产重量
     */
    @ApiModelProperty(value = "月生产重量", name = "monthProduceWeight")
    private BigDecimal monthProduceWeight;

    /**
     * 月生产量占比
     */
    @ApiModelProperty(value = "月生产量占比", name = "monthProduceQtyProportion")
    private BigDecimal monthProduceQtyProportion;

    /**
     * 月生产重量占比
     */
    @ApiModelProperty(value = "月生产重量占比", name = "monthProduceWeightProportion")
    private BigDecimal monthProduceWeightProportion;

    /**
     * 月销售量
     */
    @ApiModelProperty(value = "月销售量", name = "monthSaleQty")
    private BigDecimal monthSaleQty;

    /**
     * 月销售重量
     */
    @ApiModelProperty(value = "月销售重量", name = "monthSaleWeight")
    private BigDecimal monthSaleWeight;

    /**
     * 月销售量占比
     */
    @ApiModelProperty(value = "月销售量占比", name = "monthSaleQtyProportion")
    private BigDecimal monthSaleQtyProportion;

    /**
     * 月销售重量占比
     */
    @ApiModelProperty(value = "月销售重量占比", name = "monthSaleWeightProportion")
    private BigDecimal monthSaleWeightProportion;

    /**
     * 计算月生产量占比，月生产重量占比、月销售量占比、月销售重量占比
     *
     * @param sumVo 计算比例的汇总对象
     */
    public void calculateProportion(ProduceSalePlanResultVo sumVo) {
        BigDecimal sumMonthProduceQty = Optional.ofNullable(sumVo.getMonthProduceQty()).orElse(BigDecimal.ZERO);
        BigDecimal produceQty = Optional.ofNullable(this.getMonthProduceQty()).orElse(BigDecimal.ZERO);
        this.monthProduceQtyProportion = sumMonthProduceQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : produceQty.divide(sumMonthProduceQty, 4, RoundingMode.HALF_UP);

        BigDecimal sumMonthProduceWeight = Optional.ofNullable(sumVo.getMonthProduceWeight()).orElse(BigDecimal.ZERO);
        BigDecimal produceWeight = Optional.ofNullable(this.getMonthProduceWeight()).orElse(BigDecimal.ZERO);
        this.monthProduceWeightProportion = sumMonthProduceWeight.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : produceWeight.divide(sumMonthProduceWeight, 4, RoundingMode.HALF_UP);

        BigDecimal sumMonthSaleQty = Optional.ofNullable(sumVo.getMonthSaleQty()).orElse(BigDecimal.ZERO);
        BigDecimal saleQty = Optional.ofNullable(this.getMonthSaleQty()).orElse(BigDecimal.ZERO);
        this.monthSaleQtyProportion = sumMonthSaleQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : saleQty.divide(sumMonthSaleQty, 4, RoundingMode.HALF_UP);

        BigDecimal sumMonthSaleWeight = Optional.ofNullable(sumVo.getMonthSaleWeight()).orElse(BigDecimal.ZERO);
        BigDecimal saleWeight = Optional.ofNullable(this.getMonthSaleWeight()).orElse(BigDecimal.ZERO);
        this.monthSaleWeightProportion = sumMonthSaleWeight.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : saleWeight.divide(sumMonthSaleWeight, 4, RoundingMode.HALF_UP);
    }
}
