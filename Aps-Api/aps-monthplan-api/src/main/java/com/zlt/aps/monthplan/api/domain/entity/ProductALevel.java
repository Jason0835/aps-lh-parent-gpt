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
 * 文件名称：ProductALevel.java
 * 描    述：基础数据-SAP-OEE率对象 t_mdm_product_a_level
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */

@Data
@TableName(value = "T_MDM_PRODUCT_A_LEVEL")
@ApiModel(value = "基础数据-SAP-OEE率对象", description = "基础数据-SAP-OEE率对象 ")
public class ProductALevel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.ProductALevel.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，取字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 品名
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.ProductALevel.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类，取字典：biz_product_type", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 物料编码
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.ProductALevel.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * OEE率
     */
    @ImportExcelValidated(required = true, number = true, min = 0.01, max = 99.99)
    @Excel(name = "ui.data.column.ProductALevel.aLevel")
    @ApiModelProperty(value = "OEE率，数值类型:1-100，小数点后两位", name = "aLevel")
    @TableField(value = "A_LEVEL")
    private BigDecimal aLevel;

    /**
     * 是否备货，字典：biz_yes_no，0-否，1-是
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.ProductALevel.isStockUp", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否备货", name = "isStockUp")
    @TableField(value = "IS_STOCK_UP")
    private String isStockUp;

    @Excel(name = "ui.data.column.docProductALevel.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.productDesc", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    @TableField(exist = false)
    private String productDesc;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.proSize", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(exist = false)
    private BigDecimal proSize;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.pattern", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(exist = false)
    private String pattern;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.brand", dictType = "biz_brand_type", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "品牌，字典：biz_brand_type", name = "brand")
    @TableField(exist = false)
    private String brand;


}
