package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQty.java
 * 描    述：月均销量对象 T_MDM_MONTH_AVG_SALE_QTY
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@ApiModel(value = "月均销量对象", description = "月均销量对象 ")
@Data
@TableName(value = "T_MDM_MONTH_AVG_SALE_QTY")
public class MpMonthlySaleQty extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 产品品类
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 内外销
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.locationType")
    @ApiModelProperty(value = "内外销", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 滚动12个月销量
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.rollTwelveMonthSaleQty")
    @ApiModelProperty(value = "滚动12个月销量", name = "rollTwelveMonthSaleQty")
    @TableField(value = "ROLL_TWELVE_MONTH_SALE_QTY")
    private Long rollTwelveMonthSaleQty;

    /**
     * 月均销量
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.averageSaleQty")
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Long averageSaleQty;

    /**
     * 近3个月均销量
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.passThreeMonthSaleQty")
    @ApiModelProperty(value = "近3个月均销量", name = "passThreeMonthSaleQty")
    @TableField(value = "PASS_THREE_MONTH_SALE_QTY")
    private Long passThreeMonthSaleQty;
    /**
     * 近6个月均销量
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.passSixMonthSaleQty")
    @ApiModelProperty(value = "近6个月均销量", name = "passThreeMonthSaleQty")
    @TableField(value = "PASS_SIX_MONTH_SALE_QTY")
    private Long passSixMonthSaleQty;
    /** 近12个月的发货频次 */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.deliveryFrequency")
    @ApiModelProperty(value = "近12个月发货频次", name = "deliveryFrequency")
    @TableField(value = "DELIVERY_FREQUENCY")
    private Integer deliveryFrequency;
    /**
     * 适销区域，多个英文逗号分隔
     */
    @Excel(name = "ui.data.column.mpMonthlySaleQty.saleArea")
    @ApiModelProperty(value = "适销区域，多个英文逗号分隔", name = "saleArea")
    @TableField(value = "SALE_AREA")
    private String saleArea;

    @ApiModelProperty(value = "区域(往前12个月所有区域)月均销量总和", name = "areaGroupList")
    @TableField(exist = false)
    private List<MpHistorySaleRecord> areaGroupList;

    @ApiModelProperty(value = "月份(往前12个月)月均销量总和", name = "monthGroupList")
    @TableField(exist = false)
    private List<MpHistorySaleRecord> monthGroupList;
}
