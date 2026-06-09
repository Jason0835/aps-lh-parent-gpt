package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "直裁参数设置", description = "直裁参数设置")
@TableName("t_cd90_params")
public class Cd90Params extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */

    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90Params.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 参数编码 */

    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("PARAM_CODE")
    @Excel(name = "ui.data.column.cd90Params.paramCode")
    private String paramCode;

    /** 参数名称 */

    @ImportExcelValidated(maxLength = 50)
    @TableField("PARAM_NAME")
    @Excel(name = "ui.data.column.cd90Params.paramName")
    private String paramName;

    /** 参数值 */

    @ImportExcelValidated(maxLength = 50)
    @TableField("PARAM_VALUE")
    @Excel(name = "ui.data.column.cd90Params.paramValue")
    private String paramValue;

    /** 正则表达式 */

    @ImportExcelValidated(maxLength = 100)
    @TableField("REGULAR_EXPRESSION")
    @Excel(name = "ui.data.column.cd90Params.regularExpression")
    private String regularExpression;

    /** 错误提示 */

    @ImportExcelValidated(maxLength = 200)
    @TableField("ERROR_TIPS")
    @Excel(name = "ui.data.column.cd90Params.errorTips")
    private String errorTips;
}