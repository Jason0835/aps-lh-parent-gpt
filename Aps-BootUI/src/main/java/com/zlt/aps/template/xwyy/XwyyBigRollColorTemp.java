package com.zlt.aps.template.xwyy;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "XwyyBigRollColor对象", description = "帘布大卷颜色提示信息表")
public class XwyyBigRollColorTemp {

    @ApiModelProperty(value = "帘布大卷编号")
    @Excel(name = "ui.bigRollColor.column.bigRollCode", sort = 10)
    private String bigRollCode;

    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）")
    @Excel(name = "ui.bigRollColor.column.colorType", sort = 30, dictType = "BIG_ROLL_COLOR")
    private String colorType;

    @ApiModelProperty(value = "颜色代码，例如：#000000")
    @Excel(name = "ui.bigRollColor.column.colorCode", sort = 20)
    private String colorCode;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @Excel(name = "ui.bigRollColor.column.status", sort = 40, dictType = "STATUS")
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark", sort = 50)
    private String remark;
}
