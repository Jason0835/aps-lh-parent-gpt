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
 * 每日各工序工单完成情况统计报表对象
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "每日各工序工单完成情况统计报表对象", description = "每日各工序工单完成情况统计报表对象")
public class ReportOrderStatisticsDto extends ApsBaseDto {

    @ApiModelProperty("排程日期")
    private String scheduleDate;

    @ApiModelProperty("工序代码")
    private String procedureCode;

    @ApiModelProperty("工单号")
    private String orderNo;

    @ApiModelProperty("物料规格")
    private String specCode;

    @ApiModelProperty("计划生产量")
    private Double planProductionQty;

    @ApiModelProperty("实际完成量")
    private Double actualFinishQty;

    @ApiModelProperty("完成率")
    private String finishRate;
    
    @ApiModelProperty("数据类型。1：明细数据，2：合计数据")
    private String dataType;


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
