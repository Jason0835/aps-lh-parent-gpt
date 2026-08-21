package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：GsqParams.java
 * 描    述：钢丝圈排程参数配置 实体类（对齐胎圈 TqParams / 胎面 TmParams）
 *
 * <p>映射 T_GSQ_PARAMS 表，承载自动排程参数和自动滚动参数，
 * 支持按工厂+参数编码查询生效参数值。</p>
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
@ApiModel(value = "钢丝圈排程参数配置对象", description = "钢丝圈排程参数配置对象")
@Data
@TableName(value = "T_GSQ_PARAMS")
public class GsqParams extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 参数代码：自动触发提前分钟数（班次开始前N分钟触发滚动更新） */
    public static final String PARAM_CODE_ROLLING_LEAD_MINUTES = "GSQ_ROLLING_AUTO_TRIGGER_LEAD_MINUTES";
    /** 参数代码：3班库存阈值（用于库存积压判断的班次数） */
    public static final String PARAM_CODE_STOCK_THRESHOLD_CLASSES = "GSQ_ROLLING_STOCK_THRESHOLD_CLASSES";
    /** 参数代码：自动滚动开关（1-启用，0-关闭） */
    public static final String PARAM_CODE_AUTO_ROLLING_ENABLED = "GSQ_AUTO_ROLLING_ENABLED";
    /** 参数代码：自动滚动提前窗口（分钟），早于班次开始多少分钟开始进入触发窗口 */
    public static final String PARAM_CODE_ROLLING_EARLY_MINUTES = "GSQ_ROLLING_EARLY_MINUTES";
    /** 参数代码：自动滚动延后窗口（分钟），晚于班次开始多少分钟结束触发窗口 */
    public static final String PARAM_CODE_ROLLING_LATE_MINUTES = "GSQ_ROLLING_LATE_MINUTES";
    /** 参数代码：自动滚动输入稳定时间（分钟），库存快照需保持稳定N分钟才允许触发 */
    public static final String PARAM_CODE_ROLLING_STABLE_MINUTES = "GSQ_ROLLING_STABLE_MINUTES";

    /** 默认值：自动触发提前分钟数 */
    public static final int DEFAULT_LEAD_MINUTES = 30;
    /** 默认值：3班库存阈值班次数 */
    public static final int DEFAULT_THRESHOLD_CLASSES = 3;
    /** 默认值：自动滚动开关（关闭） */
    public static final String DEFAULT_AUTO_ROLLING_ENABLED = "0";
    /** 默认值：自动滚动提前窗口（30分钟） */
    public static final String DEFAULT_ROLLING_EARLY_MINUTES = "30";
    /** 默认值：自动滚动延后窗口（15分钟） */
    public static final String DEFAULT_ROLLING_LATE_MINUTES = "15";
    /** 默认值：自动滚动输入稳定时间（5分钟） */
    public static final String DEFAULT_ROLLING_STABLE_MINUTES = "5";

    @Excel(name = "ui.data.column.gsq.params.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.gsq.params.paramCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "参数编码", name = "paramCode")
    @TableField(value = "PARAM_CODE")
    private String paramCode;

    @Excel(name = "ui.data.column.gsq.params.paramName")
    @ImportValidated(required = true, maxLength = 50)
    @ApiModelProperty(value = "参数名称", name = "paramName")
    @TableField(value = "PARAM_NAME")
    private String paramName;

    @Excel(name = "ui.data.column.gsq.params.paramValue")
    @ImportValidated(maxLength = 200)
    @ApiModelProperty(value = "参数值", name = "paramValue")
    @TableField(value = "PARAM_VALUE")
    private String paramValue;

    @Excel(name = "ui.data.column.gsq.params.defaultValue")
    @ImportValidated(maxLength = 200)
    @ApiModelProperty(value = "默认值", name = "defaultValue")
    @TableField(value = "DEFAULT_VALUE")
    private String defaultValue;

    @Excel(name = "ui.data.column.gsq.params.regularExpression")
    @ImportValidated(maxLength = 200)
    @ApiModelProperty(value = "校验正则", name = "regularExpression")
    @TableField(value = "REGULAR_EXPRESSION")
    private String regularExpression;

    @Excel(name = "ui.data.column.gsq.params.errorTips")
    @ImportValidated(maxLength = 200)
    @ApiModelProperty(value = "错误提示", name = "errorTips")
    @TableField(value = "ERROR_TIPS")
    private String errorTips;

    @Excel(name = "ui.data.column.gsq.params.paramGroup")
    @ImportValidated(maxLength = 50)
    @ApiModelProperty(value = "参数分组", name = "paramGroup")
    @TableField(value = "PARAM_GROUP")
    private String paramGroup;

    @Excel(name = "ui.data.column.gsq.params.valueType")
    @ImportValidated(maxLength = 50)
    @ApiModelProperty(value = "参数值类型", name = "valueType")
    @TableField(value = "VALUE_TYPE")
    private String valueType;

    @Excel(name = "ui.data.column.gsq.params.enableStatus", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
