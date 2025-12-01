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
 * 文件名称：MonthPlanPreProductionCapacity.java
 * 描    述：分厂月生产计划产能预分配 t_mp_month_plan_pre_capacity
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */

@Data
@TableName(value = "T_MP_MONTH_PLAN_PRE_CAPACITY")
@ApiModel(value = "分厂月生产计划产能预分配对象", description = "分厂月生产计划产能预分配对象")
public class MonthPlanPreProductionCapacity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;
    /**
     * 需求计划
     */
    @ApiModelProperty(value = "需求计划", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 序号
     */
    @ApiModelProperty(value = "序号", name = "planSeq")
    @TableField(value = "PLAN_SEQ")
    private Long planSeq;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private Integer constructionStage;

    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.mouldMethod", dictType = "MACHINE_TYPE")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.specCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.embryoCode")
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
     * 胎体布层级数 1 表示单层 2 表示多层(即2,3等)
     */
    @ApiModelProperty(value = "胎体布层级数", name = "tireFabricNumber")
    @TableField(value = "TIRE_FABRIC_NUMBER")
    private Integer tireFabricNumber;
    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.productTypeCode", dictType = "biz_product_name")
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
     * 规格
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.pattern")
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
     * 生产需求计划
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.prodReqPlan", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Long prodReqPlan;

    /**
     * 实际生产需求(含损耗)
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.factProdReqQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "实际生产需求", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Long factProdReqQty;

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
     * 是否续作 0 不续作 1续作
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isContinue", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否续作 0 不续作 1续作", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private Integer isContinue;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isEnsurePlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isEmergency", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 是否欠产（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isDebitPlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private Integer isDebitPlan;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 是否备货 0 不是 1 是
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isStockUp", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否备货 0 不是 1 是", name = "isStockUp")
    @TableField(value = "IS_STOCK_UP")
    private Integer isStockUp;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 利润等级
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.profitGrade", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "利润等级", name = "profitGrade")
    @TableField(value = "PROFIT_GRADE")
    private Integer profitGrade;

    /**
     * 分厂不排产:0-否，1-是
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.isFactoryProduction", dictType = "biz_yes_no")
    @ApiModelProperty(value = "分厂不排产:0-否，1-是", name = "isFactoryProduction")
    @TableField(value = "IS_FACTORY_PRODUCTION")
    private Integer isFactoryProduction;

    /**
     * 硫化时间
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.curingTime")
    @ApiModelProperty(value = "硫化时间-到秒", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private BigDecimal curingTime;

    /**
     * 排产顺序
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.productionSequence", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    @TableField(value = "PRODUCTION_SEQUENCE")
    private Long productionSequence;

    /**
     * 是否备胎 RF:0-否，1-是
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.rf", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否备胎 RF:0-否，1-是", name = "rf")
    @TableField(value = "RF")
    private Integer rf;

    /**
     * 预占模具信息
     */
    @ApiModelProperty(value = "预占模具信息", name = "mouldCodeInfo")
    @TableField(value = "MOULD_CODE_INFO")
    private String mouldCodeInfo;

    /**
     * 可用模具数量
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.mouldQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "可用模具数量", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 模具满产产量
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.mouldFullQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "模具满产产量", name = "mouldFullQty")
    @TableField(value = "MOULD_FULL_QTY")
    private Long mouldFullQty;

    /**
     * 预可排产量
     */
    @Excel(name = "ui.data.column.monthPlanPreProductionCapacity.preProductionQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "预可排产量", name = "productionQty")
    @TableField(value = "PRE_PRODUCTION_QTY")
    private Long preProductionQty;

}