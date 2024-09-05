package com.zlt.aps.gsq.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 钢丝圈缠绕盘信息表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-09
 */
@Data
@ApiModel(value="GsqTwiningDisc对象", description="钢丝圈缠绕盘信息表")
public class GsqTwiningDiscDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "编号")
    @Excel(name = "ui.twiningDisc.column.serialNumber")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    private String serialNumber;

    @ApiModelProperty(value = "缠绕盘名称")
    @Excel(name = "ui.twiningDisc.column.name")
    @ImportValidated(required = true, maxLength = 16)
    private String name;

    @ApiModelProperty(value = "序号")
    private Long seq;

    @ApiModelProperty(value = "规格尺寸")
    @Excel(name = "ui.twiningDisc.column.spec")
    @ImportValidated(required = true, isInteger = true, min = 1, max = 999999999)
    private String spec;

    @ApiModelProperty(value = "排列方式")
    @Excel(name = "ui.twiningDisc.column.orderWay")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    private String orderWay;

    @ApiModelProperty(value = "用途")
    @Excel(name = "ui.twiningDisc.column.purpose")
    @ImportValidated(maxLength = 40)
    private String purpose;


    @ApiModelProperty(value = "数量，描述对应的缠绕盘数量信息。")
    @Excel(name = "ui.twiningDisc.column.twiningNum")
    @ImportValidated(isInteger = true, min = 0, max = 9999)
    private Integer twiningNum;

    @ApiModelProperty(value = "入厂时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.twiningDisc.column.inTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(date = true)
    private Date inTime;

    @ApiModelProperty(value = "报废时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.twiningDisc.column.scrapTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(date = true)
    private Date scrapTime;

    @ApiModelProperty(value = "报废原因")
    @Excel(name = "ui.twiningDisc.column.scrapReason")
    @ImportValidated(maxLength = 50)
    private String scrapReason;

    @ApiModelProperty(value = "使用机台id（对应T_GSQ_MACHINE_INFO表id）")
    private Long machineId;

    @ApiModelProperty(value = "机台名称")
    @Excel(name = "ui.twiningDisc.column.machine",importName = "ui.data.column.loss.line")
    @ImportValidated(required = true, maxLength = 30)
    private String machineName;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
