package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 胎类区分对象
 *
 * @author Chen
 * @date 2025/3/31
 */
@Data
public class TireTypeClassificationVo implements Serializable {

    /**
     * 订单号，用于关联
     */
    private String orderNo;

    /**
     * 分类名称
     */
    @ApiModelProperty(value = "分类名称", name = "classificationName")
    private String classificationName;

    /**
     * 需求SKU个数
     */
    @ApiModelProperty(value = "需求SKU个数", name = "saleSkuCount")
    private BigDecimal saleSkuCount;

    /**
     * 库存SKU个数
     */
    @ApiModelProperty(value = "库存SKU个数", name = "stockSkuCount")
    private BigDecimal stockSkuCount;

    /**
     * 库存数量
     */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    private BigDecimal stockQty;

    /**
     * 总需求数量
     */
    @ApiModelProperty(value = "总需求数量", name = "totalSalePlanQty")
    private BigDecimal totalSalePlanQty;

    /**
     * 排产SKU个数
     */
    @ApiModelProperty(value = "排产SKU个数", name = "produceSkuCount")
    private BigDecimal produceSkuCount;

    /**
     * 排产数量
     */
    @ApiModelProperty(value = "排产数量", name = "produceQty")
    private BigDecimal produceQty;

    /**
     * 总缺口数量
     */
    @ApiModelProperty(value = "总缺口数量", name = "gapQty")
    private BigDecimal gapQty;

    /**
     * 月末理论滚动库存数量
     */
    @ApiModelProperty(value = "月末理论滚动库存数量", name = "monthEndStockQty")
    private BigDecimal monthEndStockQty;

    /**
     * 本月满足度
     */
    @ApiModelProperty(value = "本月满足度", name = "thisMonthFinishRate")
    private BigDecimal thisMonthFinishRate;

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
     * 计算字段
     */
    public void calculateFields() {
        BigDecimal stockQty = Optional.ofNullable(this.stockQty).orElse(BigDecimal.ZERO);
        BigDecimal produceQty = Optional.ofNullable(this.produceQty).orElse(BigDecimal.ZERO);
        BigDecimal totalSalePlanQty = Optional.ofNullable(this.totalSalePlanQty).orElse(BigDecimal.ZERO);
        this.gapQty = stockQty.add(produceQty).subtract(totalSalePlanQty);
        this.monthEndStockQty = stockQty.add(produceQty).subtract(totalSalePlanQty);
        if (stockQty.compareTo(BigDecimal.ZERO) > 0) {
            this.thisMonthFinishRate = totalSalePlanQty.divide(stockQty, 2, RoundingMode.HALF_UP);
        }
    }
}
