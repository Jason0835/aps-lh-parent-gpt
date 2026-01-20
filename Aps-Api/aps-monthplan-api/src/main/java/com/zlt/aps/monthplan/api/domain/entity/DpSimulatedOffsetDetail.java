package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpSimulatedOffsetDetail.java
 * 描    述：预测冲减分配对象 t_dp_simulated_offset_detail
 *@author yelq
 *@date 2026-01-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "预测冲减分配对象", description = "预测冲减分配对象 ")
@Data
@TableName(value = "T_DP_SIMULATED_OFFSET_DETAIL")
public class DpSimulatedOffsetDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 需求版本号 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.monthPlanVersion")
    @ApiModelProperty(value = "需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 产品品类 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.productTypeCode")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 库位类别 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.locationType")
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 区域 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.areaCode")
    @ApiModelProperty(value = "区域", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /** 客户编号 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.customCode")
    @ApiModelProperty(value = "客户编号", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /** 客户名称 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.customName")
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /** 客户国别 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.customNationCode")
    @ApiModelProperty(value = "客户国别", name = "customNationCode")
    @TableField(value = "CUSTOM_NATION_CODE")
    private String customNationCode;

    /** 目的国 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.destinationNationCode")
    @ApiModelProperty(value = "目的国", name = "destinationNationCode")
    @TableField(value = "DESTINATION_NATION_CODE")
    private String destinationNationCode;

    /** PO号 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.poNumber")
    @ApiModelProperty(value = "PO号", name = "poNumber")
    @TableField(value = "PO_NUMBER")
    private String poNumber;

    /** 品牌 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 订单数量 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.orderQty")
    @ApiModelProperty(value = "订单数量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Integer orderQty;

    /** 库存总数 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.stockQty")
    @ApiModelProperty(value = "库存总数", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /** 已排产量 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.productionQty")
    @ApiModelProperty(value = "已排产量", name = "productionQty")
    @TableField(value = "PRODUCTION_QTY")
    private Integer productionQty;

    /** 已完成量 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.completionQty")
    @ApiModelProperty(value = "已完成量", name = "completionQty")
    @TableField(value = "COMPLETION_QTY")
    private Integer completionQty;

    /** 净需求量 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.netQty")
    @ApiModelProperty(value = "净需求量", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.scmPriority")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /** 年周号 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 动平衡 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.isDynamicBalance")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /** 均匀性 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.isUniformity")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /** 发货模式 */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.deliverGoodsType")
    @ApiModelProperty(value = "发货模式", name = "deliverGoodsType")
    @TableField(value = "DELIVER_GOODS_TYPE")
    private String deliverGoodsType;

    /** SCMID */
    @Excel(name = "ui.data.column.simulatedOffsetDetail.scmId")
    @ApiModelProperty(value = "SCMID", name = "scmId")
    @TableField(value = "SCM_ID")
    private Integer scmId;


}