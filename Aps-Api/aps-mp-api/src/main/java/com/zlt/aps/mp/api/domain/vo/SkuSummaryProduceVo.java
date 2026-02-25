package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/13
 */
@Data
public class SkuSummaryProduceVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份，如果是13=年累计、14=H1、15=环比", name = "month")
    private Integer month;

    /**
     * 天数是1-7的sku数量
     */
    @ApiModelProperty(value = "天数是1-7的sku数量", name = "month")
    private BigDecimal day1To7Count;

    /**
     * 天数是8-14的sku数量
     */
    @ApiModelProperty(value = "天数是8-14的sku数量", name = "month")
    private BigDecimal day8To14Count;

    /**
     * 天数是15-21的sku数量
     */
    @ApiModelProperty(value = "天数是15-21的sku数量", name = "month")
    private BigDecimal day15To21Count;

    /**
     * 天数是22-31的sku数量
     */
    @ApiModelProperty(value = "天数是22-31的sku数量", name = "day22To31Count")
    private BigDecimal day22To31Count;

    /**
     * 合计
     */
    @ApiModelProperty(value = "合计", name = "totalCount")
    private BigDecimal totalCount;

}
