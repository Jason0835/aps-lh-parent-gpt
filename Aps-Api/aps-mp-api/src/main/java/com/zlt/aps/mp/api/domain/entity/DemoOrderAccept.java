package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DemoOrderAccept.java
 * 描    述：订单接单情况演示对象 t_demo_order_accept
 *@author zlt
 *@date 2025-11-27
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "订单接单情况演示对象", description = "订单接单情况演示对象 ")
@Data
@TableName(value = "T_DEMO_ORDER_ACCEPT")
public class DemoOrderAccept extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 年份 */
    @Excel(name = "ui.data.column.demoOrderAccept.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.demoOrderAccept.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 计划订单量 */
    @Excel(name = "ui.data.column.demoOrderAccept.planOrderQty")
    @ApiModelProperty(value = "计划订单量", name = "planOrderQty")
    @TableField(value = "PLAN_ORDER_QTY")
    private Integer planOrderQty;

    /** 备货量 */
    @Excel(name = "ui.data.column.demoOrderAccept.stockUpQty")
    @ApiModelProperty(value = "备货量", name = "stockUpQty")
    @TableField(value = "STOCK_UP_QTY")
    private Integer stockUpQty;

    /** 计划需求量 */
    @Excel(name = "ui.data.column.demoOrderAccept.planDemandQty")
    @ApiModelProperty(value = "计划需求量", name = "planDemandQty")
    @TableField(value = "PLAN_DEMAND_QTY")
    private Integer planDemandQty;

    /** 实际排产计划量 */
    @Excel(name = "ui.data.column.demoOrderAccept.actualPlanQty")
    @ApiModelProperty(value = "实际排产计划量", name = "actualPlanQty")
    @TableField(value = "ACTUAL_PLAN_QTY")
    private Integer actualPlanQty;


}
