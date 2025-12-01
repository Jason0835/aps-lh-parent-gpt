package com.zlt.aps.template.gdyy;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value="钢带大卷颜色提示信息导入模板", description="钢带大卷颜色提示信息导入模板")
public class GdyySteelRollColorTemp extends BaseEntity {

    @ApiModelProperty(value = "钢压大卷代号")
    @Excel(name = "ui.steelRollColor.column.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "颜色代码，例如：#000000")
    @Excel(name = "ui.bigRollColor.column.colorCode")
    private String colorCode;

    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）")
    @Excel(name = "ui.bigRollColor.column.colorType",dictType="BIG_ROLL_COLOR")
    private String colorType;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @Excel(name = "ui.bigRollColor.column.status",dictType="STATUS")
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
