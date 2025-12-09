package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawMaterialRequirePlan.java
 * 描    述：原材料需求计划对象 t_raw_material_require_plan
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "原材料需求计划对象", description = "原材料需求计划对象 ")
@Data
@TableName(value = "T_RAW_MATERIAL_REQUIRE_PLAN")
public class RawMaterialRequirePlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 所属年份 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.year")
    @ApiModelProperty(value = "所属年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 所属月份 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.month")
    @ApiModelProperty(value = "所属月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 生产物料编号 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 分类 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.category")
    @ApiModelProperty(value = "分类", name = "category")
    @TableField(value = "CATEGORY")
    private String category;

    /** 当月需求量 */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.curMonthQty")
    @ApiModelProperty(value = "当月需求量", name = "curMonthQty")
    @TableField(value = "CUR_MONTH_QTY")
    private BigDecimal curMonthQty;

    /** 当月EUDR */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.curMonthRudrQty")
    @ApiModelProperty(value = "当月EUDR", name = "curMonthRudrQty")
    @TableField(value = "CUR_MONTH_RUDR_QTY")
    private BigDecimal curMonthRudrQty;

    /** 次月需求量(T月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.tMonthQty")
    @ApiModelProperty(value = "次月需求量(T月)", name = "tMonthQty")
    @TableField(value = "T_MONTH_QTY")
    private BigDecimal tMonthQty;

    /** 次月EUDR(T月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.tMonthEudrQty")
    @ApiModelProperty(value = "次月EUDR(T月)", name = "tMonthEudrQty")
    @TableField(value = "T_MONTH_EUDR_QTY")
    private BigDecimal tMonthEudrQty;

    /** 次次月需求量(T+1月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t1MonthQty")
    @ApiModelProperty(value = "次次月需求量(T+1月)", name = "t1MonthQty")
    @TableField(value = "T1_MONTH_QTY")
    private BigDecimal t1MonthQty;

    /** 次次月EUDR(T+1月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t1MonthEudrQty")
    @ApiModelProperty(value = "次次月EUDR(T+1月)", name = "t1MonthEudrQty")
    @TableField(value = "T1_MONTH_EUDR_QTY")
    private BigDecimal t1MonthEudrQty;

    /** 次次次月需求量(T+2月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t2MonthQty")
    @ApiModelProperty(value = "次次次月需求量(T+2月)", name = "t2MonthQty")
    @TableField(value = "T2_MONTH_QTY")
    private BigDecimal t2MonthQty;

    /** 次次次月EUDR(T+2月) */
    @Excel(name = "ui.data.column.rawMaterialRequirePlan.t2MonthEudrQty")
    @ApiModelProperty(value = "次次次月EUDR(T+2月)", name = "t2MonthEudrQty")
    @TableField(value = "T2_MONTH_EUDR_QTY")
    private BigDecimal t2MonthEudrQty;


}