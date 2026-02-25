package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/6/28
 */
@Data
public class HomePage4OrderVo implements Serializable {

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 计划订单量
     */
    @ApiModelProperty(value = "计划订单量", name = "planOrderQty")
    private Double planOrderQty = 0D;

    /**
     * 备货量
     */
    @ApiModelProperty(value = "备货量", name = "stockUpQty")
    private Double stockUpQty = 0D;

    /**
     * 订单需求量
     */
    @ApiModelProperty(value = "订单需求量", name = "orderQty")
    private Double orderQty = 0D;

    /**
     * 实际排产计划量
     */
    @ApiModelProperty(value = "实际排产计划量", name = "planQty")
    private Double planQty = 0D;
}
