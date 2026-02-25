package com.zlt.aps.mp.api.domain.entity;

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
 * 文件名称：PlanMoldingProdution.java
 * 描    述：分厂月生产计划排产过程-成型-计划成型排产对象 t_mp_proc_plan_molding_prodution
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-19
 */

@Data
@TableName(value = "T_MP_PROC_PLAN_MOLDING_PRODUTION")
@ApiModel(value = "分厂月生产计划排产过程-成型-计划成型排产对象", description = "分厂月生产计划排产过程-成型-计划成型排产对象 ")
public class PlanMoldingProdution extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 序号
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.planSeq")
    @ApiModelProperty(value = "序号", name = "planSeq")
    @TableField(value = "PLAN_SEQ")
    private Integer planSeq;

    /**
     * 月计划ID
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.monthPlanId")
    @ApiModelProperty(value = "月计划ID", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 等级码
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode")
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName")
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.locationType")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private Integer locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.channel")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isImportantCustom")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isEnsurePlan")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private Integer isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isEmergency")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private Integer isEmergency;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.PlanMoldingProdution.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 利润等级
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.profitGrade")
    @ApiModelProperty(value = "利润等级", name = "profitGrade")
    @TableField(value = "PROFIT_GRADE")
    private Integer profitGrade;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isImport", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 施工号
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.constructionCode")
    @ApiModelProperty(value = "施工号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * 成型法：1-1次法，2-2次法，3-3鼓，4-2鼓
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.mouldMethod")
    @ApiModelProperty(value = "成型法：1-1次法，2-2次法，3-3鼓，4-2鼓", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private Integer mouldMethod;

    /**
     * 排产模式:1- 1次法，2-2次法
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productionMode")
    @ApiModelProperty(value = "排产模式:1- 1次法，2-2次法", name = "productionMode")
    @TableField(value = "PRODUCTION_MODE")
    private Integer productionMode;

    /**
     * 硫化时间
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.curingTime")
    @ApiModelProperty(value = "硫化时间", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private BigDecimal curingTime;

    /**
     * 胎休布层数            1-1            2-2            3-3
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.carcassClothType")
    @ApiModelProperty(value = "胎休布层数            1-1            2-2            3-3", name = "carcassClothType")
    @TableField(value = "CARCASS_CLOTH_TYPE")
    private Integer carcassClothType;

    /**
     * 扣圈盘直径
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.bucklePlageDiameter")
    @ApiModelProperty(value = "扣圈盘直径", name = "bucklePlageDiameter")
    @TableField(value = "BUCKLE_PLAGE_DIAMETER")
    private Integer bucklePlageDiameter;

    /**
     * 机头宽度
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.headWidth")
    @ApiModelProperty(value = "机头宽度", name = "headWidth")
    @TableField(value = "HEAD_WIDTH")
    private BigDecimal headWidth;

    /**
     *
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.viceStockCircumFerence")
    @ApiModelProperty(value = "", name = "viceStockCircumFerence")
    @TableField(value = "VICE_STOCK_CIRCUM_FERENCE")
    private Integer viceStockCircumFerence;

    /**
     * 排产顺序
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productionSequence")
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    @TableField(value = "PRODUCTION_SEQUENCE")
    private Integer productionSequence;

    /**
     * 续作
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isContinue")
    @ApiModelProperty(value = "续作", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private Integer isContinue;

    /**
     * 是否定点生产:0-否，1-是
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isFixedPoint")
    @ApiModelProperty(value = "是否定点生产:0-否，1-是", name = "isFixedPoint")
    @TableField(value = "IS_FIXED_POINT")
    private Integer isFixedPoint;

    /**
     * 是否欠产（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isDebitPlan", readConverterExp = "0=：默认不是，1：是")
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private Integer isDebitPlan;

    /**
     * 可平衡:0-否，1-是
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.isBalance")
    @ApiModelProperty(value = "可平衡:0-否，1-是", name = "isBalance")
    @TableField(value = "IS_BALANCE")
    private Integer isBalance;

    /**
     * 成型顺序
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.moldingSeq")
    @ApiModelProperty(value = "成型顺序", name = "moldingSeq")
    @TableField(value = "MOLDING_SEQ")
    private Integer moldingSeq;

    /**
     * 成型机ID
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.moldingMachineId")
    @ApiModelProperty(value = "成型机ID", name = "moldingMachineId")
    @TableField(value = "MOLDING_MACHINE_ID")
    private Long moldingMachineId;

    /**
     * 成型机编号
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.moldingMachineCode")
    @ApiModelProperty(value = "成型机编号", name = "moldingMachineCode")
    @TableField(value = "MOLDING_MACHINE_CODE")
    private String moldingMachineCode;

    /**
     * 成型排产数量
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.productionQty")
    @ApiModelProperty(value = "成型排产数量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /**
     * 拆A计划量
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.a0LevelQty")
    @ApiModelProperty(value = "拆A计划量", name = "a0LevelQty")
    @TableField(value = "A0_LEVEL_QTY")
    private Integer a0LevelQty;

    /**
     * 成型排产数量
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.vulcanizingQty")
    @ApiModelProperty(value = "成型排产数量", name = "vulcanizingQty")
    @TableField(value = "VULCANIZING_QTY")
    private Integer vulcanizingQty;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @Excel(name = "ui.data.column.PlanMoldingProdution.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;


}