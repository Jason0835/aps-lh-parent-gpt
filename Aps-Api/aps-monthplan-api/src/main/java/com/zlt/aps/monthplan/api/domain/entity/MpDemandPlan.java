package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpDemandPlan.java
 * 描    述：需求计划对象 t_mp_demand_plan
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "需求计划对象", description = "需求计划对象 ")
@Data
public class MpDemandPlan extends BaseEntity{

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.demandPlan.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.demandPlan.year")
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.demandPlan.month")
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /** 需求计划版本号 */
    @Excel(name = "ui.data.column.demandPlan.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本号", name = "monthPlanVersion")
    private String monthPlanVersion;

    /** 优先级(订单类型) */
    @Excel(name = "ui.data.column.demandPlan.orderPriority")
    @ApiModelProperty(value = "优先级(订单类型)", name = "orderPriority")
    private String orderPriority;

    /** 是否替换料 */
    @Excel(name = "ui.data.column.demandPlan.isAlternateMaterial")
    @ApiModelProperty(value = "是否替换料", name = "isAlternateMaterial")
    private String isAlternateMaterial;

    /** 产品品类 */
    @Excel(name = "ui.data.column.demandPlan.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    private String productTypeCode;

    /** 库位 */
    @Excel(name = "ui.data.column.demandPlan.locationType")
    @ApiModelProperty(value = "库位", name = "locationType")
    private String locationType;

    /** 品牌 */
    @Excel(name = "ui.data.column.demandPlan.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.demandPlan.isPrioritize")
    @ApiModelProperty(value = "供应链优先级", name = "isPrioritize")
    private String isPrioritize;

    /** 结构 */
    @Excel(name = "ui.data.column.demandPlan.structureName")
    @ApiModelProperty(value = "结构", name = "structureName")
    private String structureName;

    /** 主花纹 */
    @Excel(name = "ui.data.column.demandPlan.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    private String mainPattern;

    /** 物料编码 */
    @Excel(name = "ui.data.column.demandPlan.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.demandPlan.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /** 排产分类 */
    @Excel(name = "ui.data.column.demandPlan.productionType")
    @ApiModelProperty(value = "排产分类", name = "productionType")
    private String productionType;

    /** 年周号 */
    @Excel(name = "ui.data.column.demandPlan.yearWeek")
    @ApiModelProperty(value = "年周号", name = "yearWeek")
    private String yearWeek;

    /** 动平衡 */
    @Excel(name = "ui.data.column.demandPlan.isDynamicBalance")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    private String isDynamicBalance;

    /** 均匀性 */
    @Excel(name = "ui.data.column.demandPlan.isUniformity")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    private String isUniformity;

    /** 订单量 */
    @Excel(name = "ui.data.column.demandPlan.orderQty")
    @ApiModelProperty(value = "订单量", name = "orderQty")
    private Integer orderQty;

    /** 库存数 */
    @Excel(name = "ui.data.column.demandPlan.stockQty")
    @ApiModelProperty(value = "库存数", name = "stockQty")
    private Integer stockQty;

    /** 月底余量 */
    @Excel(name = "ui.data.column.demandPlan.plannedSurplus")
    @ApiModelProperty(value = "月底余量", name = "plannedSurplus")
    private Integer plannedSurplus;

    /** 排产净需求 */
    @Excel(name = "ui.data.column.demandPlan.netQty")
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    private Integer netQty;

    /** 是否排产 */
    @Excel(name = "ui.data.column.demandPlan.isProduction")
    @ApiModelProperty(value = "是否排产", name = "isProduction")
    private String isProduction;

    /** 净需求(含暂缓) */
    @Excel(name = "ui.data.column.demandPlan.postponeNetQty")
    @ApiModelProperty(value = "净需求(含暂缓)", name = "postponeNetQty")
    private Integer postponeNetQty;

    /** 净需求(不含暂缓) */
    @Excel(name = "ui.data.column.demandPlan.unPostponeNetQty")
    @ApiModelProperty(value = "净需求(不含暂缓)", name = "unPostponeNetQty")
    private Integer unPostponeNetQty;

    /** 高优先级 */
    @Excel(name = "ui.data.column.demandPlan.heightQty")
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    private Integer heightQty;

    /** 中优先级 */
    @Excel(name = "ui.data.column.demandPlan.midQty")
    @ApiModelProperty(value = "中优先级", name = "midQty")
    private Integer midQty;

    /** 暂缓订单 */
    @Excel(name = "ui.data.column.demandPlan.postponeQty")
    @ApiModelProperty(value = "暂缓订单", name = "postponeQty")
    private Integer postponeQty;

    /** 周期排产储备 */
    @Excel(name = "ui.data.column.demandPlan.cycleReserveQty")
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    private Integer cycleReserveQty;

    /** 常规储备 */
    @Excel(name = "ui.data.column.demandPlan.conventionReserveQty")
    @ApiModelProperty(value = "常规储备", name = "conventionReserveQty")
    private Integer conventionReserveQty;

    /** 是否满足最小投产量 */
    @Excel(name = "ui.data.column.demandPlan.isReachMinProductionQty")
    @ApiModelProperty(value = "是否满足最小投产量", name = "isReachMinProductionQty")
    private String isReachMinProductionQty;

    /** 最小投产量值 */
    @Excel(name = "ui.data.column.demandPlan.minProductionQty")
    @ApiModelProperty(value = "最小投产量值", name = "minProductionQty")
    private Integer minProductionQty;

    /** 计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟 */
    @Excel(name = "ui.data.column.demandPlan.planType")
    @ApiModelProperty(value = "计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟", name = "planType")
    private String planType;

    /** MES物料编号 */
    @Excel(name = "ui.data.column.demandPlan.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编号", name = "mesMaterialCode")
    private String mesMaterialCode;

    /** 渠道 */
    @Excel(name = "ui.data.column.demandPlan.channel")
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /** 英寸 */
    @Excel(name = "ui.data.column.demandPlan.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    private String proSize;

    /** 规格 */
    @Excel(name = "ui.data.column.demandPlan.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /** 花纹 */
    @Excel(name = "ui.data.column.demandPlan.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /** 层级 */
    @Excel(name = "ui.data.column.demandPlan.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    private String hierarchy;

    /** 速级 */
    @Excel(name = "ui.data.column.demandPlan.speed")
    @ApiModelProperty(value = "速级", name = "speed")
    private String speed;

    /** 是否重要客户 0 不重要 1 重要 */
    @Excel(name = "ui.data.column.demandPlan.isImportantCustom")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    private String isImportantCustom;

    /** 是否必保计划 0 不必保 1 必保 */
    @Excel(name = "ui.data.column.demandPlan.isEnsurePlan")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    private String isEnsurePlan;

    /** 是否紧急订单 0 不紧急 1 紧急 */
    @Excel(name = "ui.data.column.demandPlan.isEmergency")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    private String isEmergency;

    /** 是否欠产（0：默认不是，1：是） */
    @Excel(name = "ui.data.column.demandPlan.isDebitPlan", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    private String isDebitPlan;

    /** 期望交期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.demandPlan.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    private Date deliveryDateDue;

    /** 是否EXCEL导入（0：默认不是，1：是） */
    @Excel(name = "ui.data.column.demandPlan.isImport", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    private String isImport;

}