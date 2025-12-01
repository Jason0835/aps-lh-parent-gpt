package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionPlan.java
 * 描    述：分厂月生产计划排产过程-未排产计划对象 t_mp_proc_no_production_plan
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
@TableName(value = "T_MP_PROC_NO_PRODUCTION_PLAN")
@ApiModel(value = "分厂月生产计划排产过程-未排产计划对象", description = "分厂月生产计划排产过程-未排产计划对象 ")
public class MonthPlanNoProductionPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 序号
     */
    // @Excel(name = "ui.data.column.monthPlanNoProductionPlan.planSeq")
    @ApiModelProperty(value = "序号", name = "planSeq")
    @TableField(value = "PLAN_SEQ")
    private Integer planSeq;

    /**
     * 需求计划ID
     */
    // @Excel(name = "ui.data.column.monthPlanNoProductionPlan.monthPlanId")
    @ApiModelProperty(value = "需求计划ID", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 施工阶段 0 无工艺 1 试制 2 量试 3 正式
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.constructionStage", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段 0 无工艺 1 试制 2 量试 3 正式", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private Integer constructionStage;

    /**
     * 20250911 成型法
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.mouldMethod", dictType = "MACHINE_TYPE")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;
    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @ApiModelProperty(value = "生胎代码", name = "embryoCodeInfo")
    @TableField(value = "EMBRYO_CODE_INFO")
    private String embryoCodeInfo;
    /**
     * 模具
     */
    @ApiModelProperty(value = "模具", name = "mouldNoInfo")
    @TableField(value = "MOULD_NO_INFO")
    private String mouldNoInfo;
    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    // @Excel(name = "ui.data.column.monthPlanNoProductionPlan.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productTypeCode", dictType = "biz_product_name")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    // @Excel(name = "ui.data.column.monthPlanNoProductionPlan.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 生产需求计划
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.prodReqPlan", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Long prodReqPlan;

    /**
     * 实际生产需求(含损耗)
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.factProdReqQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "实际生产需求(含损耗)", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Long factProdReqQty;

    /**
     * 等级码
     */
//    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode")
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
//    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName")
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 是否续作 0 不续作 1续作
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isContinue", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否续作 0 不续作 1续作", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private Integer isContinue;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isEnsurePlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isEmergency", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 是否欠产（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isDebitPlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private Integer isDebitPlan;

    /**
     * 是否备货 0 不是 1 是
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isStockUp", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否备货 0 不是 1 是", name = "isStockUp")
    @TableField(value = "IS_STOCK_UP")
    private Integer isStockUp;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 利润等级
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.profitGrade")
    @ApiModelProperty(value = "利润等级", name = "profitGrade")
    @TableField(value = "PROFIT_GRADE")
    private Integer profitGrade;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    // @Excel(name = "ui.data.column.monthPlanNoProductionPlan.isImport", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 不排产原因
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.reason")
    @ApiModelProperty(value = "不排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;

    /**
     * 未排产数量
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.unProductionQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "未排产数量", name = "unProductionQty")
    @TableField(value = "UN_PRODUCTION_QTY")
    private Long unProductionQty;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.monthPlanNoProductionPlan.totalQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(exist = false)
    private Long totalQty;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.mdmModelInfo.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(exist = false)
    private String mouldNo;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.construction.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(exist = false)
    private String embryoCode;
}
