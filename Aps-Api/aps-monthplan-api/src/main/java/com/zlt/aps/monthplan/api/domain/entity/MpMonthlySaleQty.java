package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQty.java
 * 描    述：月均销量对象 t_mp_monthly_sale_qty
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "月均销量对象", description = "月均销量对象 ")
@Data
public class MpMonthlySaleQty extends BaseEntity{

    private static final long serialVersionUID = 1L;


    @Excel(name = "ui.data.column.monthlySaleQty.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;


    @Excel(name = "ui.data.column.monthlySaleQty.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;


    @Excel(name = "ui.data.column.monthlySaleQty.locationType")
    @ApiModelProperty(value = "内外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;


    @Excel(name = "ui.data.column.monthlySaleQty.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;


    @Excel(name = "ui.data.column.monthlySaleQty.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;


    @Excel(name = "ui.data.column.monthlySaleQty.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;


    @Excel(name = "ui.data.column.monthlySaleQty.rollMonthSaleQty")
    @ApiModelProperty(value = "滚动12个月销量", name = "rollMonthSaleQty")
    @TableField(value = "ROLL_MONTH_SALE_QTY")
    private Long rollMonthSaleQty;

    @Excel(name = "ui.data.column.monthlySaleQty.averageSaleQty")
    @ApiModelProperty(value = "滚动12个月销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Long averageSaleQty;

    /**  */
    @Excel(name = "ui.data.column.monthlySaleQty.passThreeMonthSaleQty")
    @ApiModelProperty(value = "近3个月均销量", name = "passThreeMonthSaleQty")
    @TableField(value = "PASS_THREE_MONTH_SALE_QTY")
    private Long passThreeMonthSaleQty;

    /**  */
    @Excel(name = "ui.data.column.monthlySaleQty.saleArea")
    @ApiModelProperty(value = "适销区域", name = "saleArea")
    private String saleArea;

}