package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;

import java.util.Date;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpOrderOffsetAllocation.java
 * 描    述：订单冲减分配对象 t_mp_order_offset_allocation
 *@author yelq
 *@date 2025-12-15
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "订单冲减分配对象", description = "订单冲减分配对象 ")
@Data
@TableName(value = "t_mp_order_offset_allocation")
public class MpOrderOffsetAllocation extends BaseEntity{

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 需求版本号 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.monthPlanVersion")
    @ApiModelProperty(value = "需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 产品品类 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 库位类别 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.locationType")
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 区域 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.areaCode")
    @ApiModelProperty(value = "区域", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /** 客户编号 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.customCode")
    @ApiModelProperty(value = "客户编号", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /** 客户名称 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.customName")
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /** 客户国别 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.customNationCode")
    @ApiModelProperty(value = "客户国别", name = "customNationCode")
    @TableField(value = "CUSTOM_NATION_CODE")
    private String customNationCode;

    /** 目的国 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.destinationNationCode")
    @ApiModelProperty(value = "目的国", name = "destinationNationCode")
    @TableField(value = "DESTINATION_NATION_CODE")
    private String destinationNationCode;

    /** PO号 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.poNumber")
    @ApiModelProperty(value = "PO号", name = "poNumber")
    @TableField(value = "PO_NUMBER")
    private String poNumber;

    /** 品牌 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 订单数量 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.orderQty")
    @ApiModelProperty(value = "订单数量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Long orderQty;

    /** 库存总数 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.stockQty")
    @ApiModelProperty(value = "库存总数", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Long stockQty;

    /** 库存分配量 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.allocationQty")
    @ApiModelProperty(value = "库存分配量", name = "allocationQty")
    @TableField(value = "ALLOCATION_QTY")
    private Long allocationQty;

    /** 月底计划余量分配量 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.plannedSurplus")
    @ApiModelProperty(value = "月底计划余量分配量", name = "plannedSurplus")
    @TableField(value = "PLANNED_SURPLUS")
    private Long plannedSurplus;

    /** 预计需要生产量 */
    @ApiModelProperty(value = "预计需要生产量", name = "produceQtyDue")
    @TableField(value = "PRODUCE_QTY_DUE")
    private Long produceQtyDue;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.scmPriority")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /** 年周号 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 均匀性 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.dynamicBalance")
    @ApiModelProperty(value = "均匀性", name = "dynamicBalance")
    @TableField(value = "DYNAMIC_BALANCE")
    private String dynamicBalance;

    /** 动平衡 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.uniformity")
    @ApiModelProperty(value = "动平衡", name = "uniformity")
    @TableField(value = "UNIFORMITY")
    private String uniformity;

    /** 发货模式 */
    @Excel(name = "ui.data.column.orderOffsetAllocation.deliverGoodsType")
    @ApiModelProperty(value = "发货模式", name = "deliverGoodsType")
    @TableField(value = "DELIVER_GOODS_TYPE")
    private String deliverGoodsType;

    /** SCMID */
    @Excel(name = "ui.data.column.orderOffsetAllocation.scmId")
    @ApiModelProperty(value = "SCMID", name = "scmId")
    @TableField(value = "SCM_ID")
    private Long scmId;

    /** 订单优先级，数据字典：biz_order_type，1 高优先级 3 中优先级 5 暂缓订单 */
    @ApiModelProperty(value = "订单优先级，数据字典：biz_order_type，1 高优先级 3 中优先级 5 暂缓订单", name = "orderPriority")
    @TableField(exist = false)
    private String orderPriority;

    /** 提报日期 */
    @ApiModelProperty(value = "提报日期", name = "billDate")
    @TableField(exist = false)
    private Date billDate;

}