package com.zlt.aps.mp.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureIn.java
 * 描    述：调整-结构内调整记录对象 t_mp_adjust_structure_in
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "调整-结构内调整记录对象", description = "调整-结构内调整记录对象 ")
@Data
@TableName(value = "T_MP_ADJUST_STRUCTURE_IN")
public class MpAdjustStructureIn extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

     /** 年份 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 版本规则：ADJ+年月日+3位流水号； */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.version")
    @ApiModelProperty(value = "版本规则：ADJ+年月日+3位流水号；", name = "version")
    @TableField(value = "VERSION")
    private String version;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 排产计划版本
     */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.productionVersion")
    @ApiModelProperty(value = "排产计划版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 最新需求计划版本(每次调整后变化)
     */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.lastMonthPlanVersion")
    @ApiModelProperty(value = "最新需求计划版本(每次调整后变化)", name = "lastMonthPlanVersion")
    @TableField(value = "LAST_MONTH_PLAN_VERSION")
    private String lastMonthPlanVersion;

    /** 产品结构 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 排产机台,多个机台用逗号分隔 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.scheduledMachines")
    @ApiModelProperty(value = "排产机台,多个机台用逗号分隔", name = "scheduledMachines")
    @TableField(value = "SCHEDULED_MACHINES")
    private String scheduledMachines;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 排产分类 数据字典：biz_schedule_type
     */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.productionType", dictType = "biz_schedule_type")
    @ApiModelProperty(value = "排产分类", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

    /** 是否含特殊材料            2、0-否，1-是 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.hasSpecialMaterial")
    @ApiModelProperty(value = "是否含特殊材料            2、0-否，1-是", name = "hasSpecialMaterial")
    @TableField(value = "HAS_SPECIAL_MATERIAL")
    private String hasSpecialMaterial;

    /**
     * 产品品类 数据字典：biz_product_type TBR 全钢 PCR 半钢
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.productTypeCode")
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type TBR 全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 英寸
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /**
     * 产品分类
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.productCategory", dictType = "product_category")
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    @TableField(value = "PRODUCT_CATEGORY")
    private String productCategory;
    /**
     * 产品状态
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.productStatus")
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    @TableField(value = "PRODUCT_STATUS")
    private String productStatus;

    /**
     * 主物料(胎胚号)
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;


    /**
     * 施工阶段 0 无工艺 1 试制 2 量试 3 正式
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.constructionStage")
    @ApiModelProperty(value = "施工阶段 0 无工艺 1 试制 2 量试 3 正式", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 主花纹
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 型腔数量(同主花纹的模具数量)
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.mouldCavityQty")
    @ApiModelProperty(value = "型腔数量(同主花纹的模具数量)", name = "mouldCavityQty")
    @TableField(value = "MOULD_CAVITY_QTY")
    private Integer mouldCavityQty;

    /**
     * 活块数量(同主花纹的物料模具数量)
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.typeBlockQty")
    @ApiModelProperty(value = "活块数量(同主花纹的物料模具数量)", name = "typeBlockQty")
    @TableField(value = "TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

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
     * 日硫化量
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.dayVulcanizationQty")
    @ApiModelProperty(value = "日硫化量", name = "dayVulcanizationQty")
    @TableField(value = "DAY_VULCANIZATION_QTY")
    private Integer dayVulcanizationQty;

    /**
     * 单条硫化时间(包含增加间隔)-调整时使用
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.curingTime")
    @ApiModelProperty(value = "单条硫化时间(包含增加间隔)-调整时使用", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /** 调整前净需求量（上周） */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.previousNetQty", readConverterExp = "上=周")
    @ApiModelProperty(value = "调整前净需求量", name = "previousNetQty")
    @TableField(value = "PREVIOUS_NET_QTY")
    private Integer previousNetQty;

    /** 当前净需求量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.currentNetQty")
    @ApiModelProperty(value = "当前净需求量", name = "currentNetQty")
    @TableField(value = "CURRENT_NET_QTY")
    private Integer currentNetQty;

    /** 净需求变动 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.netQtyChange")
    @ApiModelProperty(value = "净需求变动", name = "netQtyChange")
    @TableField(value = "NET_QTY_CHANGE")
    private Integer netQtyChange;

    /** 月计划已排产量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.monthScheduledQty")
    @ApiModelProperty(value = "月计划已排产量", name = "monthScheduledQty")
    @TableField(value = "MONTH_SCHEDULED_QTY")
    private Integer monthScheduledQty;

    /** 待调整量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.pendingQty")
    @ApiModelProperty(value = "待调整量", name = "pendingQty")
    @TableField(value = "PENDING_QTY")
    private Integer pendingQty;

    /** 确认调整量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.confirmAdjustQty")
    @ApiModelProperty(value = "确认调整量", name = "confirmAdjustQty")
    @TableField(value = "CONFIRM_ADJUST_QTY")
    private Integer confirmAdjustQty;

    /** 调整优先级            2、针对增量            2.1）在产SKU增量，先补；            2.2）新增SKU，按调整优先级1.2.3…            3、该列默认空，允许编辑； */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.adjustPriority", dictType = "adjust_priority")
    @ApiModelProperty(value = "调整优先级            2、针对增量            2.1）在产SKU增量，先补；            2.2）新增SKU，按调整优先级1.2.3…            3、该列默认空，允许编辑；", name = "adjustPriority")
    @TableField(value = "ADJUST_PRIORITY")
    private Integer adjustPriority;

    /** 实际调整 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.actualAdjustQty")
    @ApiModelProperty(value = "实际调整", name = "actualAdjustQty")
    @TableField(value = "ACTUAL_ADJUST_QTY")
    private Integer actualAdjustQty;

    /** 调整原因 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.adjustReason")
    @ApiModelProperty(value = "调整原因", name = "adjustReason")
    @TableField(value = "ADJUST_REASON")
    private String adjustReason;

    /** 已生产量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.productionQty")
    @ApiModelProperty(value = "已生产量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 是否SKU新增 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.isSkuAdd", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否SKU新增")
    @TableField(value = "IS_SKU_ADD")
    private String isSkuAdd;

    /**
     * 紧急程度 数据字典 biz_urgency_type 01 紧急 04 普通
     */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.urgencyType", dictType = "biz_urgency_type")
    @ApiModelProperty(value = "紧急程度 数据字典 biz_urgency_type 01 紧急 04 普通", name = "urgencyType")
    @TableField(value = "URGENCY_TYPE")
    private String urgencyType;

    /**
     * 制造示方书号
     */
    @ImportExcelValidated(maxLength = 30)
    @Excel(name = "ui.data.column.mpAdjustStructureIn.embryoNo")
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /**
     * 试制量试ID
     */
    @ApiModelProperty(value = "试制量试ID", name = "trialPlanId")
    @TableField(value = "TRIAL_PLAN_ID")
    private String trialPlanId;


    /**
     * 获取分组key
     * @return
     */
    public String getGroupKey() {
        String groupKeyFormat = "%s|*|%s";
        return String.format(groupKeyFormat, structureName, materialCode);
    }


}