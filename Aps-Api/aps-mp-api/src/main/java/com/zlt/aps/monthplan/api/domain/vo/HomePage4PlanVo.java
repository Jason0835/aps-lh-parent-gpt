package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/6/28
 */
@Data
public class HomePage4PlanVo implements Serializable {

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 今年总计划量
     */
    @ApiModelProperty(value = "今年总计划量", name = "yearTotalPlanQty")
    private Double yearTotalPlanQty = 0D;

    /**
     * 今年完成量
     */
    @ApiModelProperty(value = "今年完成量", name = "yearTotalFinishQty")
    private Double yearTotalFinishQty = 0D;

    /**
     * 本月总计划量
     */
    @ApiModelProperty(value = "本月总计划量", name = "monthTotalPlanQty")
    private Double monthTotalPlanQty = 0D;

    /**
     * 本月完成量
     */
    @ApiModelProperty(value = "本月完成量", name = "monthTotalFinishQty")
    private Double monthTotalFinishQty = 0D;

    /**
     * 计划达成率
     */
    @ApiModelProperty(value = "计划达成率", name = "planFinishRate")
    private Double planFinishRate = 0D;

    /**
     * 本月总规格数
     */
    @ApiModelProperty(value = "本月总规格数", name = "monthTotalSpecQty")
    private Double monthTotalSpecQty = 0D;

    /**
     * 本月规格完成量
     */
    @ApiModelProperty(value = "本月规格完成量", name = "monthTotalSpecFinishQty")
    private Double monthTotalSpecFinishQty = 0D;

    /**
     * 规格完成率
     */
    @ApiModelProperty(value = "规格完成率", name = "monthSpecFinishRate")
    private Double monthSpecFinishRate = 0D;
}
