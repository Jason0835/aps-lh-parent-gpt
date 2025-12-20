package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpOrderPrediction.java
 * 描    述：订单预测对象 T_MP_ORDER_PREDICTION
 *
 * @author zlt
 * @version 1.0
 */
@ApiModel(value = "订单预测对象", description = "订单预测表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_MP_PRODUCTION_PREDICTION")
public class MpProductionPrediction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.prediction.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.prediction.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.prediction.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 产品品类 数据字典：biz_product_type  全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.prediction.productTypeCode")
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type  全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 库位类别 1 内销 2 外销 3 OE */
    @Excel(name = "ui.data.column.prediction.locationType")
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 品牌 */
    @Excel(name = "ui.data.column.prediction.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.prediction.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.prediction.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.prediction.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 第1个月排产量 */
    @Excel(name = "ui.data.column.prediction.month1")
    @ApiModelProperty(value = "第1个月排产量", name = "month1")
    @TableField(value = "MONTH_1")
    private Integer month1;

    /** 第2个月排产量 */
    @Excel(name = "ui.data.column.prediction.month2")
    @ApiModelProperty(value = "第2个月排产量", name = "month2")
    @TableField(value = "MONTH_2")
    private Integer month2;

    /** 第3个月排产量 */
    @Excel(name = "ui.data.column.prediction.month3")
    @ApiModelProperty(value = "第3个月排产量", name = "month3")
    @TableField(value = "MONTH_3")
    private Integer month3;

    /** 预测版本号 */
    @Excel(name = "ui.data.column.prediction.predictionVersion")
    @ApiModelProperty(value = "预测版本号", name = "predictionVersion")
    @TableField(value = "PREDICTION_VERSION")
    private String predictionVersion;

    /** 预测需求版本号 */
    @Excel(name = "ui.data.column.prediction.monthPlanVersion")
    @ApiModelProperty(value = "预测需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 预测排产计划版本 */
    @Excel(name = "ui.data.column.prediction.productionVersion")
    @ApiModelProperty(value = "预测排产计划版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 产品结构 */
    @Excel(name = "ui.data.column.prediction.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 英寸 */
    @Excel(name = "ui.data.column.prediction.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 规格 */
    @Excel(name = "ui.data.column.prediction.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 花纹 */
    @Excel(name = "ui.data.column.prediction.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 主花纹 */
    @Excel(name = "ui.data.column.prediction.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 主物料 */
    @Excel(name = "ui.data.column.prediction.mainMaterialDesc")
    @ApiModelProperty(value = "主物料", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 模具数量(同主花纹的模具数量) */
    @Excel(name = "ui.data.column.prediction.mouldQty")
    @ApiModelProperty(value = "模具数量(同主花纹的模具数量)", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /** 活块数量(同主花纹的物料模具数量) */
    @Excel(name = "ui.data.column.prediction.typeBlockQty")
    @ApiModelProperty(value = "活块数量(同主花纹的物料模具数量)", name = "typeBlockQty")
    @TableField(value = "TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

    /** 净需求 */
    @Excel(name = "ui.data.column.prediction.netQty")
    @ApiModelProperty(value = "净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /** 高优先级数量 */
    @Excel(name = "ui.data.column.prediction.heightQty")
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /** 排产总量 */
    @Excel(name = "ui.data.column.prediction.productionQty")
    @ApiModelProperty(value = "排产总量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟 */
    @Excel(name = "ui.data.column.prediction.planType")
    @ApiModelProperty(value = "计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 获取指定月份的生产量
     * @param monthIndex 月份索引：1, 2, 3
     * @return 生产量
     */
    public Integer getProductionQtyByMonthIndex(int monthIndex) {
        switch (monthIndex) {
            case 1:
                return this.month1;
            case 2:
                return this.month2;
            case 3:
                return this.month3;
            default:
                return 0;
        }
    }

    /**
     * 获取年份+月份的字符串表示
     * @return 年份+月份，格式：YYYYMM
     */
    public String getYearMonth() {
        return String.format("%04d%02d", this.year, this.month);
    }

    /**
     * 判断是否有生产量
     * @return 是否有生产量
     */
    public boolean hasProduction() {
        return (this.month1 != null && this.month1 > 0)
                || (this.month2 != null && this.month2 > 0)
                || (this.month3 != null && this.month3 > 0)
                || (this.productionQty != null && this.productionQty > 0);
    }

    /**
     * 获取总排产量（所有月份之和）
     * @return 总排产量
     */
    public Integer getTotalProduction() {
        int total = 0;
        if (this.month1 != null) total += this.month1;
        if (this.month2 != null) total += this.month2;
        if (this.month3 != null) total += this.month3;
        return total;
    }
}