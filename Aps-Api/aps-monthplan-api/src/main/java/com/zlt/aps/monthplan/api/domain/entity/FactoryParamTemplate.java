package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamTemplate.java
 * 描    述：系统参数设置模板对象 t_mdm_param_template
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-26
 */

@ApiModel(value = "系统参数设置模板对象", description = "系统参数设置模板对象 ")
@Data
@TableName(value = "T_MDM_PARAM_TEMPLATE")
public class FactoryParamTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型 01 月度计划排产
     */
    @Excel(name = "ui.data.column.factoryParamTemplate.businessType")
    @ApiModelProperty(value = "业务类型 01 月度计划排产", name = "businessType")
    @TableField(value = "BUSINESS_TYPE")
    private String businessType;

    /**
     * 业务分组--有业务关联的参数一个组
     */
    @ApiModelProperty(value = "业务分组", name = "businessGroup")
    @TableField(value = "BUSINESS_GROUP")
    private String businessGroup;

    /**
     * 参数编码
     */
    @Excel(name = "ui.data.column.factoryParamTemplate.paramCode")
    @ApiModelProperty(value = "参数编码", name = "paramCode")
    @TableField(value = "PARAM_CODE")
    private String paramCode;

    /**
     * 参数名称
     */
    @Excel(name = "ui.data.column.factoryParamTemplate.paramName")
    @ApiModelProperty(value = "参数名称", name = "paramName")
    @TableField(value = "PARAM_NAME")
    private String paramName;

    /**
     * 数据类型:            0-字符型            1-整型            2-数值型            3-日期型            4-时间型            5-日期时间型            6-布尔型
     */
    @Excel(name = "ui.data.column.factoryParamTemplate.dataType")
    @ApiModelProperty(value = "数据类型:            0-字符型            1-整型            2-数值型            3-日期型            4-时间型            5-日期时间型            6-布尔型", name = "dataType")
    @TableField(value = "DATA_TYPE")
    private Integer dataType;

    /**
     * 默认值
     */
    @Excel(name = "ui.data.column.factoryParamTemplate.defauleValue")
    @ApiModelProperty(value = "默认值", name = "defauleValue")
    @TableField(value = "DEFAULE_VALUE")
    private String defauleValue;

}