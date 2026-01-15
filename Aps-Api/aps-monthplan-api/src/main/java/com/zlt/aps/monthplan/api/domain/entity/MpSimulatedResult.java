package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSimulatedResult.java
 * 描    述：S2-1004.实单模拟排产对象 t_mp_simulated_result
 *@author yelq
 *@date 2025-12-31
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "S2-1004.实单模拟排产对象", description = "S2-1004.实单模拟排产对象 ")
@Data
@TableName(value = "T_MP_SIMULATED_RESULT")
public class MpSimulatedResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.simulatedResult.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.simulatedResult.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.simulatedResult.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 产品品类 数据字典：biz_product_type  全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.simulatedResult.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type  全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 产品结构 */
    @Excel(name = "ui.data.column.simulatedResult.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 规格 */
    @Excel(name = "ui.data.column.simulatedResult.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 花纹 */
    @Excel(name = "ui.data.column.simulatedResult.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 主花纹 */
    @Excel(name = "ui.data.column.simulatedResult.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 品牌 */
    @Excel(name = "ui.data.column.simulatedResult.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 主物料 */
    @Excel(name = "ui.data.column.simulatedResult.mainMaterialDesc")
    @ApiModelProperty(value = "主物料", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 物料编码 */
    @Excel(name = "ui.data.column.simulatedResult.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.simulatedResult.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;


    /** 模具数量(同主花纹的模具数量) */
    @Excel(name = "ui.data.column.simulatedResult.mouldQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "模具数量(同主花纹的模具数量)", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /** 活块数量(同主花纹的物料模具数量) */
    @Excel(name = "ui.data.column.simulatedResult.typeBlockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "活块数量(同主花纹的物料模具数量)", name = "typeBlockQty")
    @TableField(value = "TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

    /** 净需求 */
    @Excel(name = "ui.data.column.simulatedResult.netQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /** 高优先级数量 */
    @Excel(name = "ui.data.column.simulatedResult.heightQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /** 排产总量 */
    @Excel(name = "ui.data.column.simulatedResult.productionQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "排产总量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 模拟排产需求版本号 */
    @Excel(name = "ui.data.column.simulatedResult.monthPlanVersion")
    @ApiModelProperty(value = "模拟排产需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 第1个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month1", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第1个月排产量", name = "month1")
    @TableField(value = "MONTH_1")
    private Integer month1;

    /** 第2个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month2", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第2个月排产量", name = "month2")
    @TableField(value = "MONTH_2")
    private Integer month2;

    /** 第3个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month3", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第3个月排产量", name = "month3")
    @TableField(value = "MONTH_3")
    private Integer month3;

    /** 第4个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month4", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第4个月排产量", name = "month4")
    @TableField(value = "MONTH_4")
    private Integer month4;

    /** 第5个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month5", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第5个月排产量", name = "month5")
    @TableField(value = "MONTH_5")
    private Integer month5;

    /** 第6个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month6", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第6个月排产量", name = "month6")
    @TableField(value = "MONTH_6")
    private Integer month6;

    /** 第7个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month7", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第7个月排产量", name = "month7")
    @TableField(value = "MONTH_7")
    private Integer month7;

    /** 第8个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month8", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第8个月排产量", name = "month8")
    @TableField(value = "MONTH_8")
    private Integer month8;

    /** 第9个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month9", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第9个月排产量", name = "month9")
    @TableField(value = "MONTH_9")
    private Integer month9;

    /** 第10个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month10", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第10个月排产量", name = "month10")
    @TableField(value = "MONTH_10")
    private Integer month10;

    /** 第11个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month11", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第11个月排产量", name = "month11")
    @TableField(value = "MONTH_11")
    private Integer month11;

    /** 第12个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month12", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第12个月排产量", name = "month12")
    @TableField(value = "MONTH_12")
    private Integer month12;

    /** 第13个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month13", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第13个月排产量", name = "month13")
    @TableField(value = "MONTH_13")
    private Integer month13;

    /** 第14个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month14", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第14个月排产量", name = "month14")
    @TableField(value = "MONTH_14")
    private Integer month14;

    /** 第15个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month15", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第15个月排产量", name = "month15")
    @TableField(value = "MONTH_15")
    private Integer month15;

    /** 第16个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month16", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第16个月排产量", name = "month16")
    @TableField(value = "MONTH_16")
    private Integer month16;

    /** 第17个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month17", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第17个月排产量", name = "month17")
    @TableField(value = "MONTH_17")
    private Integer month17;

    /** 第18个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month18", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第18个月排产量", name = "month18")
    @TableField(value = "MONTH_18")
    private Integer month18;

    /** 第19个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month19", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第19个月排产量", name = "month19")
    @TableField(value = "MONTH_19")
    private Integer month19;

    /** 第20个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month20", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第20个月排产量", name = "month20")
    @TableField(value = "MONTH_20")
    private Integer month20;

    /** 第21个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month21", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第21个月排产量", name = "month21")
    @TableField(value = "MONTH_21")
    private Integer month21;

    /** 第22个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month22", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第22个月排产量", name = "month22")
    @TableField(value = "MONTH_22")
    private Integer month22;

    /** 第23个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month23", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第23个月排产量", name = "month23")
    @TableField(value = "MONTH_23")
    private Integer month23;

    /** 第24个月排产量 */
    @Excel(name = "ui.data.column.simulatedResult.month24", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "第24个月排产量", name = "month24")
    @TableField(value = "MONTH_24")
    private Integer month24;
    /** 模拟排产计划版本 = 模拟排产需求版本号 */
    @ApiModelProperty(value = "模拟排产计划版本 = 模拟排产需求版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** MES物料编码 */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;
}