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
 * 文件名称：MdmMonCycleSchStruConf.java
 * 描    述：月周期排产结构配置对象 t_mdm_mon_cycle_sch_stru_conf
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
@ApiModel(value = "月周期排产结构配置对象", description = "月周期排产结构配置对象 ")
@Data
@TableName(value = "t_dp_month_cycle_struct_config")
public class MdmMonCycleSchStruConf extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true, min = 1, max = 9999)
    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true, min = 1, max = 12)
    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 结构
     */
    @ImportExcelValidated(required = true, maxLength = 100)
    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.structureName")
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 周转月数
     */
    @ImportExcelValidated(required = true, digits = true, min = 0)
    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.turnoverMonth", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "周转月数", name = "turnoverMonth")
    @TableField(value = "TURNOVER_MONTH")
    private Integer turnoverMonth;

    /**
     * 最低硫化机台数
     */
    @ImportExcelValidated(required = true, digits = true, min = 0)
    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.minVulcanizingMachine", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "最低硫化机台数", name = "minVulcanizingMachine")
    @TableField(value = "MIN_VULCANIZING_MACHINE")
    private Integer minVulcanizingMachine;

    @ApiModelProperty("订单来源：01：正常 02：产量预测：03：实单模拟")
    @TableField("SOURCE_TYPE")
    private String sourceType;

    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.updateDate")
    @ApiModelProperty(value = "更新日期", name = "updateDate")
    @TableField(exist = false)
    private String updateDate;


}
