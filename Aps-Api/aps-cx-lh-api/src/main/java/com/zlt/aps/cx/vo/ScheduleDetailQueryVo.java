package com.zlt.aps.cx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@ApiModel(value = "排程明细查询对象")
public class ScheduleDetailQueryVo {

    @ApiModelProperty(value = "主表ID")
    private Long mainId;

    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    @ApiModelProperty(value = "排程日期")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "胎胚编码")
    private String embryoCode;

    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    @ApiModelProperty(value = "物料描述")
    private String materialDesc;

    @ApiModelProperty(value = "主要物料描述")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "工单号")
    private String orderNo;

    @ApiModelProperty(value = "生产状态：0-未生产；1-生产中；2-已收尾")
    private String productionStatus;

    @ApiModelProperty(value = "发布状态：0-未发布；1-已发布")
    private String isRelease;

    @ApiModelProperty(value = "结构名称")
    private String structureName;
}
