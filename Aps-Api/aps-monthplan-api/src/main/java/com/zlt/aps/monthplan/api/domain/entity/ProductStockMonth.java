package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductStockMonth.java
 * 描    述：物料月库存信息对象 t_mdm_product_stock_month
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */

@Data
@TableName(value = "T_MDM_PRODUCT_STOCK_MONTH")
@ApiModel(value = "物料月库存信息对象", description = "物料月库存信息对象 ")
public class ProductStockMonth extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthStock.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthStock.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编码
     */
    @Excel(name = "ui.data.column.monthStock.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.monthStock.productCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.monthStock.productDesc")
    // @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 库位类别 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.monthStock.locationType", dictType = "biz_stor_type")
//    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 期初库存数量
     */
    @Excel(name = "ui.data.column.monthStock.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(required = true, digits = true, min = -99999999, max = 99999999)
    @ApiModelProperty(value = "期初库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /**
     * 以分厂+物料为维度，转换库存
     *
     * @return
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, productCode);
    }

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.monthStock.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(exist = false)
    private String brand;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.monthStock.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(exist = false)
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthStock.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(exist = false)
    private String pattern;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.monthStock.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(exist = false)
    private BigDecimal proSize;


    @Excel(name = "ui.data.column.monthStock.remark")
    @ImportExcelValidated(maxLength = 300)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

}
