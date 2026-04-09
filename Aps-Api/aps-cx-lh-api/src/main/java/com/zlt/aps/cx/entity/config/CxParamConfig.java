package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 排程参数配置表
 * 对应表：T_CX_PARAM_CONFIG
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_PARAM_CONFIG")
@ApiModel(value = "排程参数配置对象", description = "排程参数配置表")
public class CxParamConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Excel(name = "ui.data.column.cxParamConfig.paramCode")
    @ApiModelProperty(value = "参数编码")
    @TableField("PARAM_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String paramCode;

    @Excel(name = "ui.data.column.cxParamConfig.paramName")
    @ApiModelProperty(value = "参数名称")
    @TableField("PARAM_NAME")
    @ImportValidated(maxLength = 100)
    private String paramName;

    @Excel(name = "ui.data.column.cxParamConfig.paramValue")
    @ApiModelProperty(value = "参数值")
    @TableField("PARAM_VALUE")
    @ImportValidated(maxLength = 200)
    private String paramValue;

    @Excel(name = "ui.data.column.cxParamConfig.isActive", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @Excel(name = "ui.data.column.cxParamConfig.regularExpression")
    @ApiModelProperty(value = "参数值对应的正则表达式")
    @TableField("REGULAR_EXPRESSION")
    @ImportValidated(maxLength = 500)
    private String regularExpression;

    @Excel(name = "ui.data.column.cxParamConfig.errorTips")
    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示")
    @TableField("ERROR_TIPS")
    @ImportValidated(maxLength = 200)
    private String errorTips;
}
