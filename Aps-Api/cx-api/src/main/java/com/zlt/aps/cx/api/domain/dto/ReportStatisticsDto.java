package com.zlt.aps.cx.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.Map;

/**
 * @author: Chen
 * @since: 2022/4/25 10:02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "报表统计对象", description = "报表统计对象")
public class ReportStatisticsDto extends ApsBaseDto {

    public ReportStatisticsDto() {
        planProductionQty = 0D;
        actualProductionQty = 0D;
        actualProFinishRateLow = 0D;
        actualProFinishRateMid = 0D;
        actualProFinishRateHigh = 0D;
        totalSpecifications = 0D;
    }

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty("排程日期")
    private Date scheduleDate;

    @ApiModelProperty("工序代码")
    private String procedureCode;

    @ApiModelProperty("计划生产量")
    private Double planProductionQty;

    @ApiModelProperty("实际生产量")
    private Double actualProductionQty;

    @ApiModelProperty("生产完成率")
    private Double produceFinishRate;

    @ApiModelProperty("实际生产规格完成率:X<90%")
    private Double actualProFinishRateLow;

    @ApiModelProperty("实际生产规格完成率:90%<X<110%")
    private Double  actualProFinishRateMid;

    @ApiModelProperty("实际生产规格完成率:X>110%")
    private Double actualProFinishRateHigh;

    @ApiModelProperty("班次计划准确率")
    private Double shiftPlanAccuracy;

    @ApiModelProperty("规格总量")
    private Double totalSpecifications;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "查询开始日期yyyy-MM-dd")
    private Date startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "查询结束日期yyyy-MM-dd")
    private Date endTime;

    @ApiModelProperty("统计方式。1：每天，2：汇总")
    private String statisticalMethod;

    @ApiModelProperty("用于导出使用的工序字典转换")
    private Map<String, String> procedureCodeMap;
}
