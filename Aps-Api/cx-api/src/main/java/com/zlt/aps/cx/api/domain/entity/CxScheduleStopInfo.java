package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@ApiModel(value = "成型自动排程停排信息表", description = "成型自动排程停排信息表")
@Data
public class CxScheduleStopInfo extends ApsBaseEntity {

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    private Long id;

    @ApiModelProperty(value = "成型批次号")
    private String cxBatchNo;

    @ApiModelProperty(value = "成型工单号")
    private String orderNo;

    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;
    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 停排班次下标
     */
    @ApiModelProperty(value = "停排班次下标：0：昨日三班；1:1班;2:二班；3：三班;4:次一班；5：次二班")
    private Integer classShift;

    @ApiModelProperty(value = "停排原因")
    private String stopReason;
}
