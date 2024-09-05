package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 规格字体颜色设置对象 t_cx_spec_color
 *
 * @author chen
 * @date 2021-08-21
 */
@ApiModel(value = "规格字体颜色设置对象", description = "规格字体颜色设置对象 ")
public class CxSpecColorTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 规格型号
     */
    @ImportValidated(required = true, maxLength = 100)
    @Excel(name = "ui.data.column.specColor.specDesc", width = 50)
    @ApiModelProperty(value = "规格型号")
    private String specDesc;

    /**
     * 颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）
     */
    @ImportValidated(required = true, maxLength = 1)
    @Excel(name = "ui.data.column.specColor.colorType", dictType = "BIG_ROLL_COLOR")
    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色")
    private String colorType;

    /**
     * 颜色代码，例如：#000000
     */
    @ImportValidated(colorCode = true, maxLength = 10)
    @Excel(name = "ui.data.column.specColor.colorCode")
    @ApiModelProperty(value = "颜色代码，例如：#000000")
    private String colorCode;

    /**
     * 状态，0--启用，1--禁用。
     */
    @ImportValidated(maxLength = 1)
    @Excel(name = "ui.data.column.status", dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    private String status;

    /**
     * 备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;


}
