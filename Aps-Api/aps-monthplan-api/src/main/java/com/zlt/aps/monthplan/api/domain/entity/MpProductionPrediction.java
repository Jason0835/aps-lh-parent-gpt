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
 * 文件名称：MpProductionPrediction.java
 * 描    述：S2-1002.未来产量预测对象 t_mp_production_prediction
 *@author yelq
 *@date 2025-12-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "S2-1002.未来产量预测对象", description = "S2-1002.未来产量预测对象 ")
@Data
@TableName(value = "T_MP_PRODUCTION_PREDICTION")
public class MpProductionPrediction extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.productionPrediction.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.productionPrediction.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.productionPrediction.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 预测版本号 */
    @Excel(name = "ui.data.column.productionPrediction.predictionVersion")
    @ApiModelProperty(value = "预测版本号", name = "predictionVersion")
    @TableField(value = "PREDICTION_VERSION")
    private String predictionVersion;

    /** 产品品类 */
    @Excel(name = "ui.data.column.productionPrediction.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 内外销 */
    @Excel(name = "ui.data.column.productionPrediction.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "内外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 品牌 */
    @Excel(name = "ui.data.column.productionPrediction.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 物料编码 */
    @Excel(name = "ui.data.column.productionPrediction.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.productionPrediction.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** MES物料编码 */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;



    /** T月 */
    @Excel(name = "ui.data.column.productionPrediction.month1", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "T月", name = "month1")
    @TableField(value = "MONTH_1")
    private Integer month1;

    /** T+1月 */
    @Excel(name = "ui.data.column.productionPrediction.month2", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "T+1月", name = "month2")
    @TableField(value = "MONTH_2")
    private Integer month2;

    /** T+2月 */
    @Excel(name = "ui.data.column.productionPrediction.month3", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "T+2月", name = "month3")
    @TableField(value = "MONTH_3")
    private Integer month3;

    @Excel(name = "ui.data.column.productionPrediction.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @Excel(name = "ui.data.column.productionPrediction.updateTime")
    @ApiModelProperty("更新时间")
    @TableField(exist = false)
    private String updateDate;


    /** 预测需求版本号 */
    @ApiModelProperty(value = "预测需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 预测排产计划版本 */
    @ApiModelProperty(value = "预测排产计划版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 产品结构 */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 规格 */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 花纹 */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 主花纹 */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 主物料 */
    @ApiModelProperty(value = "主物料", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 模具数量(同主花纹的模具数量) */
    @ApiModelProperty(value = "模具数量(同主花纹的模具数量)", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /** 活块数量(同主花纹的物料模具数量) */
    @ApiModelProperty(value = "活块数量(同主花纹的物料模具数量)", name = "typeBlockQty")
    @TableField(value = "TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

    /** 净需求 */
    @ApiModelProperty(value = "净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /** 高优先级数量 */
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /** 排产总量 */
    @ApiModelProperty(value = "排产总量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 计划类型 */
    @ApiModelProperty(value = "计划类型", name = "planType")
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
        if (this.month1 != null) {
          total += this.month1;
        }
        if (this.month2 != null) {
          total += this.month2;
        }
        if (this.month3 != null) {
          total += this.month3;
        }
        return total;
    }

}