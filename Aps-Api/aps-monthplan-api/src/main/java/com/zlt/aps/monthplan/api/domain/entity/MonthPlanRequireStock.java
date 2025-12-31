package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SaleMonthPlanRequire.java
 * 描    述：月度生产需求计划库存对象 t_mdm_month_require_stock
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */

@Data
@TableName(value = "T_MP_MONTH_REQUIRE_STOCK")
@ApiModel(value = "月度生产需求计划库存对象", description = "月度生产需求计划库存对象")
public class MonthPlanRequireStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 需求计划版本号
     */
    @Excel(name = "ui.data.column.requireStock.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.requireStock.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.requireStock.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编码
     */
    @Excel(name = "ui.data.column.requireStock.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.requireStock.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.requireStock.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.requireStock.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.requireStock.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.requireStock.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.requireStock.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.requireStock.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 期初库存数量
     */
    @Excel(name = "ui.data.column.requireStock.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月结库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /**
     * 内销库存数量
     */
    @Excel(name = "ui.data.column.requireStock.domesticStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "内销月结库存数量", name = "domesticStockQty")
    @TableField(value = "DOMESTIC_STOCK_QTY")
    private Long domesticStockQty;

    /**
     * 外销库存数量
     */
    @Excel(name = "ui.data.column.requireStock.foreignStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "外销月结库存数量", name = "foreignStockQty")
    @TableField(value = "foreign_Stock_Qty")
    private Long foreignStockQty;

    /**
     * OE月结库存
     */
    @Excel(name = "ui.data.column.requireStock.oeStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "OE月结库存数量", name = "oeStockQty")
    @TableField(value = "OE_STOCK_QTY")
    private Long oeStockQty;

    /**
     * 需求对冲后-余量库存
     */
    @Excel(name = "ui.data.column.requireStock.remainingQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月结库存余量", name = "REMAINING_QTY")
    @TableField(value = "REMAINING_QTY")
    private Long remainingQty;
}
