package com.zlt.aps.monthplan.api.domain.entity;

import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SupplyOrderPool.java
 * 描    述：供应链订单池对象 t_mp_supply_order_pool
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "供应链订单池对象", description = "供应链订单池对象 ")
@Data
public class SupplyOrderPool extends BaseEntity{

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.supplyOrderPool.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.supplyOrderPool.year")
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.supplyOrderPool.month")
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /** 产品品类 */
    @Excel(name = "ui.data.column.supplyOrderPool.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    private String productTypeCode;

    /** 产品品类名称 */
    @Excel(name = "ui.data.column.supplyOrderPool.productTypeName")
    @ApiModelProperty(value = "产品品类名称", name = "productTypeName")
    private String productTypeName;

    /** 内外销 */
    @Excel(name = "ui.data.column.supplyOrderPool.locationType")
    @ApiModelProperty(value = "内外销", name = "locationType")
    private String locationType;

    /** 产品分类 */
    @Excel(name = "ui.data.column.supplyOrderPool.productCategory")
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    private String productCategory;

    /** 订单类型 */
    @Excel(name = "ui.data.column.supplyOrderPool.orderType")
    @ApiModelProperty(value = "订单类型", name = "orderType")
    private String orderType;

    /** 适销区域 */
    @Excel(name = "ui.data.column.supplyOrderPool.saleArea")
    @ApiModelProperty(value = "适销区域", name = "saleArea")
    private String saleArea;

    /** 品牌 */
    @Excel(name = "ui.data.column.supplyOrderPool.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /** 物料编码 */
    @Excel(name = "ui.data.column.supplyOrderPool.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    private String productCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.supplyOrderPool.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    private String productDesc;

    /** 储备数量 */
    @Excel(name = "ui.data.column.supplyOrderPool.stockQty")
    @ApiModelProperty(value = "储备数量", name = "stockQty")
    private Long stockQty;

    /** 近3个月月均销量 */
    @Excel(name = "ui.data.column.supplyOrderPool.averageSaleQtyInThreeMonth")
    @ApiModelProperty(value = "近3个月月均销量", name = "averageSaleQtyInThreeMonth")
    private Long averageSaleQtyInThreeMonth;

    /** 近6个月月均销量 */
    @Excel(name = "ui.data.column.supplyOrderPool.averageSaleQtyInSixMonth")
    @ApiModelProperty(value = "近6个月月均销量", name = "averageSaleQtyInSixMonth")
    private Long averageSaleQtyInSixMonth;

    /** 滚动12个月发货频次 */
    @Excel(name = "ui.data.column.supplyOrderPool.deliveryFrequency")
    @ApiModelProperty(value = "滚动12个月发货频次", name = "deliveryFrequency")
    private Integer deliveryFrequency;

    /** 滚动12个月 结构上机频次 */
    @Excel(name = "ui.data.column.supplyOrderPool.structureFrequency")
    @ApiModelProperty(value = "滚动12个月 结构上机频次", name = "structureFrequency")
    private Integer structureFrequency;

    /** 超3个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.overThreeMonthStockQty")
    @ApiModelProperty(value = "超3个月库存", name = "overThreeMonthStockQty")
    private Long overThreeMonthStockQty;

    /** 超6个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.overSixStockQty")
    @ApiModelProperty(value = "超6个月库存", name = "overSixStockQty")
    private Long overSixStockQty;

    /** 超9个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.overNightStockQty")
    @ApiModelProperty(value = "超9个月库存", name = "overNightStockQty")
    private Long overNightStockQty;

    /** 超12个月库存 */
    @Excel(name = "ui.data.column.supplyOrderPool.overTwelveStockQty")
    @ApiModelProperty(value = "超12个月库存", name = "overTwelveStockQty")
    private Long overTwelveStockQty;

    /** 备库上限 */
    @Excel(name = "ui.data.column.supplyOrderPool.stockLimit")
    @ApiModelProperty(value = "备库上限", name = "stockLimit")
    private Long stockLimit;

}