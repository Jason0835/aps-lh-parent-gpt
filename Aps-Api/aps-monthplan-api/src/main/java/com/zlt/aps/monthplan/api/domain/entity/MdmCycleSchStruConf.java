package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCycleSchStruConf.java
 * 描    述：周期排产结构配置对象 T_DP_CYCLE_SCH_STRU_CONF
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
@ApiModel(value = "周期排产结构配置对象", description = "周期排产结构配置对象 ")
@Data
@TableName(value = "T_DP_CYCLE_SCH_STRU_CONF")
public class MdmCycleSchStruConf extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmCycleSchStruConf.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 结构
     */
    @ImportExcelValidated(required = true, maxLength = 100)
    @Excel(name = "ui.data.column.mdmCycleSchStruConf.structureName")
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 周转月数
     */
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.mdmCycleSchStruConf.turnoverMonth")
    @ApiModelProperty(value = "周转月数", name = "turnoverMonth")
    @TableField(value = "TURNOVER_MONTH")
    private Integer turnoverMonth;

    /**
     * 最低硫化机台数
     */
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.mdmCycleSchStruConf.minVulcanizingMachine")
    @ApiModelProperty(value = "最低硫化机台数", name = "minVulcanizingMachine")
    @TableField(value = "MIN_VULCANIZING_MACHINE")
    private Integer minVulcanizingMachine;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(exist = false)
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(exist = false)
    private Integer month;
}
