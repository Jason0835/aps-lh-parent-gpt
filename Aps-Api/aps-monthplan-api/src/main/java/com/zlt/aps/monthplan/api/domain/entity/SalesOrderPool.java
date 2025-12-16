package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPool.java
 * 描    述：销售订单池对象 t_mp_sales_order_pool
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@Data
@TableName(value = "T_MP_SALES_ORDER_POOL")
@ApiModel(value = "销售订单池对象", description = "销售订单池对象 ")
public class SalesOrderPool extends BaseEntity{

    private static final long serialVersionUID = 1L;

     /** 工厂编码 默认116 */
    @Excel(name = "ui.data.column.SalesOrderPool.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码 默认116", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 产品品类，数据字典：biz_product_type，TBR 全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.SalesOrderPool.productType", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类，数据字典：biz_product_type，TBR 全钢 PCR 半钢", name = "productType")
    @TableField(value = "PRODUCT_TYPE")
    private String productType;

    /** 订单优先级，数据字典：biz_order_type，1 高优先级 3 中优先级 5 暂缓订单 */
    @Excel(name = "ui.data.column.SalesOrderPool.orderPriority", dictType = "biz_order_type")
    @ApiModelProperty(value = "订单优先级，数据字典：biz_order_type，1 高优先级 3 中优先级 5 暂缓订单", name = "orderPriority")
    @TableField(value = "ORDER_PRIORITY")
    private String orderPriority;

    /** 区域 */
    @Excel(name = "ui.data.column.SalesOrderPool.area")
    @ApiModelProperty(value = "区域", name = "area")
    @TableField(value = "AREA")
    private String area;

    /** 客户 */
    @Excel(name = "ui.data.column.SalesOrderPool.salCode")
    @ApiModelProperty(value = "客户", name = "salCode")
    @TableField(value = "SAL_CODE")
    private String salCode;

    /** 国别 */
    @Excel(name = "ui.data.column.SalesOrderPool.salNCode")
    @ApiModelProperty(value = "国别", name = "salNCode")
    @TableField(value = "SAL_N_CODE")
    private String salNCode;

    /** 目的国 */
    @Excel(name = "ui.data.column.SalesOrderPool.natCode")
    @ApiModelProperty(value = "目的国", name = "natCode")
    @TableField(value = "NAT_CODE")
    private String natCode;

    /** 品牌 */
    @Excel(name = "ui.data.column.SalesOrderPool.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** PO号 */
    @Excel(name = "ui.data.column.SalesOrderPool.salCodePo")
    @ApiModelProperty(value = "PO号", name = "salCodePo")
    @TableField(value = "SAL_CODE_PO")
    private String salCodePo;

    /** 提报日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.SalesOrderPool.billDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "提报日期", name = "billDate")
    @TableField(value = "BILL_DATE")
    private Date billDate;

    /** NC物料编码 */
    @Excel(name = "ui.data.column.SalesOrderPool.oriMaterialCode")
    @ApiModelProperty(value = "NC物料编码", name = "oriMaterialCode")
    @TableField(value = "ORI_MATERIAL_CODE")
    private String oriMaterialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.SalesOrderPool.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 数量 */
    @Excel(name = "ui.data.column.SalesOrderPool.ordQty")
    @ApiModelProperty(value = "数量", name = "ordQty")
    @TableField(value = "ORD_QTY")
    private BigDecimal ordQty;

    /** 年周号 */
    @Excel(name = "ui.data.column.SalesOrderPool.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 均匀性，1 是 0 否 */
    @Excel(name = "ui.data.column.SalesOrderPool.dynamicBalance")
    @ApiModelProperty(value = "均匀性，1 是 0 否", name = "dynamicBalance")
    @TableField(value = "DYNAMIC_BALANCE")
    private String dynamicBalance;

    /** 动平衡，1 是 0 否 */
    @Excel(name = "ui.data.column.SalesOrderPool.uniformity")
    @ApiModelProperty(value = "动平衡，1 是 0 否", name = "uniformity")
    @TableField(value = "UNIFORMITY")
    private String uniformity;

    /** EUDR，1 是 0 否 */
    @Excel(name = "ui.data.column.SalesOrderPool.eudr")
    @ApiModelProperty(value = "EUDR，1 是 0 否", name = "eudr")
    @TableField(value = "EUDR")
    private String eudr;

    /** 发货模式，数据字典：biz_deliver_goods_type，01 分批交货 02 整单发货 */
    @Excel(name = "ui.data.column.SalesOrderPool.deliverGoodsType", dictType = "biz_deliver_goods_type")
    @ApiModelProperty(value = "发货模式，数据字典：biz_deliver_goods_type，01 分批交货 02 整单发货", name = "deliverGoodsType")
    @TableField(value = "DELIVER_GOODS_TYPE")
    private String deliverGoodsType;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.SalesOrderPool.scmPriority", dictType = "biz_product_type")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /** SCM行ID */
    @ApiModelProperty(value = "SCM行ID", name = "scmDetailId")
    @TableField(value = "SCM_DETAIL_ID")
    private Long scmDetailId;

    /** 年份 */
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 以分厂+物料为维度，转换销售订单
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, oriMaterialCode);
    }
}