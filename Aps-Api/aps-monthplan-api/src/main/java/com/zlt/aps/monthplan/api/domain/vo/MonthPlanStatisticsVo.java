package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 月度计划统计对象
 *
 * @author Liam
 * @since 2025/5/7
 */
@ApiModel(value = "月度计划统计对象", description = "月度计划统计对象")
@Data
public class MonthPlanStatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排产SAP个数", name = "productionCount")
    private Long productionCount = 0L;

    @ApiModelProperty(value = "未排SAP总量", name = "noProductionCount")
    private Long noProductionCount = 0L;

    @ApiModelProperty(value = "已排SAP总量", name = "productionSum")
    private Long productionSum = 0L;

    @ApiModelProperty(value = "提报的SAP个数", name = "reportCount")
    private Long reportCount = 0L;

    @ApiModelProperty(value = "提报的SAP总量", name = "reportSum")
    private Long reportSum = 0L;

    @ApiModelProperty(value = "备货量", name = "stockNum")
    private Long stockNum = 0L;

    /**
     * 净需求
     */
    @ApiModelProperty(value = "净需求", name = "netDemandQty")
    private Long netDemandQty = 0L;

    /**
     * 缺口
     */
    @ApiModelProperty(value = "缺口", name = "gapQty")
    private Long gapQty = 0L;

    /**
     * 销售需求总量
     */
    @ApiModelProperty(value = "销售需求总量", name = "prodReqPlan")
    private Long prodReqPlan = 0L;
}
