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
 * 文件名称：SupplyOrderPool.java
 * 描    述：供应链订单池对象 t_dp_supply_order_pool
 *@author yelq
 *@date 2025-12-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@ApiModel(value = "供应链订单池对象", description = "供应链订单池对象 ")
@Data
@TableName(value = "T_DP_SUPPLY_ORDER_POOL")
public class SupplyOrderPool extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.supplyOrderPool.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.supplyOrderPool.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.supplyOrderPool.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 产品品类，TBR 全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.supplyOrderPool.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类，TBR 全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 库位 */
    @Excel(name = "ui.data.column.supplyOrderPool.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 产品分类 */
    @Excel(name = "ui.data.column.supplyOrderPool.productCategory", dictType = "product_category")
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    @TableField(value = "PRODUCT_CATEGORY")
    private String productCategory;

    /** 订单类型:数据字典：supply_order_type */
    @Excel(name = "ui.data.column.supplyOrderPool.orderType", dictType = "supply_order_type")
    @ApiModelProperty(value = "订单类型:数据字典：supply_order_type", name = "orderType")
    @TableField(value = "ORDER_TYPE")
    private String orderType;

    /** 适销区域 */
    @Excel(name = "ui.data.column.supplyOrderPool.saleArea")
    @ApiModelProperty(value = "适销区域", name = "saleArea")
    @TableField(value = "SALE_AREA")
    private String saleArea;

    /** 品牌 */
    @Excel(name = "ui.data.column.supplyOrderPool.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 物料编码 */
    @Excel(name = "ui.data.column.supplyOrderPool.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.supplyOrderPool.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 数量 */
    @Excel(name = "ui.data.column.supplyOrderPool.qty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "数量", name = "qty")
    @TableField(value = "QTY")
    private Integer qty;

    /** 近3个月月均销量 */
    @Excel(name = "ui.data.column.supplyOrderPool.threeAverageQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "近3个月月均销量", name = "threeAverageQty")
    @TableField(value = "THREE_AVERAGE_QTY")
    private Integer threeAverageQty;

    /** 近6个月月均销量 */
    @Excel(name = "ui.data.column.supplyOrderPool.sixAverageQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "近6个月月均销量", name = "sixAverageQty")
    @TableField(value = "SIX_AVERAGE_QTY")
    private Integer sixAverageQty;

    /** 滚动12个月发货频次 */
    @Excel(name = "ui.data.column.supplyOrderPool.deliveryFrequency", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "滚动12个月发货频次", name = "deliveryFrequency")
    @TableField(value = "DELIVERY_FREQUENCY")
    private Integer deliveryFrequency;

    /** 滚动12个月结构上机频次 */
    @Excel(name = "ui.data.column.supplyOrderPool.structureFrequency", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "滚动12个月结构上机频次", name = "structureFrequency")
    @TableField(value = "STRUCTURE_FREQUENCY")
    private Integer structureFrequency;

    /** 超3个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.threeOverdueStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "超3个月库存", name = "threeOverdueStockQty")
    @TableField(value = "THREE_OVERDUE_STOCK_QTY")
    private Integer threeOverdueStockQty;

    /** 超6个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.sixOverdueStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "超6个月库存", name = "sixOverdueStockQty")
    @TableField(value = "SIX_OVERDUE_STOCK_QTY")
    private Integer sixOverdueStockQty;

    /** 超9个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.nightOverdueStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "超9个月库存", name = "nightOverdueStockQty")
    @TableField(value = "NIGHT_OVERDUE_STOCK_QTY")
    private Integer nightOverdueStockQty;

    /** 超12个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.twelveOverdueStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "超12个月库存", name = "twelveOverdueStockQty")
    @TableField(value = "TWELVE_OVERDUE_STOCK_QTY")
    private Integer twelveOverdueStockQty;

    /** 备库上限 */
    @Excel(name = "ui.data.column.supplyOrderPool.stockLimit", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "备库上限", name = "stockLimit")
    @TableField(value = "STOCK_LIMIT")
    private Integer stockLimit;

    @Excel(name = "ui.data.column.supplyOrderPool.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 月均销量
     */
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(exist = false)
    private Integer averageSaleQty;
    /**
     * 无订单库存
     */
    @ApiModelProperty(value = "无订单库存", name = "notOrderStockQty")
    @TableField(exist = false)
    private Integer notOrderStockQty;
    /**
     * 结构
     */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(exist = false)
    private String structureName;

    /**
     * 以分厂+物料为维度，转换销售订单
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, materialCode);
    }
}