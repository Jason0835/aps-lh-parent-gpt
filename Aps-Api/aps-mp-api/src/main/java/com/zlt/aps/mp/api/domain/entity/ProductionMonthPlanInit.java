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
 * 文件名称：ProductionMonthPlanInit.java
 * 描    述：分厂月生产计划排产过程-计划初始化对象 T_MP_PROC_MONTH_PLAN_INIT
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20251205
 */

@Data
@TableName(value = "T_MP_PROC_MONTH_PLAN_INIT")
@ApiModel(value = "工厂月生产计划排产过程-计划初始化表对象", description = "工厂月生产计划排产过程-计划初始化表对象")
public class ProductionMonthPlanInit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.planType", dictType = "biz_plan_type")
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 产品品类
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 需求计划ID 可是需求计划或是试制量试计划
     */
    @ApiModelProperty(value = "需求计划", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * MES物料编码
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 英寸
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /**
     * 产品分类
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.productCategory", dictType = "product_category")
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    @TableField(value = "PRODUCT_CATEGORY")
    private String productCategory;

    /**
     * 产品状态
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.productStatus")
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
    @Excel(name = "ui.data.column.productionMonthPlanInit.productionType", dictType = "biz_schedule_type")
    @ApiModelProperty(value = "排产分类", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.schedulingType", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.mouldMethod", dictType = "MACHINE_TYPE")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.embryoCode")
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 主物料(胎胚描述)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /**
     * 是否零度材料
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isZeroRack")
    @ApiModelProperty(value = "是否零度材料", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;


    /**
     * 制造示方书号
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.embryoNo")
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /**
     * 文字示方书号
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.textNo")
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    /**
     * 硫化示方书号
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.lhNo")
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;

    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    @TableField(exist = false)
    private String specCodeInfo;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

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
    @Excel(name = "ui.data.column.productionMonthPlanInit.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 主花纹
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 速级
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.speed")
    @ApiModelProperty(value = "速级", name = "speed")
    @TableField(value = "SPEED")
    private String speed;

    /**
     * 年周号
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.yearWeek")
    @ApiModelProperty(value = "年周号", name = "yearWeek")
    @TableField(value = "YEAR_WEEK")
    private String yearWeek;

    /**
     * 动平衡
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isDynamicBalance")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /**
     * 均匀性
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isUniformity")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /**
     * 物料优先 1 优先 0 不优先
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isPrioritize", dictType = "biz_yes_no")
    @ApiModelProperty(value = "物料优先", name = "isPrioritize")
    @TableField(value = "IS_PRIORITIZE")
    private String isPrioritize;

    /**
     * 结构优先 1 优先 0 不优先
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.structurePriority", dictType = "biz_yes_no")
    @ApiModelProperty(value = "结构优先", name = "structurePriority")
    @TableField(value = "STRUCTURE_PRIORITY")
    private String structurePriority;

    /**
     * 最小投产量值
     */
    @Excel(name = "ui.data.column.demandPlan.minProductionQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "最小投产量值", name = "minProductionQty")
    @TableField(value = "MIN_PRODUCTION_QTY")
    private Integer minProductionQty;
    /**
     * 高优先级
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.heightQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /**
     * 中优先级
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.midQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "中优先级", name = "midQty")
    @TableField(value = "MID_QTY")
    private Integer midQty;

    /**
     * 暂缓订单
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.postponeQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "暂缓订单", name = "postponeQty")
    @TableField(value = "POSTPONE_QTY")
    private Integer postponeQty;

    /**
     * 周期排产储备
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.cycleReserveQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    @TableField(value = "CYCLE_RESERVE_QTY")
    private Integer cycleReserveQty;

    /**
     * 常规储备
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.conventionReserveQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "常规储备", name = "conventionReserveQty")
    @TableField(value = "CONVENTION_RESERVE_QTY")
    private Integer conventionReserveQty;

    /**
     * 排产净需求
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.netQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /**
     * 常规搭配是否加入排产净需求
     */
    @ApiModelProperty(value = "加入排产净需求", name = "isAddNetQty")
    @TableField(value = "IS_ADD_NET_QTY")
    private String isAddNetQty;

    /**
     * 加入排产净需求的量
     */
    @ApiModelProperty(value = "加入排产净需求的量", name = "addNetQty")
    @TableField(value = "ADD_NET_QTY")
    private Integer addNetQty;
    /**
     * 净需求(含暂缓)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.postponeNetQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "净需求(含暂缓)", name = "postponeNetQty")
    @TableField(value = "POSTPONE_NET_QTY")
    private Integer postponeNetQty;

    /**
     * 净需求(不含暂缓)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.unPostponeNetQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "净需求(不含暂缓)", name = "unPostponeNetQty")
    @TableField(value = "UN_POSTPONE_NET_QTY")
    private Integer unPostponeNetQty;
    /**
     * 高优先级需求(含损耗)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.heightLossQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "高优先级需求(含损耗)", name = "heightLossQty")
    @TableField(value = "HEIGHT_LOSS_QTY")
    private Integer heightLossQty;

    /**
     * 实际生产需求(含损耗，排除高优先级损耗)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.factProdReqQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "实际生产需求(含损耗，排除高优先级损耗)", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Integer factProdReqQty;

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
    @Excel(name = "ui.data.column.productionMonthPlanInit.isContinue", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否续作 0 不续作 1续作", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private String isContinue;

    /**
     * 是否重要客户 0 不重要 1 重要
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户 0 不重要 1 重要", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private String isImportantCustom;

    /**
     * 是否必保计划 0 不必保 1 必保
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isEnsurePlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否必保计划 0 不必保 1 必保", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private String isEnsurePlan;

    /**
     * 是否紧急订单 0 不紧急 1 紧急
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isEmergency", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否紧急订单 0 不紧急 1 紧急", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private String isEmergency;

    /**
     * 是否欠产（0：默认不是，1：是）
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isDebitPlan", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private String isDebitPlan;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.productionMonthPlanInit.deliveryDateDue", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private String isImport;

    /**
     * 利润等级
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.profitGrade", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "利润等级", name = "profitGrade")
    @TableField(value = "PROFIT_GRADE")
    private Integer profitGrade;

    /**
     * 工厂不排产:0-否，1-是
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isFactoryProduction", dictType = "biz_yes_no")
    @ApiModelProperty(value = "工厂不排产:0-否，1-是", name = "isFactoryProduction")
    @TableField(value = "IS_FACTORY_PRODUCTION")
    private String isFactoryProduction;

    /**
     * 硫化时间
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.curingTime")
    @ApiModelProperty(value = "硫化时间-到秒", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private BigDecimal curingTime;

    /**
     * 日硫化量(单模)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.dayVulcanizationQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "日硫化量", name = "dayVulcanizationQty")
    @TableField(value = "DAY_VULCANIZATION_QTY")
    private Integer dayVulcanizationQty;

    /**
     * 库存数量(成品总库存)
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存数量(成品总库存)", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /**
     * 月均销量
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.averageSaleQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Integer averageSaleQty;

    /**
     * 可用模具数量
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.mouldQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "可用模具数量", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 是否排产:0-否，1-是
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.isProduction", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否排产:0-否，1-是", name = "isProduction")
    @TableField(value = "IS_PRODUCTION")
    private String isProduction;

    /**
     * 不排产原因:0-否，1-是
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.noProductionReason")
    @ApiModelProperty(value = "不排产原因:0-否，1-是", name = "noProductionReason")
    @TableField(value = "NO_PRODUCTION_REASON")
    private String noProductionReason;

}
