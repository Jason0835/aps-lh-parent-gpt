package com.zlt.aps.gsq.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 钢丝圈颜色提示信息表
 * </p>
 */
@Data
@ApiModel(value="GsqSteelTypeColor对象", description="钢丝圈颜色提示信息表")
public class GsqSteelTypeColorDto extends ApsBaseDto implements Serializable{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "钢丝类型")
    @Excel(name = "ui.data.column.scheduleResult.steelType")
    @ImportValidated(name = "ui.data.column.scheduleResult.steelType", required = true, maxLength = 20)
    private String steelType;

    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）")
    @Excel(name = "ui.bigRollColor.column.colorType",dictType="BIG_ROLL_COLOR")
    @ImportValidated(name = "ui.bigRollColor.column.colorType", required = true, isCode = true, maxLength = 1)
    private String colorType;

    @ApiModelProperty(value = "颜色代码，例如：#000000")
    @Excel(name = "ui.bigRollColor.column.colorCode")
    @ImportValidated(colorCode = true, maxLength = 50)
    private String colorCode;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @Excel(name = "ui.bigRollColor.column.status",dictType="STATUS")
    @ImportValidated(name = "ui.bigRollColor.column.status", required = true, isCode = true, maxLength = 1)
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
