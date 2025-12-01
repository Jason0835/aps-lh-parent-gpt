package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxEmbryoMonthPlanSurplus.java
 * 描    述：成型工序胎胚计划量汇总表对象 t_cx_embryo_month_plan_surplus
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型工序胎胚计划量汇总表对象", description = "成型工序胎胚计划量汇总表对象 ")
@Data
@TableName(value = "T_CX_EMBRYO_MONTH_PLAN_SURPLUS")
public class CxEmbryoMonthPlanSurplus extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

     /** 主计划版本号 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.monthPlanVersion")
    @ApiModelProperty(value = "主计划版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 主计划所属年份 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.year")
    @ApiModelProperty(value = "主计划所属年份", name = "year")
    @TableField(value = "YEAR")
    private String year;

    /** 主计划所属月份 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.month")
    @ApiModelProperty(value = "主计划所属月份", name = "month")
    @TableField(value = "MONTH")
    private String month;

    /** BOM信息中所使用的版本 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.bomDataVersion")
    @ApiModelProperty(value = "BOM信息中所使用的版本", name = "bomDataVersion")
    @TableField(value = "BOM_DATA_VERSION")
    private String bomDataVersion;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.materialCode")
    @ApiModelProperty(value = "胎胚代码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 月度计划量 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.monthPlanQty")
    @ApiModelProperty(value = "月度计划量", name = "monthPlanQty")
    @TableField(value = "MONTH_PLAN_QTY")
    private Integer monthPlanQty;

    /** 胎胚月结库存，月结库存获取时更新到该字段 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.lastMonthStock")
    @ApiModelProperty(value = "胎胚月结库存，月结库存获取时更新到该字段", name = "lastMonthStock")
    @TableField(value = "LAST_MONTH_STOCK")
    private Integer lastMonthStock;

    /** 月度完成量 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.monthFinishQty")
    @ApiModelProperty(value = "月度完成量", name = "monthFinishQty")
    @TableField(value = "MONTH_FINISH_QTY")
    private Integer monthFinishQty;

    /** 月剩余量 */
    @Excel(name = "ui.data.column.cxEmbryoMonthPlanSurplus.monthRemainQty")
    @ApiModelProperty(value = "月剩余量", name = "monthRemainQty")
    @TableField(value = "MONTH_REMAIN_QTY")
    private Integer monthRemainQty;

    /**
     * 数据来源
     */
    @ApiModelProperty(value = "数据来源：0：主计划；1:APS插单；插单数据主计划版本更新不进行删除")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;


}