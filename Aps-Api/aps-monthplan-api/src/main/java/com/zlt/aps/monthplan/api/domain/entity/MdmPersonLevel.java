package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmPersonLevel.java
 * 描    述：成型机人员档配置对象 t_mdm_person_level
 *@author hsc
 *@date 2025-02-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */

@ApiModel(value = "成型机人员档配置对象", description = "成型机人员档配置对象 ")
@Data
@TableName(value = "T_MDM_PERSON_LEVEL")
public class MdmPersonLevel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码 默认116
     */
    @Excel(name = "ui.data.column.mdmPersonLevel.factoryCode")
    @ApiModelProperty(value = "分厂编码 默认116", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.mdmPersonLevel.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.mdmPersonLevel.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 人员档等级代码 */
    @Excel(name = "ui.data.column.mdmPersonLevel.levelCode")
    @ApiModelProperty(value = "人员档等级代码", name = "levelCode")
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /** 成形法类别 一次法 二次法 */
    @Excel(name = "ui.data.column.mdmPersonLevel.methodType")
    @ApiModelProperty(value = "成形法类别 一次法 二次法", name = "methodType")
    @TableField(value = "METHOD_TYPE")
    private String methodType;

    /** 排挡数量 */
    @Excel(name = "ui.data.column.mdmPersonLevel.machineNumber")
    @ApiModelProperty(value = "排挡数量", name = "machineNumber")
    @TableField(value = "MACHINE_NUMBER")
    private Integer machineNumber;

    /** 产能系数 */
    @Excel(name = "ui.data.column.mdmPersonLevel.factor")
    @ApiModelProperty(value = "产能系数", name = "factor")
    @TableField(value = "FACTOR")
    private BigDecimal factor;


}
