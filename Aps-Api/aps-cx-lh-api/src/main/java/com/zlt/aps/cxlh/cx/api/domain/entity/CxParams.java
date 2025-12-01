package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxParams.java
 * 描    述：成型工序参数信息对象 t_cx_params
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型工序参数信息对象", description = "成型工序参数信息对象 ")
@Data
@TableName(value = "T_CX_PARAMS")
//@KeySequence(value = "SEQ_ARAMS")
public class CxParams extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

     /** 参数code */
    @Excel(name = "ui.data.column.cxParams.paramCode")
    @ApiModelProperty(value = "参数code", name = "paramCode")
    @TableField(value = "PARAM_CODE")
    private String paramCode;

    /** 参数名称 */
    @Excel(name = "ui.data.column.cxParams.paramName")
    @ApiModelProperty(value = "参数名称", name = "paramName")
    @TableField(value = "PARAM_NAME")
    private String paramName;

    /** 参数值 */
    @Excel(name = "ui.data.column.cxParams.paramValue")
    @ApiModelProperty(value = "参数值", name = "paramValue")
    @TableField(value = "PARAM_VALUE")
    private String paramValue;

    /** 参数值对应的正则表达式 */
    @Excel(name = "ui.data.column.cxParams.regularExpression")
    @ApiModelProperty(value = "参数值对应的正则表达式", name = "regularExpression")
    @TableField(value = "REGULAR_EXPRESSION")
    private String regularExpression;

    /** 参数值根据正则表达式校验是失败后的错误提示 */
    @Excel(name = "ui.data.column.cxParams.errorTips")
    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示", name = "errorTips")
    @TableField(value = "ERROR_TIPS")
    private String errorTips;


}