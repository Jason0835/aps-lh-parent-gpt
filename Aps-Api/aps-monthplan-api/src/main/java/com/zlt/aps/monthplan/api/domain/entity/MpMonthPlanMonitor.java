package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanMonitor.java
 * 描    述：月度硫化监控对象 t_mp_month_plan_monitor
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "月度硫化监控对象", description = "月度硫化监控对象 ")
@Data
@TableName(value = "T_MP_MONTH_PLAN_MONITOR")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpMonthPlanMonitor extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 年月:YYYYMM */
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /** 需求计划版本 */
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 排产版本号 */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 产品品类 数据字典：biz_product_type  全钢 PCR 半钢 */
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type  全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 产品状态 */
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    @TableField(value = "PRODUCT_STATUS")
    private String productStatus;

    /** 产品结构 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 成型机台 */
    @ApiModelProperty(value = "成型机台", name = "cxMachine")
    @TableField(exist = false)
    private String cxMachine;

    /** 硫化机台 */
    @ApiModelProperty(value = "硫化机台", name = "lhMachine")
    @TableField(exist = false)
    private String lhMachine;

    /** MES物料编码 */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 主物料(胎胚号) */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 品牌 */
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 规格 */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 主花纹 */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 花纹 */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 模具数格式：2-0 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.mouldQty")
    @ApiModelProperty(value = "模具数", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private String mouldQty;

    /** 净需求量 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.netDemandQty")
    @ApiModelProperty(value = "净需求量", name = "netDemandQty")
    @TableField(value = "NET_DEMAND_QTY")
    private Integer netDemandQty;

    /** 排产量 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.scheduleQty")
    @ApiModelProperty(value = "排产量", name = "scheduleQty")
    @TableField(value = "SCHEDULE_QTY")
    private Integer scheduleQty;

    /** 上机日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.onboardDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "上机日期", name = "onboardDate")
    @TableField(value = "ONBOARD_DATE")
    private Date onboardDate;

    /** 不合格数量 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.unqualifiedQty")
    @ApiModelProperty(value = "不合格数量", name = "unqualifiedQty")
    @TableField(value = "UNQUALIFIED_QTY")
    private Integer unqualifiedQty;

    /** 累计生产量 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.productionQty")
    @ApiModelProperty(value = "累计生产量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 硫化余量 */
    @Excel(name = "ui.data.column.mpMonthPlanMonitor.lhMargin")
    @ApiModelProperty(value = "硫化余量", name = "lhMargin")
    @TableField(value = "LH_MARGIN")
    private Integer lhMargin;

    /** 预计收尾天数 */
    @ApiModelProperty(value = "预计收尾天数", name = "expectedCloseDay")
    @TableField(value = "EXPECTED_CLOSE_DAY")
    private Integer expectedCloseDay;

    /** 预计收尾时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "预计收尾时间", name = "expectedCloseDate")
    @TableField(value = "EXPECTED_CLOSE_DATE")
    private Date expectedCloseDate;

    /** 计划收尾时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划收尾时间", name = "planCloseDate")
    @TableField(value = "PLAN_CLOSE_DATE")
    private Date planCloseDate;

    /** 差异天数 */
    @ApiModelProperty(value = "差异天数", name = "diffDay")
    @TableField(value = "DIFF_DAY")
    private Integer diffDay;


}