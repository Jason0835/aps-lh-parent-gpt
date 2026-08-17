package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 15度裁断参数设置
 */
@Data
@ApiModel(value = "15度裁断参数设置", description = "15度裁断参数设置")
@TableName("t_cd15_params")
public class Cd15Params extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15Params.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 参数编码 */
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("PARAM_CODE")
    @Excel(name = "ui.data.column.cd15Params.paramCode")
    private String paramCode;

    /** 参数名称 */
    @ImportExcelValidated(maxLength = 50)
    @TableField("PARAM_NAME")
    @Excel(name = "ui.data.column.cd15Params.paramName")
    private String paramName;

    /** 参数值 */
    @ImportExcelValidated(maxLength = 50)
    @TableField("PARAM_VALUE")
    @Excel(name = "ui.data.column.cd15Params.paramValue")
    private String paramValue;

    /** 正则表达式 */
    @ImportExcelValidated(maxLength = 100)
    @TableField("REGULAR_EXPRESSION")
    private String regularExpression;

    /** 错误提示 */
    @ImportExcelValidated(maxLength = 200)
    @TableField("ERROR_TIPS")
    private String errorTips;

    /** 参数标识 */
    @ImportExcelValidated(maxLength = 50)
    @TableField("REMARK2")
    private String remark2;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}