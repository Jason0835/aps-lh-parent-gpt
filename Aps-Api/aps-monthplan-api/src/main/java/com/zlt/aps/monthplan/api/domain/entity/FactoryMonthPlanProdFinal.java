package com.zlt.aps.monthplan.api.domain.entity;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.monthplan.api.domain.vo.SinglePlanInfoHelper;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProdFinal.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果对象 t_mp_month_plan_prod_final
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-14
 */

@Data
@TableName(value = "T_MP_MONTH_PLAN_PROD_FINAL")
@ApiModel(value = "分厂月生产计划排产结果-生产计划排产结果对象", description = "分厂月生产计划排产结果-生产计划排产结果对象 ")
public class FactoryMonthPlanProdFinal extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 排产制造单号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productionNo")
    @ApiModelProperty(value = "排产制造单号", name = "productionNo")
    @TableField(value = "PRODUCTION_NO")
    private String productionNo;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 年月:YYYYMM
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.yearMonth")
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.monthPlanVersion")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productionVersion")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.constructionStage", dictType = "biz_construction_stage")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private Integer constructionStage;

    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldMethod", dictType = "MACHINE_TYPE")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    @TableField(value = "SPEC_CODE_INFO")
    private String specCodeInfo;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productTypeCode", dictType = "biz_product_name")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 等级码
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode", hidden = true)
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName", hidden = true)
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.specCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.embryoCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;

    /**
     * 模数
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.mouldQty")
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "模数", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 模具编码集合，多个以,分隔
     */
    @ApiModelProperty(value = "模具编码集合，多个以,分隔", name = "mouldInfo", hidden = true)
    @TableField(value = "MOULD_INFO")
    private String mouldInfo;

    /**
     * 合并信息json串
     */
    @ApiModelProperty(value = "合并信息json串", name = "mergeInfo", hidden = true)
    @TableField(value = "MERGE_INFO")
    private String mergeInfo;

    /**
     * 是否有交期（0：默认没有，1：有）
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.isDeliveryDate", dictType = "biz_yes_no")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "是否有交期", name = "isDeliveryDate")
    @TableField(value = "IS_DELIVERY_DATE")
    private Integer isDeliveryDate;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.isImport", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 单条硫化时间(包含增加间隔)-调整时使用
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.curingTime")
    @ApiModelProperty(value = "单条硫化时间(包含增加间隔)-到秒", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private BigDecimal curingTime;

    /**
     * 生产需求计划
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.prodReqPlan", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Long prodReqPlan;

    /**
     * 实际生产需求(含损耗)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.factProdReqQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true)
    @ApiModelProperty(value = "实际生产需求(含损耗)", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Long factProdReqQty;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.totalQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Long totalQty;

    /**
     * 差异量(未排产数量)
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.differenceQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "差异量(未排产数量)", name = "differenceQty")
    @TableField(value = "DIFFERENCE_QTY")
    private Long differenceQty;

    /**
     * 未排产原因
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.reason")
    @ImportExcelValidated(maxLength = 1000)
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.beginDate")
    @ImportExcelValidated(min = 1, max = 31)
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE", updateStrategy = FieldStrategy.IGNORED)
    private Integer beginDate;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.endDay")
    @ImportExcelValidated(min = 1, max = 31)
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY", updateStrategy = FieldStrategy.IGNORED)
    private Integer endDay;
    /**
     * 备注说明
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
    /**
     * PRE_DAY_1
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay1")
    @ApiModelProperty(value = "PRE_DAY_1", name = "preDay1", hidden = true)
    @TableField(value = "PRE_DAY_1")
    private Long preDay1;

    /**
     * PRE_DAY_2
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay2")
    @ApiModelProperty(value = "PRE_DAY_2", name = "preDay2", hidden = true)
    @TableField(value = "PRE_DAY_2")
    private Long preDay2;

    /**
     * PRE_DAY_3
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay3")
    @ApiModelProperty(value = "PRE_DAY_3", name = "preDay3", hidden = true)
    @TableField(value = "PRE_DAY_3")
    private Long preDay3;

    /**
     * PRE_DAY_4
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay4")
    @ApiModelProperty(value = "PRE_DAY_4", name = "preDay4", hidden = true)
    @TableField(value = "PRE_DAY_4")
    private Long preDay4;

    /**
     * PRE_DAY_5
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay5")
    @ApiModelProperty(value = "PRE_DAY_5", name = "preDay5", hidden = true)
    @TableField(value = "PRE_DAY_5")
    private Long preDay5;

    /**
     * PRE_DAY_6
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay6")
    @ApiModelProperty(value = "PRE_DAY_6", name = "preDay6", hidden = true)
    @TableField(value = "PRE_DAY_6")
    private Long preDay6;

    /**
     * PRE_DAY_7
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay7")
    @ApiModelProperty(value = "PRE_DAY_7", name = "preDay7", hidden = true)
    @TableField(value = "PRE_DAY_7")
    private Long preDay7;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day1", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1", updateStrategy = FieldStrategy.IGNORED)
    private Long day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day2", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2", updateStrategy = FieldStrategy.IGNORED)
    private Long day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day3", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3", updateStrategy = FieldStrategy.IGNORED)
    private Long day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day4", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4", updateStrategy = FieldStrategy.IGNORED)
    private Long day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day5", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5", updateStrategy = FieldStrategy.IGNORED)
    private Long day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day6", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6", updateStrategy = FieldStrategy.IGNORED)
    private Long day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day7", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7", updateStrategy = FieldStrategy.IGNORED)
    private Long day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day8", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8", updateStrategy = FieldStrategy.IGNORED)
    private Long day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day9", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9", updateStrategy = FieldStrategy.IGNORED)
    private Long day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day10", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10", updateStrategy = FieldStrategy.IGNORED)
    private Long day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day11", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11", updateStrategy = FieldStrategy.IGNORED)
    private Long day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day12", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12", updateStrategy = FieldStrategy.IGNORED)
    private Long day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day13", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13", updateStrategy = FieldStrategy.IGNORED)
    private Long day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day14", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14", updateStrategy = FieldStrategy.IGNORED)
    private Long day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day15", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15", updateStrategy = FieldStrategy.IGNORED)
    private Long day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day16", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16", updateStrategy = FieldStrategy.IGNORED)
    private Long day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day17", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17", updateStrategy = FieldStrategy.IGNORED)
    private Long day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day18", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18", updateStrategy = FieldStrategy.IGNORED)
    private Long day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day19", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19", updateStrategy = FieldStrategy.IGNORED)
    private Long day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day20", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20", updateStrategy = FieldStrategy.IGNORED)
    private Long day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day21", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21", updateStrategy = FieldStrategy.IGNORED)
    private Long day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day22", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22", updateStrategy = FieldStrategy.IGNORED)
    private Long day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day23", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23", updateStrategy = FieldStrategy.IGNORED)
    private Long day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day24", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24", updateStrategy = FieldStrategy.IGNORED)
    private Long day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day25", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25", updateStrategy = FieldStrategy.IGNORED)
    private Long day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day26", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26", updateStrategy = FieldStrategy.IGNORED)
    private Long day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day27", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27", updateStrategy = FieldStrategy.IGNORED)
    private Long day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day28", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28", updateStrategy = FieldStrategy.IGNORED)
    private Long day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day29", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29", updateStrategy = FieldStrategy.IGNORED)
    private Long day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day30", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30", updateStrategy = FieldStrategy.IGNORED)
    private Long day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.day31", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31", updateStrategy = FieldStrategy.IGNORED)
    private Long day31;

    /**
     * 硫化总工时
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.totalVulcanizationMinutes", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES", updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal totalVulcanizationMinutes;

    /**
     * 合模压力
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(exist = false)
    private BigDecimal mouldClampingPressure;

    /**
     * 模具型腔
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.moldCavity")
    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    @TableField(exist = false)
    private String moldCavity;

    /**
     * 获取原始的合并计划集合
     *
     * @return
     */
    @ApiModelProperty(hidden = true)
    public List<SinglePlanInfoHelper> getMergePlanList() {
        if (StringUtils.isBlank(mergeInfo)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(mergeInfo, SinglePlanInfoHelper.class);
    }

    /**
     * 获取更新值
     * 生产版本号、物料编码
     * 库位类别、品牌、渠道、
     * 是否有交期
     * 规格代号
     *
     * @return
     */
    @ApiModelProperty(hidden = true)
    public String getUpdateImportValue() {
        String summaryFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%s";
        return String.format(summaryFormat, getProductionVersion(), getProductCode(),
                getLocationType(), getBrand(), getChannel(), getIsDeliveryDate(), getSpecCode());
    }

    /**
     * 获取生胎代码与硫化规格代码唯一组合键
     *
     * @return
     */
    public String getProductConstructionKey() {
        String duplicateKeyFormat = "%s|*|%s";
        return String.format(duplicateKeyFormat, getEmbryoCode(), getSpecCode());
    }

    /**
     * 获取SAP+规格代号唯一组合键
     *
     * @return
     */
    public String getProductMouldKey() {
        String duplicateKeyFormat = "%s|*|%s";
        return String.format(duplicateKeyFormat, getProductCode(), getSpecCode());
    }

    /**
     * 判断是否重复
     * 生产版本号、物料编码
     * 库位类别、品牌、渠道、
     * 是否有交期
     * 规格代号
     *
     * @return
     */
    public String getImportDuplicateKey() {
        String duplicateKeyFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%s";
        return String.format(duplicateKeyFormat, getProductionVersion(), getProductCode(),
                getLocationType(), getBrand(), getChannel(), getIsDeliveryDate(), getSpecCode());
    }

    /**
     * 获取同版本的值
     * 分厂、年、月
     * 需求版本、排产版本
     *
     * @return
     */
    public String getSameProductionVersionKey() {
        String sameProductionVersionKeyFormat = "%s|*|%s|*|%s|*|%s|*|%s";
        return String.format(sameProductionVersionKeyFormat, getFactoryCode(), getYear(), getMonth(), getMonthPlanVersion(), getProductionVersion());
    }

    /**
     * 未排产原因国际化
     */
    @ApiModelProperty(value = "未排产原因国际化", name = "reasonI18n")
    @TableField(exist = false)
    private String reasonI18n;
}
