package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanSumStatisticsVo.java
 * 描    述：需求计划汇总统计对象
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20260302
 */

@Data
@ApiModel(value = "需求计划汇总统计对象", description = "需求计划汇总统计对象 ")
public class DpDemandPlanSumStatisticsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 库存
     */
    @ApiModelProperty(value = "库存", name = "stockQty")
    private Integer stockQty;

    /**
     * 订单量
     */
    @ApiModelProperty(value = "订单量", name = "orderQty")
    private Integer orderQty;

    /**
     * 月底余量
     */
    @ApiModelProperty(value = "月底余量", name = "plannedSurplus")
    private Integer plannedSurplus;


    /**
     * 排产净需求
     */
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    private Integer netQty;

    /**
     * 净需求(含暂缓)
     */
    @ApiModelProperty(value = "净需求(含暂缓)", name = "postponeNetQty")
    private Integer postponeNetQty;

    /**
     * 净需求(不含暂缓)
     */
    @ApiModelProperty(value = "净需求(不含暂缓)", name = "unPostponeNetQty")
    private Integer unPostponeNetQty;

    /**
     * 高优先级
     */
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    private Integer heightQty;

    /**
     * 中优先级
     */
    @ApiModelProperty(value = "中优先级", name = "midQty")
    private Integer midQty;

    /**
     * 暂缓订单
     */
    @ApiModelProperty(value = "暂缓订单", name = "postponeQty")
    private Integer postponeQty;

    /**
     * 周期排产储备
     */
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    private Integer cycleReserveQty;

    /**
     * 常规储备
     */
    @ApiModelProperty(value = "常规储备", name = "conventionReserveQty")
    private Integer conventionReserveQty;

    /**
     * 创建空数据
     *
     * @return
     */
    public static DpDemandPlanSumStatisticsVo createEmpty() {
        DpDemandPlanSumStatisticsVo empty = new DpDemandPlanSumStatisticsVo();
        empty.setOrderQty(BigDecimal.ZERO.intValue());
        empty.setStockQty(BigDecimal.ZERO.intValue());
        empty.setPlannedSurplus(BigDecimal.ZERO.intValue());

        empty.setNetQty(BigDecimal.ZERO.intValue());
        empty.setPostponeNetQty(BigDecimal.ZERO.intValue());
        empty.setUnPostponeNetQty(BigDecimal.ZERO.intValue());

        empty.setHeightQty(BigDecimal.ZERO.intValue());
        empty.setMidQty(BigDecimal.ZERO.intValue());
        empty.setPostponeQty(BigDecimal.ZERO.intValue());
        empty.setCycleReserveQty(BigDecimal.ZERO.intValue());
        empty.setConventionReserveQty(BigDecimal.ZERO.intValue());
        return empty;
    }
}