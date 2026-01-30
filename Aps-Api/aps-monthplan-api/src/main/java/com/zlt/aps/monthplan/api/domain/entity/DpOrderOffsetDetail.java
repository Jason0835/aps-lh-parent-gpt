package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderOffsetDetail.java
 * 描    述：S1-0604订单冲减分配对象 t_dp_order_offset_detail
 *@author yelq
 *@date 2025-12-30
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "S1-0604订单冲减分配对象", description = "S1-0604订单冲减分配对象 ")
@Data
@TableName(value = "T_DP_ORDER_OFFSET_DETAIL")
public class DpOrderOffsetDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.orderOffsetDetail.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.orderOffsetDetail.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.orderOffsetDetail.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 需求版本号 */
    @Excel(name = "ui.data.column.orderOffsetDetail.monthPlanVersion")
    @ApiModelProperty(value = "需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 产品品类 */
    @Excel(name = "ui.data.column.orderOffsetDetail.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 库位类别 */
    @Excel(name = "ui.data.column.orderOffsetDetail.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 区域 */
    @ApiModelProperty(value = "区域", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /**
     * 区域名称国际化字符串
     */
    @TableField(exist = false)
    private String areaCodeName;

    /**
     * 区域名称国际化
     */
    @Excel(name = "ui.data.column.orderOffsetDetail.areaCode")
    @TableField(exist = false)
    private String areaCodeNameI18n;

    /** 客户编号 */
    @Excel(name = "ui.data.column.orderOffsetDetail.customCode")
    @ApiModelProperty(value = "客户编号", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /** 客户名称 */
    @Excel(name = "ui.data.column.orderOffsetDetail.customName")
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /** 客户国别 */
    @Excel(name = "ui.data.column.orderOffsetDetail.customNationCode")
    @ApiModelProperty(value = "客户国别", name = "customNationCode")
    @TableField(value = "CUSTOM_NATION_CODE")
    private String customNationCode;

    /** 目的国 */
    @Excel(name = "ui.data.column.orderOffsetDetail.destinationNationCode")
    @ApiModelProperty(value = "目的国", name = "destinationNationCode")
    @TableField(value = "DESTINATION_NATION_CODE")
    private String destinationNationCode;

    /** PO号 */
    @Excel(name = "ui.data.column.orderOffsetDetail.poNumber")
    @ApiModelProperty(value = "PO号", name = "poNumber")
    @TableField(value = "PO_NUMBER")
    private String poNumber;

    /** 品牌 */
    @Excel(name = "ui.data.column.orderOffsetDetail.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;
    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;
    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.orderOffsetDetail.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.orderOffsetDetail.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.orderOffsetDetail.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 订单数量 */
    @Excel(name = "ui.data.column.orderOffsetDetail.orderQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "订单数量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Integer orderQty;

    /** 库存总数 */
    @Excel(name = "ui.data.column.orderOffsetDetail.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存总数", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /** 库存分配量 */
    @Excel(name = "ui.data.column.orderOffsetDetail.allocationQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存分配量", name = "allocationQty")
    @TableField(value = "ALLOCATION_QTY")
    private Integer allocationQty;

    /** 月底计划余量分配量 */
    @Excel(name = "ui.data.column.orderOffsetDetail.plannedSurplus", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月底计划余量分配量", name = "plannedSurplus")
    @TableField(value = "PLANNED_SURPLUS")
    private Integer plannedSurplus;

    /** 预计需要生产量 */
    @Excel(name = "ui.data.column.orderOffsetDetail.produceQtyDue", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "预计需要生产量", name = "produceQtyDue")
    @TableField(value = "PRODUCE_QTY_DUE")
    private Integer produceQtyDue;

    /** 计划排产量 */
    @Excel(name = "ui.data.column.orderOffsetDetail.productionQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "计划排产量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.orderOffsetDetail.scmPriority")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /** 年周号 */
    @Excel(name = "ui.data.column.orderOffsetDetail.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 动平衡 */
    //@Excel(name = "ui.data.column.orderOffsetDetail.isDynamicBalance", dictType = "biz_yes_no")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /** 均匀性 */
   // @Excel(name = "ui.data.column.orderOffsetDetail.isUniformity", dictType = "biz_yes_no")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /** EUDR，1 是 0 否 */
    @Excel(name = "ui.data.column.orderOffsetDetail.isEudr", dictType = "biz_yes_no")
    @ApiModelProperty(value = "EUDR，1 是 0 否", name = "isEudr")
    @TableField(value = "IS_EUDR")
    private String isEudr;

    /** 发货模式 */
    @Excel(name = "ui.data.column.orderOffsetDetail.deliverGoodsType", dictType = "biz_deliver_goods_type")
    @ApiModelProperty(value = "发货模式", name = "deliverGoodsType")
    @TableField(value = "DELIVER_GOODS_TYPE")
    private String deliverGoodsType;

    /** SCMID */
    @Excel(name = "ui.data.column.orderOffsetDetail.scmId")
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
    /**
     * 是否替换料
     */
    @ApiModelProperty(value = "是否替换料", name = "isAlternateMaterial")
    @TableField(exist = false)
    private String isAlternateMaterial;

    /**
     * 品牌+规格+花纹
     *
     * @return
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, brand, specifications, pattern);
    }

    /**
     * 分厂+物料描述
     *
     * @return
     */
    public String getStockGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, materialDesc);
    }
}