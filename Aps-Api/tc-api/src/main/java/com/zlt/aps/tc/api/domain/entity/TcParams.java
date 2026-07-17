package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcParams.java
 * 描    述：胎侧排程参数配置 实体类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@ApiModel(value = "胎侧排程参数配置对象", description = "胎侧排程参数配置对象")
@Data
@TableName(value = "T_TC_PARAMS")
public class TcParams extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.params.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.params.paramCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "参数编码", name = "paramCode")
    @TableField(value = "PARAM_CODE")
    private String paramCode;

    @Excel(name = "ui.data.column.tc.params.paramName")
    @ImportValidated(required = true, maxLength = 50)
    @ApiModelProperty(value = "参数名称", name = "paramName")
    @TableField(value = "PARAM_NAME")
    private String paramName;

    @Excel(name = "ui.data.column.tc.params.paramValue")
    @ImportValidated(maxLength = 200)
    @ApiModelProperty(value = "参数值", name = "paramValue")
    @TableField(value = "PARAM_VALUE")
    private String paramValue;

    @Excel(name = "ui.data.column.tc.params.defaultValue")
    @ImportValidated(maxLength = 200)
    @ApiModelProperty(value = "默认值", name = "defaultValue")
    @TableField(value = "DEFAULT_VALUE")
    private String defaultValue;

    @ApiModelProperty(value = "校验正则", name = "regularExpression")
    @TableField(value = "REGULAR_EXPRESSION")
    private String regularExpression;

    @ApiModelProperty(value = "错误提示", name = "errorTips")
    @TableField(value = "ERROR_TIPS")
    private String errorTips;

    @Excel(name = "ui.data.column.tc.params.paramGroup")
    @ImportValidated(maxLength = 50)
    @ApiModelProperty(value = "参数分组", name = "paramGroup")
    @TableField(value = "PARAM_GROUP")
    private String paramGroup;

    @Excel(name = "ui.data.column.tc.params.valueType")
    @ImportValidated(maxLength = 50)
    @ApiModelProperty(value = "参数值类型", name = "valueType")
    @TableField(value = "VALUE_TYPE")
    private String valueType;

    @Excel(name = "ui.data.column.tc.params.enableStatus", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    /** 参数生效开始时间，为空表示不限制开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tc.params.effectiveStartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(date = true)
    @ApiModelProperty(value = "生效开始时间", name = "effectiveStartTime")
    @TableField(value = "EFFECTIVE_START_TIME")
    private Date effectiveStartTime;

    /** 参数生效结束时间，为空表示长期有效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tc.params.effectiveEndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(date = true)
    @ApiModelProperty(value = "生效结束时间", name = "effectiveEndTime")
    @TableField(value = "EFFECTIVE_END_TIME")
    private Date effectiveEndTime;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
