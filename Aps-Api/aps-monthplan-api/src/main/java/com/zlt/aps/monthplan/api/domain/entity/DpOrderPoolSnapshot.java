package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpOrderPoolSnapshot.java
 * 描    述：S1-0206.订单池快照对象 t_dp_order_pool_snapshot
 *@author yelq
 *@date 2025-12-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "S1-0206.订单池快照对象", description = "S1-0206.订单池快照对象 ")
@Data
@TableName(value = "T_DP_ORDER_POOL_SNAPSHOT")
public class DpOrderPoolSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 生产分厂编号 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 需求版本号 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.monthPlanVersion")
    @ApiModelProperty(value = "需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 品牌 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 订单类型 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.orderPriority")
    @ApiModelProperty(value = "订单类型", name = "orderPriority")
    @TableField(value = "ORDER_PRIORITY")
    private String orderPriority;

    /** 供应链优先级 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.scmPriority")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /** 需求量 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.demandQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "需求量", name = "demandQty")
    @TableField(value = "DEMAND_QTY")
    private Long demandQty;

    /** 区域 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.areaCode")
    @ApiModelProperty(value = "区域", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /** 客户编号 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.customCode")
    @ApiModelProperty(value = "客户编号", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /** 客户名称 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.customName")
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /** 客户国别 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.customNationCode")
    @ApiModelProperty(value = "客户国别", name = "customNationCode")
    @TableField(value = "CUSTOM_NATION_CODE")
    private String customNationCode;

    /** 目的国 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.destinationNationCode")
    @ApiModelProperty(value = "目的国", name = "destinationNationCode")
    @TableField(value = "DESTINATION_NATION_CODE")
    private String destinationNationCode;

    /** PO号 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.poNumber")
    @ApiModelProperty(value = "PO号", name = "poNumber")
    @TableField(value = "PO_NUMBER")
    private String poNumber;

    /** 提报日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.orderPoolSnapshot.submitDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "提报日期", name = "submitDate")
    @TableField(value = "SUBMIT_DATE")
    private Date submitDate;

    /** 年周号 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /** 动平衡 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.isDynamicBalance", dictType = "biz_yes_no")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /** 均匀性 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.isUniformity", dictType = "biz_yes_no")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /** 发货模式 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.deliverGoodsType")
    @ApiModelProperty(value = "发货模式", name = "deliverGoodsType")
    @TableField(value = "DELIVER_GOODS_TYPE")
    private String deliverGoodsType;

    /** SCMID */
    @Excel(name = "ui.data.column.orderPoolSnapshot.scmId")
    @ApiModelProperty(value = "SCMID", name = "scmId")
    @TableField(value = "SCM_ID")
    private Long scmId;

    /** 预测版本号 */
    @Excel(name = "ui.data.column.orderPoolSnapshot.predictionVersion")
    @ApiModelProperty(value = "预测版本号", name = "predictionVersion")
    @TableField(value = "PREDICTION_VERSION")
    private String predictionVersion;



}