package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionPlan.java
 * 描    述：S2-0606.排产结果-未排产计划对象 T_MP_PROC_NO_PRODUCTION_PLAN
 *@author yelq
 *@date 2026-01-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "S2-0606.排产结果-未排产计划对象", description = "S2-0606.排产结果-未排产计划对象 ")
@Data
@TableName(value = "T_MP_PROC_NO_PRODUCTION_PLAN")
public class MonthPlanNoProductionPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 需求计划版本 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 排产版本号 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productionVersion")
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 需求计划ID */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.monthPlanId")
    @ApiModelProperty(value = "需求计划ID", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /** 产品品类 数据字典：biz_product_type  TBR 全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productTypeCode")
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type  TBR 全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 类型 01 内销 02 外销 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.locationType")
    @ApiModelProperty(value = "类型 01 内销 02 外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 品牌 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 供应链优先级 1 优先 0 不优先 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isPrioritize")
    @ApiModelProperty(value = "供应链优先级 1 优先 0 不优先", name = "isPrioritize")
    @TableField(value = "IS_PRIORITIZE")
    private String isPrioritize;

    /** 产品结构 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 主物料(胎胚号) */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;


    /**
     * 英寸
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 规格 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 花纹 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 排产分类 数据字典：biz_product_characteristics 1 主销产品 2 常规产品 3 周期排产产品 4 波动性产品 5 按单排产产品 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productionType")
    @ApiModelProperty(value = "排产分类 数据字典：biz_product_characteristics 1 主销产品 2 常规产品 3 周期排产产品 4 波动性产品 5 按单排产产品", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

    /** 施工阶段 0 无工艺 1 试制 2 量试 3 正式 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.constructionStage")
    @ApiModelProperty(value = "施工阶段 0 无工艺 1 试制 2 量试 3 正式", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /** 年周号 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.yearWeek")
    @ApiModelProperty(value = "年周号", name = "yearWeek")
    @TableField(value = "YEAR_WEEK")
    private String yearWeek;

    /** 动平衡要求 0 不要求 1 要求 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isDynamicBalance")
    @ApiModelProperty(value = "动平衡要求 0 不要求 1 要求", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /** 是否试制量试计划 0 不是 1 是 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isTrialPlan")
    @ApiModelProperty(value = "是否试制量试计划 0 不是 1 是", name = "isTrialPlan")
    @TableField(value = "IS_TRIAL_PLAN")
    private String isTrialPlan;

    /** 均匀性要求 0 不要求 1 要求 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isUniformity")
    @ApiModelProperty(value = "均匀性要求 0 不要求 1 要求", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /** 是否排产 0 不排产 1 排产 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isProduction")
    @ApiModelProperty(value = "是否排产 0 不排产 1 排产", name = "isProduction")
    @TableField(value = "IS_PRODUCTION")
    private String isProduction;

    /** 订单量 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.orderQty")
    @ApiModelProperty(value = "订单量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Integer orderQty;

    /** 库存数量(成品库存总数) */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.stockQty")
    @ApiModelProperty(value = "库存数量(成品库存总数)", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /** 月均销量 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.averageSaleQty")
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Integer averageSaleQty;

    /** 月底余量 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.plannedSurplus")
    @ApiModelProperty(value = "月底余量", name = "plannedSurplus")
    @TableField(value = "PLANNED_SURPLUS")
    private Integer plannedSurplus;

    /** 排产净需求 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.netQty")
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /** 净需求(含暂缓) */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.postponeNetQty")
    @ApiModelProperty(value = "净需求(含暂缓)", name = "postponeNetQty")
    @TableField(value = "POSTPONE_NET_QTY")
    private Integer postponeNetQty;

    /** 净需求(不含暂缓) */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.unPostponeNetQty")
    @ApiModelProperty(value = "净需求(不含暂缓)", name = "unPostponeNetQty")
    @TableField(value = "UN_POSTPONE_NET_QTY")
    private Integer unPostponeNetQty;

    /** 高优先级数量 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.heightQty")
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /** 中优先级 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.midQty")
    @ApiModelProperty(value = "中优先级", name = "midQty")
    @TableField(value = "MID_QTY")
    private Integer midQty;

    /** 暂缓订单 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.postponeQty")
    @ApiModelProperty(value = "暂缓订单", name = "postponeQty")
    @TableField(value = "POSTPONE_QTY")
    private Integer postponeQty;

    /** 周期排产储备 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.cycleReserveQty")
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    @TableField(value = "CYCLE_RESERVE_QTY")
    private Integer cycleReserveQty;

    /** 常规储备 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.conventionReserveQty")
    @ApiModelProperty(value = "常规储备", name = "conventionReserveQty")
    @TableField(value = "CONVENTION_RESERVE_QTY")
    private Integer conventionReserveQty;

    /** 高优先级需求(含损耗) */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.heightLossQty")
    @ApiModelProperty(value = "高优先级需求(含损耗)", name = "heightLossQty")
    @TableField(value = "HEIGHT_LOSS_QTY")
    private Integer heightLossQty;

    /** 实际生产需求(含损耗，排除高优先级损耗) */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.factProdReqQty")
    @ApiModelProperty(value = "实际生产需求(含损耗，排除高优先级损耗)", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Integer factProdReqQty;

    /** 未排产数量 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.unProductionQty")
    @ApiModelProperty(value = "未排产数量", name = "unProductionQty")
    @TableField(value = "UN_PRODUCTION_QTY")
    private Integer unProductionQty;

    /** 未排产原因 */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.reason")
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;
    /**
     * 生产实际排产量
     */
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(exist = false)
    private Long totalQty;

}