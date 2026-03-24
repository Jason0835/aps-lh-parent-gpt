package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMonthPlanSurplus.java
 * 描    述：月度计划外胎汇总对象 t_lh_month_plan_surplus
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "月度计划外胎汇总对象", description = "月度计划外胎汇总对象 ")
@Data
@TableName(value = "T_LH_MONTH_PLAN_SURPLUS")
public class LhMonthPlanSurplus extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 生产排程记录主计划版本号,年+月+日+01，02 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.monthPlanApsVersion")
    @ApiModelProperty(value = "生产排程记录主计划版本号,年+月+日+01，02", name = "monthPlanApsVersion")
    @TableField(value = "MONTH_PLAN_APS_VERSION")
    private String monthPlanApsVersion;

    /** 月度计划版本 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.monthPlanVersion")
    @ApiModelProperty(value = "月度计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 月度计划所属年份 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.year")
    @ApiModelProperty(value = "月度计划所属年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月度计划所属月份 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.month")
    @ApiModelProperty(value = "月度计划所属月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编号 */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 规格代码 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 月度计划量 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.monthPlanQty")
    @ApiModelProperty(value = "月度计划量", name = "monthPlanQty")
    @TableField(value = "MONTH_PLAN_QTY")
    private Integer monthPlanQty;

    /** 外胎月结库存，月结库存获取时更新到该字段 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.lastMonthStock")
    @ApiModelProperty(value = "月结库存", name = "lastMonthStock")
    @TableField(value = "LAST_MONTH_STOCK")
    private Integer lastMonthStock;

    /** 外胎不良数，若不良接口可以提供SAP+胎胚，则接口同步更新该字段,如果给不了由人为输入确认同步更新 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.specBadQty")
    @ApiModelProperty(value = "不良数", name = "specBadQty")
    @TableField(value = "SPEC_BAD_QTY")
    private Integer specBadQty;

    /** 月度硫化完成量 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.monthFinishQty")
    @ApiModelProperty(value = "硫化完成量", name = "monthFinishQty")
    @TableField(value = "MONTH_FINISH_QTY")
    private Integer monthFinishQty;

    /** 外胎月剩余量 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.monthRemainQty")
    @ApiModelProperty(value = "月剩余量", name = "monthRemainQty")
    @TableField(value = "MONTH_REMAIN_QTY")
    private Integer monthRemainQty;

    /** 数据来源：0&gt;主计划；1&gt;APS插单。主计划更新插单数据不删除 */
    @Excel(name = "ui.data.column.lhMonthPlanSurplus.dataSource")
    @ApiModelProperty(value = "数据来源：0&gt;主计划；1&gt;APS插单。主计划更新插单数据不删除", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /**
     * 胎胚代码
     */
    @TableField(exist = false)
    private String embryoCode;

    /** BOM信息中所使用的版本 */
    @TableField(exist = false)
    private String bomDataVersion;

}