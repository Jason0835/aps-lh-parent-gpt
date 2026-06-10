package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TmParams.java
 * 描    述：胎面排程参数配置 实体类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@ApiModel(value = "胎面排程参数配置对象", description = "胎面排程参数配置对象")
@Data
@TableName(value = "T_TM_PARAMS")
public class TmParams extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.Params.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.Params.paramCode")
    @ApiModelProperty(value = "参数编码", name = "paramCode")
    @TableField(value = "PARAM_CODE")
    private String paramCode;

    @Excel(name = "ui.data.column.tm.Params.paramName")
    @ApiModelProperty(value = "参数名称", name = "paramName")
    @TableField(value = "PARAM_NAME")
    private String paramName;

    @Excel(name = "ui.data.column.tm.Params.paramValue")
    @ApiModelProperty(value = "参数值", name = "paramValue")
    @TableField(value = "PARAM_VALUE")
    private String paramValue;

    @Excel(name = "ui.data.column.tm.Params.defaultValue")
    @ApiModelProperty(value = "默认值", name = "defaultValue")
    @TableField(value = "DEFAULT_VALUE")
    private String defaultValue;

    @ApiModelProperty(value = "校验正则", name = "regularExpression")
    @TableField(value = "REGULAR_EXPRESSION")
    private String regularExpression;

    @ApiModelProperty(value = "错误提示", name = "errorTips")
    @TableField(value = "ERROR_TIPS")
    private String errorTips;

    @Excel(name = "ui.data.column.tm.Params.paramGroup")
    @ApiModelProperty(value = "参数分组", name = "paramGroup")
    @TableField(value = "PARAM_GROUP")
    private String paramGroup;

    @Excel(name = "ui.data.column.tm.Params.valueType")
    @ApiModelProperty(value = "参数值类型", name = "valueType")
    @TableField(value = "VALUE_TYPE")
    private String valueType;

    @Excel(name = "ui.data.column.tm.Params.enableStatus", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;
}
