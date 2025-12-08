package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmFinishStock.java
 * 描    述：成品库存对象 t_mdm_finish_stock
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@ApiModel(value = "成品库存对象", description = "成品库存对象")
@Data
@TableName(value = "T_MDM_FINISH_STOCK")
public class MdmFinishStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @Excel(name = "ui.data.column.mdmFinishStock.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 产品品类，字典：biz_product_type
     */
    @Excel(name = "ui.data.column.mdmFinishStock.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类，字典：biz_product_type", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mdmFinishStock.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mdmFinishStock.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 需求版本号
     */
    @Excel(name = "ui.data.column.mdmFinishStock.requireVersion")
    @ApiModelProperty(value = "需求版本号", name = "requireVersion")
    @TableField(value = "REQUIRE_VERSION")
    private String requireVersion;

    /**
     * 品牌(物料信息.品牌)
     */
    @Excel(name = "ui.data.column.mdmFinishStock.brand")
    @ApiModelProperty(value = "品牌(物料信息.品牌)", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 产品结构(物料信息.结构)
     */
    @Excel(name = "ui.data.column.mdmFinishStock.structureName")
    @ApiModelProperty(value = "产品结构(物料信息.结构)", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * NC物料编码
     */
    @Excel(name = "ui.data.column.mdmFinishStock.materialCode")
    @ApiModelProperty(value = "NC物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.mdmFinishStock.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 库存数量
     */
    @Excel(name = "ui.data.column.mdmFinishStock.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Long stockQty;

    /**
     * 年周号
     */
    @Excel(name = "ui.data.column.mdmFinishStock.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /**
     * 均匀性，字典：biz_yes_no，1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmFinishStock.dynamicBalance", dictType = "biz_yes_no")
    @ApiModelProperty(value = "均匀性，字典：biz_yes_no，1 是 0 否", name = "dynamicBalance")
    @TableField(value = "DYNAMIC_BALANCE")
    private String dynamicBalance;

    /**
     * 动平衡，字典：biz_yes_no，1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmFinishStock.uniformity", dictType = "biz_yes_no")
    @ApiModelProperty(value = "动平衡，字典：biz_yes_no，1 是 0 否", name = "uniformity")
    @TableField(value = "UNIFORMITY")
    private String uniformity;

    /**
     * 是否超3个月胎，字典：biz_yes_no，1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmFinishStock.isExceedThreeMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超3个月胎，字典：biz_yes_no，1 是 0 否", name = "isExceedThreeMonth")
    @TableField(value = "IS_EXCEED_THREE_MONTH")
    private String isExceedThreeMonth;

    /**
     * 是否超6个月胎，字典：biz_yes_no，1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmFinishStock.isExceedSixMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超6个月胎，字典：biz_yes_no，1 是 0 否", name = "isExceedSixMonth")
    @TableField(value = "IS_EXCEED_SIX_MONTH")
    private String isExceedSixMonth;

    /**
     * 是否超9个月胎，字典：biz_yes_no，1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmFinishStock.isExceedNineMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超9个月胎，字典：biz_yes_no，1 是 0 否", name = "isExceedNineMonth")
    @TableField(value = "IS_EXCEED_NINE_MONTH")
    private String isExceedNineMonth;

    /**
     * 是否超12个月胎，字典：biz_yes_no，1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmFinishStock.isExceedTwelveMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超12个月胎，字典：biz_yes_no，1 是 0 否", name = "isExceedTwelveMonth")
    @TableField(value = "IS_EXCEED_TWELVE_MONTH")
    private String isExceedTwelveMonth;


}
