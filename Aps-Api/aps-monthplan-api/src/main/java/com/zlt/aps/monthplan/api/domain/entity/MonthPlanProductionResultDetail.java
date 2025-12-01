package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanProductionResultDetail.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果对象 T_MP_MOULDING_DAY_RESULT_DETAIL
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-13
 */

@Data
@TableName(value = "T_MP_MOULDING_DAY_RESULT_DETAIL")
@ApiModel(value = "分厂月生产计划排产结果-生产计划排产结果对象", description = "分厂月生产计划排产结果-生产计划排产结果对象 ")
public class MonthPlanProductionResultDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 年月:YYYYMM
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.yearMonth")
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 序号
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.planSeq")
    @ApiModelProperty(value = "序号", name = "planSeq")
    @TableField(value = "PLAN_SEQ")
    private Long planSeq;

    /**
     * 需求计划ID
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.monthPlanId")
    @ApiModelProperty(value = "需求计划ID", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private Integer constructionStage;
    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldMethod", dictType = "MACHINE_TYPE")
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
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.locationType")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.channel")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 等级码
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode")
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName")
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.isImportantCustom")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.isEnsurePlan")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.isEmergency")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 是否欠产（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.isDebitPlan", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private Integer isDebitPlan;

    /**
     * 是否备货 0 不是 1 是
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.isStockUp")
    @ApiModelProperty(value = "是否备货 0 不是 1 是", name = "isStockUp")
    @TableField(value = "IS_STOCK_UP")
    private Integer isStockUp;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 利润等级
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.profitGrade")
    @ApiModelProperty(value = "利润等级", name = "profitGrade")
    @TableField(value = "PROFIT_GRADE")
    private Integer profitGrade;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.isImport", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.mouldCode")
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.embryoCode")
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

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
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.prodReqPlan")
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Long prodReqPlan;

    /**
     * 实际生产需求
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.factProdReqQty")
    @ApiModelProperty(value = "实际生产需求", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Long factProdReqQty;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.totalQty")
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Long totalQty;

    /**
     * 差异量(未排产数量)
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.differenceQty")
    @ApiModelProperty(value = "差异量(未排产数量)", name = "differenceQty")
    @TableField(value = "DIFFERENCE_QTY")
    private Integer differenceQty;

    /**
     * 未排产原因
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.reason")
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.beginDate")
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Integer beginDate;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.endDay")
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;

    /**
     * PRE_DAY_1
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay1")
    @ApiModelProperty(value = "PRE_DAY_1", name = "preDay1")
    @TableField(value = "PRE_DAY_1")
    private Long preDay1;

    /**
     * PRE_DAY_2
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay2")
    @ApiModelProperty(value = "PRE_DAY_2", name = "preDay2")
    @TableField(value = "PRE_DAY_2")
    private Long preDay2;

    /**
     * PRE_DAY_3
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay3")
    @ApiModelProperty(value = "PRE_DAY_3", name = "preDay3")
    @TableField(value = "PRE_DAY_3")
    private Long preDay3;

    /**
     * PRE_DAY_4
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay4")
    @ApiModelProperty(value = "PRE_DAY_4", name = "preDay4")
    @TableField(value = "PRE_DAY_4")
    private Long preDay4;

    /**
     * PRE_DAY_5
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay5")
    @ApiModelProperty(value = "PRE_DAY_5", name = "preDay5")
    @TableField(value = "PRE_DAY_5")
    private Long preDay5;

    /**
     * PRE_DAY_6
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay6")
    @ApiModelProperty(value = "PRE_DAY_6", name = "preDay6")
    @TableField(value = "PRE_DAY_6")
    private Long preDay6;

    /**
     * PRE_DAY_7
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.preDay7")
    @ApiModelProperty(value = "PRE_DAY_7", name = "preDay7")
    @TableField(value = "PRE_DAY_7")
    private Long preDay7;

    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day1")
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Long day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day2")
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Long day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day3")
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Long day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day4")
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Long day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day5")
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Long day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day6")
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Long day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day7")
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Long day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day8")
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Long day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day9")
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Long day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day10")
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Long day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day11")
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Long day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day12")
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Long day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day13")
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Long day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day14")
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Long day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day15")
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Long day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day16")
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Long day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day17")
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Long day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day18")
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Long day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day19")
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Long day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day20")
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Long day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day21")
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Long day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day22")
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Long day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day23")
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Long day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day24")
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Long day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day25")
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Long day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day26")
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Long day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day27")
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Long day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day28")
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Long day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day29")
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Long day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day30")
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Long day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.day31")
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Long day31;

    /**
     * 硫化总工时
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.totalVulcanizationMinutes")
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES")
    private BigDecimal totalVulcanizationMinutes;

    /**
     * 显示顺序
     */
    @Excel(name = "ui.data.column.monthPlanProductionResultDetail.displaySeq")
    @ApiModelProperty(value = "显示顺序", name = "displaySeq")
    @TableField(value = "DISPLAY_SEQ")
    private Integer displaySeq;

    /**
     * 是否有交期
     *
     * @return
     */
    public Integer getIsDeliveryDate() {
        if (null == deliveryDateDue) {
            return BigDecimal.ZERO.intValue();
        }
        return BigDecimal.ONE.intValue();
    }

    /**
     * 获取合并汇总值
     * 生产版本号、物料编码
     * 库位类别、品牌、渠道、
     * 是否有交期
     * 规格代号
     *
     * @return
     */
    public String getSummaryValue() {
        String summaryFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%s";
        return String.format(summaryFormat, getProductionVersion(), getProductCode(),
                getLocationType(), getBrand(), getChannel(), getIsDeliveryDate(), getSpecCode());
    }
}