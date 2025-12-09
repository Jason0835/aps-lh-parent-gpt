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
 * 文件名称：MonthPlanProductionResultDetail.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果明细日志对象 t_mp_mould_day_detail_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20251208
 */

@Data
@TableName(value = "T_MP_MOULD_DAY_DETAIL_LOG")
@ApiModel(value = "生产计划排产结果明细对象", description = "生产计划排产结果明细对象")
public class MonthPlanProductionResultDetailLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.cxCapacity.planType", dictType = "biz_plan_type")
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 需求计划ID
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.monthPlanId")
    @ApiModelProperty(value = "需求计划ID", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 产品品类
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * MES物料编码
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 主物料
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.mainMaterialDesc")
    @ApiModelProperty(value = "主物料", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.materialDesc")
    @ApiModelProperty(value = "规格描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private Integer constructionStage;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 主花纹
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /**
     * 模具编码(型腔模号)
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.mouldCode")
    @ApiModelProperty(value = "模具编码(型腔模号)", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.embryoCode")
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 高优先级数量
     */
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Long heightQty;

    /**
     * 月均销量
     */
    @ApiModelProperty(value = "月均销量", name = "averageQty")
    @TableField(value = "AVERAGE_QTY")
    private Long averageQty;
    /**
     * 库销比
     */
    @ApiModelProperty(value = "库销比", name = "inventorySalesRatio")
    @TableField(value = "INVENTORY_SALES_RATIO")
    private BigDecimal inventorySalesRatio;

    /**
     * 日硫化量(单模)
     */
    @ApiModelProperty(value = "日硫化量(单模)", name = "dayVulcanizationQty")
    @TableField(value = "DAY_VULCANIZATION_QTY")
    private Long dayVulcanizationQty;

    /**
     * 排产顺序
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.productionSequence")
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    @TableField(value = "PRODUCTION_SEQUENCE")
    private Long productionSequence;

    /**
     * 单条硫化时间(包含增加间隔)-调整时使用
     */
    @ApiModelProperty(value = "单条硫化时间(包含增加间隔)-到秒", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private BigDecimal curingTime;
    /**
     * 生产需求计划
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.prodReqPlan")
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Long prodReqPlan;

    /**
     * 实际生产需求
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.factProdReqQty")
    @ApiModelProperty(value = "实际生产需求", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Long factProdReqQty;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.totalQty")
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Long totalQty;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.beginDay")
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    @TableField(value = "BEGIN_DAY")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.endDay")
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day1")
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Long day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day2")
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Long day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day3")
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Long day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day4")
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Long day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day5")
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Long day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day6")
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Long day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day7")
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Long day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day8")
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Long day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day9")
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Long day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day10")
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Long day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day11")
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Long day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day12")
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Long day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day13")
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Long day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day14")
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Long day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day15")
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Long day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day16")
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Long day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day17")
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Long day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day18")
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Long day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day19")
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Long day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day20")
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Long day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day21")
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Long day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day22")
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Long day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day23")
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Long day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day24")
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Long day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day25")
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Long day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day26")
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Long day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day27")
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Long day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day28")
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Long day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day29")
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Long day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day30")
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Long day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.day31")
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Long day31;

    /**
     * 硫化总工时
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.totalVulcanizationMinutes")
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES")
    private BigDecimal totalVulcanizationMinutes;

    /**
     * 显示顺序
     */
    @Excel(name = "ui.data.column.productionResultDetailLog.displaySeq")
    @ApiModelProperty(value = "显示顺序", name = "displaySeq")
    @TableField(value = "DISPLAY_SEQ")
    private Integer displaySeq;

}