package com.zlt.aps.template.gsq;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value="钢丝圈缠绕盘信息导入模板", description="钢丝圈缠绕盘信息导入模板")
public class GsqTwiningDiscTemp extends BaseEntity {

    @ApiModelProperty(value = "编号")
    @Excel(name = "ui.twiningDisc.column.serialNumber")
    private String serialNumber;

    @ApiModelProperty(value = "缠绕盘名称")
    @Excel(name = "ui.twiningDisc.column.name")
    private String name;

    @ApiModelProperty(value = "规格尺寸")
    @Excel(name = "ui.twiningDisc.column.spec")
    private String spec;

    @ApiModelProperty(value = "排列方式")
    @Excel(name = "ui.twiningDisc.column.orderWay")
    private String orderWay;

    @ApiModelProperty(value = "用途")
    @Excel(name = "ui.twiningDisc.column.purpose")
    private String purpose;

    @ApiModelProperty(value = "数量，描述对应的缠绕盘数量信息。")
    @Excel(name = "ui.twiningDisc.column.twiningNum")
    private Integer twiningNum;

    @ApiModelProperty(value = "入厂时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.twiningDisc.column.inTime", width = 30, dateFormat = "yyyy-MM-dd")
    private Date inTime;

    @ApiModelProperty(value = "报废时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.twiningDisc.column.scrapTime", width = 30, dateFormat = "yyyy-MM-dd")
    private Date scrapTime;

    @ApiModelProperty(value = "报废原因")
    @Excel(name = "ui.twiningDisc.column.scrapReason")
    private String scrapReason;

    @ApiModelProperty(value = "机台编号")
    @Excel(name = "ui.data.column.loss.line")
    private String machineCode;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
