package com.zlt.aps.cx.api.domain.dto;

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
public class CxSpecColorDto extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 规格型号
     */
    @ImportValidated(required = true, maxLength = 300)
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
    @ImportValidated(maxLength = 2)
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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSpecDesc(String specDesc) {
        this.specDesc = specDesc;
    }

    public String getSpecDesc() {
        return specDesc;
    }

    public void setColorType(String colorType) {
        this.colorType = colorType;
    }

    public String getColorType() {
        return colorType;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "CxSpecColorDto{" +
                "id=" + id +
                ", specDesc='" + specDesc + '\'' +
                ", colorType='" + colorType + '\'' +
                ", colorCode='" + colorCode + '\'' +
                ", status='" + status + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }

}
