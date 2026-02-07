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
 * 文件名称：MpPredictionDetail.java
 * 描    述：预测明细对象 t_mp_prediction_detail
 *@author yelq
 *@date 2026-01-16
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "预测明细对象", description = "预测明细对象 ")
@Data
@TableName(value = "T_MP_PREDICTION_DETAIL")
public class MpPredictionDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.predictionDetail.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.predictionDetail.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.predictionDetail.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 需求版本号 */
    @Excel(name = "ui.data.column.predictionDetail.monthPlanVersion")
    @ApiModelProperty(value = "需求版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 预测排产计划版本 */
    @Excel(name = "ui.data.column.predictionDetail.productionVersion")
    @ApiModelProperty(value = "预测排产计划版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 预测需求版本号 */
    @Excel(name = "ui.data.column.predictionDetail.predictionVersion")
    @ApiModelProperty(value = "预测需求版本号", name = "predictionVersion")
    @TableField(value = "PREDICTION_VERSION")
    private String predictionVersion;

    /** 预测排产版本 */
    @Excel(name = "ui.data.column.predictionDetail.predictionProductionVersion")
    @ApiModelProperty(value = "预测排产版本", name = "predictionProductionVersion")
    @TableField(value = "PREDICTION_PRODUCTION_VERSION")
    private String predictionProductionVersion;

    /** 批次号 */
    @Excel(name = "ui.data.column.predictionDetail.batchNumber")
    @ApiModelProperty(value = "批次号", name = "batchNumber")
    @TableField(value = "BATCH_NUMBER")
    private String batchNumber;

    /** 计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟 */
    @Excel(name = "ui.data.column.predictionDetail.planType")
    @ApiModelProperty(value = "计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;




}