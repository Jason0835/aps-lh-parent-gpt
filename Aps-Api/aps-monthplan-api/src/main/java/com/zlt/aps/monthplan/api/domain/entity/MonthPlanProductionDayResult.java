package com.zlt.aps.monthplan.api.domain.entity;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.monthplan.api.domain.vo.ProductSpecInfoVo;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanProductionDayResult.java
 * 描    述：分厂月生产计划排产过程-排产结果对象-SKU t_mp_month_production_day
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-22
 */

@Data
@TableName(value = "T_MP_MONTH_PRODUCTION_DAY")
@ApiModel(value = "分厂月生产计划排产过程-排产结果对象-SKU", description = "分厂月生产计划排产过程-排产结果对象-SKU")
public class MonthPlanProductionDayResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.monthPlanVersion")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.productionVersion")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.productCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private Integer constructionStage;
    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.mouldMethod", dictType = "MACHINE_TYPE")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.specCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.embryoCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;
    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    @TableField(value = "SPEC_CODE_INFO")
    private String specCodeInfo;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 等级码
     */
    @ApiModelProperty(value = "等级码", name = "levelCode")
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
    @ApiModelProperty(value = "等级名称", name = "levelName")
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.productTypeCode", dictType = "biz_product_name")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 是否有交期 0 没有 1 有
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isDeliveryDate", dictType = "biz_yes_no")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "是否有交期", name = "isDeliveryDate")
    @TableField(value = "IS_DELIVERY_DATE")
    private Integer isDeliveryDate;

    /**
     * 是否有续作 0 没有 1 有
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isContinue", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否有续作 0 不续作 1续作", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private Integer isContinue;

    /**
     * 是否有重要客户 0 没有 1 有
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否有重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否有必保计划 0 没有 1 有
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isEnsurePlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否有必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否有紧急订单 0 没有 1 有
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isEmergency", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否有紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 是否标记欠产（0 没有 1 有）
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isDebitPlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否标记欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private Integer isDebitPlan;
    /**
     * 是否有备货 0 没有 1 有
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.isStockUp", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否有备货 0 不是 1 是", name = "isStockUp")
    @TableField(value = "IS_STOCK_UP")
    private Integer isStockUp;
    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 单条硫化时间(包含增加间隔)-调整时使用
     */
    @ApiModelProperty(value = "单条硫化时间(包含增加间隔)-到秒", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private BigDecimal curingTime;

    /**
     * 生产需求计划
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.prodReqPlan", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Long prodReqPlan;

    /**
     * 净需求量
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.prodReqPlan", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "净需求", name = "netDemandQty")
    @TableField(value = "NET_DEMAND_QTY")
    private Long netDemandQty;

    /**
     * 备货需求量
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.prodReqPlan", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "备货需求量", name = "stockUpDemandQty")
    @TableField(value = "STOCK_UP_DEMAND_QTY")
    private Long stockUpDemandQty;
    /**
     * 实际生产需求(含损耗)
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.factProdReqQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "实际生产需求(含损耗)", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Long factProdReqQty;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.totalQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Long totalQty;
    /**
     * 差异量(未排产量)
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.differenceQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, max = 99999999)
    @ApiModelProperty(value = "差异量(未排产量)", name = "differenceQty")
    @TableField(value = "DIFFERENCE_QTY")
    private Long differenceQty;

    /**
     * 未排产原因
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.reason")
    @ImportExcelValidated(maxLength = 1000)
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;
    /**
     * 模具
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.mouldNo")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;
    /**
     * 模具数
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.mouldQty")
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "模具数", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 模具编码集合，多个以,分隔
     */
    @ApiModelProperty(value = "模具编码集合，多个以,分隔", name = "mouldInfo")
    @TableField(value = "MOULD_INFO")
    private String mouldInfo;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.beginDate")
    @ImportExcelValidated(required = true, min = 1, max = 31)
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Integer beginDate;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.endDay")
    @ImportExcelValidated(required = true, min = 1, max = 31)
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
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
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay1")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_1", name = "preDay1")
    @TableField(value = "PRE_DAY_1")
    private Long preDay1;

    /**
     * PRE_DAY_2
     */
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay2")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_2", name = "preDay2")
    @TableField(value = "PRE_DAY_2")
    private Long preDay2;

    /**
     * PRE_DAY_3
     */
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay3")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_3", name = "preDay3")
    @TableField(value = "PRE_DAY_3")
    private Long preDay3;

    /**
     * PRE_DAY_4
     */
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay4")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_4", name = "preDay4")
    @TableField(value = "PRE_DAY_4")
    private Long preDay4;

    /**
     * PRE_DAY_5
     */
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay5")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_5", name = "preDay5")
    @TableField(value = "PRE_DAY_5")
    private Long preDay5;

    /**
     * PRE_DAY_6
     */
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay6")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_6", name = "preDay6")
    @TableField(value = "PRE_DAY_6")
    private Long preDay6;

    /**
     * PRE_DAY_7
     */
    // @Excel(name = "ui.data.column.monthPlanProductionDayResult.preDay7")
    // @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "PRE_DAY_7", name = "preDay7")
    @TableField(value = "PRE_DAY_7")
    private Long preDay7;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Long day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Long day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Long day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Long day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Long day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Long day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Long day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Long day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Long day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Long day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Long day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Long day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day13", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Long day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day14", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Long day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day15", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Long day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day16", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Long day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day17", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Long day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day18", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Long day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day19", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Long day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day20", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Long day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day21", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Long day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day22", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Long day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day23", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Long day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day24", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Long day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day25", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Long day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day26", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Long day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day27", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Long day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day28", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Long day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day29", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Long day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day30", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Long day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.day31", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Long day31;

    /**
     * 硫化总工时（分钟）
     */
    @Excel(name = "ui.data.column.monthPlanProductionDayResult.totalVulcanizationMinutes", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES")
    private BigDecimal totalVulcanizationMinutes;

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
     * 获取对应的硫化规格信息
     *
     * @return
     */
    public List<ProductSpecInfoVo> getProductSpecInfos() {
        if (StringUtils.isBlank(specCodeInfo)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(specCodeInfo, ProductSpecInfoVo.class);
    }

    /**
     * 是否可以切换规格
     *
     * @return
     */
    public Boolean getHasChangeSpecCode() {
        List<ProductSpecInfoVo> productSpecInfoList = getProductSpecInfos();
        if (CollectionUtils.isEmpty(productSpecInfoList)) {
            return false;
        }
        return productSpecInfoList.size() > 1;
    }
}