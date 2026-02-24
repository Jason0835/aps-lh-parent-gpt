package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/3/12
 */
@Data
@ApiModel(value = "sku汇总月份数据Vo", description = "sku汇总月份数据Vo")
public class SkuMonthQtyVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 1月累计
     */
    @ApiModelProperty(value = "1月累计", name = "month1")
    private ProduceSkuSummaryVo month1;

    /**
     * 2月累计
     */
    @ApiModelProperty(value = "2月累计", name = "month2")
    private ProduceSkuSummaryVo month2;

    /**
     * 3月累计
     */
    @ApiModelProperty(value = "3月累计", name = "month3")
    private ProduceSkuSummaryVo month3;

    /**
     * 4月累计
     */
    @ApiModelProperty(value = "4月累计", name = "month4")
    private ProduceSkuSummaryVo month4;

    /**
     * 5月累计
     */
    @ApiModelProperty(value = "5月累计", name = "month5")
    private ProduceSkuSummaryVo month5;

    /**
     * 6月累计
     */
    @ApiModelProperty(value = "6月累计", name = "month6")
    private ProduceSkuSummaryVo month6;

    /**
     * 7月累计
     */
    @ApiModelProperty(value = "7月累计", name = "month7")
    private ProduceSkuSummaryVo month7;

    /**
     * 8月累计
     */
    @ApiModelProperty(value = "8月累计", name = "month8")
    private ProduceSkuSummaryVo month8;

    /**
     * 9月累计
     */
    @ApiModelProperty(value = "9月累计", name = "month9")
    private ProduceSkuSummaryVo month9;

    /**
     * 10月累计
     */
    @ApiModelProperty(value = "10月累计", name = "month10")
    private ProduceSkuSummaryVo month10;

    /**
     * 11月累计
     */
    @ApiModelProperty(value = "11月累计", name = "month11")
    private ProduceSkuSummaryVo month11;

    /**
     * 12月累计
     */
    @ApiModelProperty(value = "12月累计", name = "month12")
    private ProduceSkuSummaryVo month12;

    /**
     * 年累计
     */
    @ApiModelProperty(value = "年累计", name = "yearSum")
    private ProduceSkuSummaryVo yearSum;

    /**
     * 月平均(H1)
     */
    @ApiModelProperty(value = "月平均(H1)", name = "monthAvg")
    private ProduceSkuSummaryVo monthAvg;

    /**
     * 月对比
     */
    @ApiModelProperty(value = "月对比", name = "currentMonthAvgDiff")
    private ProduceSkuSummaryVo currentMonthAvgDiff;
}
