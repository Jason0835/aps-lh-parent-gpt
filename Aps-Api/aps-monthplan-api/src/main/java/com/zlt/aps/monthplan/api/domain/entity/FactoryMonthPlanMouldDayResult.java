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
 * 文件名称：FactoryMonthPlanMouldDayResult.java
 * 描    述：S2-0604.排产结果-生产计划排产结果对象 t_mp_moulding_day_result
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-31
 */

@Data
@TableName(value = "T_MP_MOULDING_DAY_RESULT")
@ApiModel(value = "S2-0604.排产结果-生产计划排产结果对象", description = "S2-0604.排产结果-生产计划排产结果对象 ")
public class FactoryMonthPlanMouldDayResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 年月:YYYYMM
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.yearMonth")
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /**
     * 需求计划版本
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 排产版本号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.productionVersion")
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.planType", dictType = "biz_plan_type")
    @ApiModelProperty(value = "计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 产品品类 数据字典：biz_product_type  全钢 PCR 半钢
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type  全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * MES物料编码
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 英寸
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /**
     * 产品分类
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.productCategory", dictType = "product_category")
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    @TableField(value = "PRODUCT_CATEGORY")
    private String productCategory;

    /**
     * 产品状态
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.productStatus", dictType = "trial_status")
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    @TableField(value = "PRODUCT_STATUS")
    private String productStatus;

    /**
     * 结构类型 01 周期结构 02 常规结构
     */
    @ApiModelProperty(value = "结构类型", name = "structureType")
    @TableField(value = "STRUCTURE_TYPE")
    private String structureType;

    /**
     * 排产分类
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.productionType", dictType = "biz_schedule_type")
    @ApiModelProperty(value = "排产分类", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

    /**
     * 主物料(胎胚号)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /**
     * 施工阶段 00 无工艺 01 试制 02 量试 03 正式
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段 00 无工艺 01 试制 02 量试 03 正式", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;
    /**
     * 是否零度材料
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.isZeroRack")
    @ApiModelProperty(value = "是否零度材料", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    /**
     * 制造示方书号
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.embryoNo")
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /**
     * 文字示方书号
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.textNo")
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    /**
     * 硫化示方书号
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.lhNo")
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;
    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 主花纹
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 型腔数量(同主花纹的模具数量)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.mouldCavityQty")
    @ApiModelProperty(value = "型腔数量(同主花纹的模具数量)", name = "mouldCavityQty")
    @TableField(value = "MOULD_CAVITY_QTY")
    private Integer mouldCavityQty;

    /**
     * 活块数量(同主花纹的物料模具数量)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.typeBlockQty")
    @ApiModelProperty(value = "活块数量(同主花纹的物料模具数量)", name = "typeBlockQty")
    @TableField(value = "TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

    /**
     * 动平衡数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.dynamicBalanceQty")
    @ApiModelProperty(value = "动平衡数量", name = "dynamicBalanceQty")
    @TableField(value = "DYNAMIC_BALANCE_QTY")
    private String dynamicBalanceQty;

    /**
     * 均匀性数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.uniformityQty")
    @ApiModelProperty(value = "均匀性数量", name = "uniformityQty")
    @TableField(value = "UNIFORMITY_QTY")
    private Integer uniformityQty;

    /**
     * 高优先级数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.heightQty")
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /**
     * 月均销量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.averageSaleQty")
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Integer averageSaleQty;

    /**
     * 库销比
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.inventorySalesRatio")
    @ApiModelProperty(value = "库销比", name = "inventorySalesRatio")
    @TableField(value = "INVENTORY_SALES_RATIO")
    private BigDecimal inventorySalesRatio;

    /**
     * 日硫化量(单模)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.dayVulcanizationQty")
    @ApiModelProperty(value = "日硫化量(单模)", name = "dayVulcanizationQty")
    @TableField(value = "DAY_VULCANIZATION_QTY")
    private Integer dayVulcanizationQty;

    /**
     * 成型机编码 多个以,分隔
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.cxMachineCode")
    @ApiModelProperty(value = "成型机编码 多个以,分隔", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 模具使用变化信息如2-4-2,或是2-4或是2
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.mouldChangeInfo")
    @ApiModelProperty(value = "模具使用变化信息如2-4-2,或是2-4或是2", name = "mouldChangeInfo")
    @TableField(value = "MOULD_CHANGE_INFO")
    private String mouldChangeInfo;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.isImport", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private String isImport;

    /**
     * 排产顺序
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.productionSequence")
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    @TableField(value = "PRODUCTION_SEQUENCE")
    private Long productionSequence;

    /**
     * 单条硫化时间(包含增加间隔)-调整时使用
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.curingTime")
    @ApiModelProperty(value = "单条硫化时间(包含增加间隔)-调整时使用", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /**
     * 生产需求计划
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.prodReqPlan")
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Integer prodReqPlan;

    /**
     * 实际生产需求(含损耗)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.factProdReqQty")
    @ApiModelProperty(value = "实际生产需求(含损耗)", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Integer factProdReqQty;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.totalQty")
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Integer totalQty;

    /**
     * 高优先级排产数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.heightProductionQty")
    @ApiModelProperty(value = "高优先级排产数量", name = "heightProductionQty")
    @TableField(value = "HEIGHT_PRODUCTION_QTY")
    private Integer heightProductionQty;

    /**
     * 中优先级排产数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.midProductionQty")
    @ApiModelProperty(value = "中优先级排产数量", name = "midProductionQty")
    @TableField(value = "MID_PRODUCTION_QTY")
    private Integer midProductionQty;

    /**
     * 周期排产储备排产数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.cycleProductionQty")
    @ApiModelProperty(value = "周期排产储备排产数量", name = "cycleProductionQty")
    @TableField(value = "CYCLE_PRODUCTION_QTY")
    private Integer cycleProductionQty;

    /**
     * 常规储备排产数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.conventionProductionQty")
    @ApiModelProperty(value = "常规储备排产数量", name = "conventionProductionQty")
    @TableField(value = "CONVENTION_PRODUCTION_QTY")
    private Integer conventionProductionQty;

    /**
     * 暂缓订单排产数量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.postponeProductionQty")
    @ApiModelProperty(value = "暂缓订单排产数量", name = "postponeProductionQty")
    @TableField(value = "POSTPONE_PRODUCTION_QTY")
    private Integer postponeProductionQty;

    /**
     * 差异量(未排产数量)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.differenceQty")
    @ApiModelProperty(value = "差异量(未排产数量)", name = "differenceQty")
    @TableField(value = "DIFFERENCE_QTY")
    private Integer differenceQty;

    /**
     * 未排产原因
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.reason")
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.beginDay")
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    @TableField(value = "BEGIN_DAY")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.endDay")
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day1")
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Integer day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day2")
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Integer day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day3")
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Integer day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day4")
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Integer day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day5")
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Integer day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day6")
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Integer day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day7")
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Integer day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day8")
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Integer day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day9")
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Integer day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day10")
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Integer day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day11")
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Integer day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day12")
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Integer day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day13")
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Integer day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day14")
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Integer day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day15")
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Integer day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day16")
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Integer day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day17")
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Integer day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day18")
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Integer day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day19")
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Integer day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day20")
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Integer day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day21")
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Integer day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day22")
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Integer day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day23")
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Integer day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day24")
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Integer day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day25")
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Integer day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day26")
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Integer day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day27")
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Integer day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day28")
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Integer day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day29")
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Integer day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day30")
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Integer day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.day31")
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Integer day31;

    /**
     * 硫化总工时
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.totalVulcanizationMinutes")
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES")
    private BigDecimal totalVulcanizationMinutes;

    /**
     * 显示顺序
     */
    @Excel(name = "ui.data.column.factoryMonthPlanMouldDayResult.displaySeq")
    @ApiModelProperty(value = "显示顺序", name = "displaySeq")
    @TableField(value = "DISPLAY_SEQ")
    private Integer displaySeq;


    /**
     * 分组|*|主花纹
     * TBR 为结构
     *
     * @return
     */
    public String getGroupAndMainPattern() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, structureName, mainPattern);
    }

}