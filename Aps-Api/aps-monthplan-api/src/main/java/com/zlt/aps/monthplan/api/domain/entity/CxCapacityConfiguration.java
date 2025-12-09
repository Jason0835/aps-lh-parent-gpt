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
 * 文件名称：SizeCapacityConfiguration.java
 * 描    述：成型产能分配(结构) T_MP_STRUCTURE_ALLOCATION
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20251208
 */

@Data
@TableName(value = "T_MP_STRUCTURE_ALLOCATION")
@ApiModel(value = "成型产能分配配置(结构)对象", description = "成型产能分配配置(结构)对象")
public class CxCapacityConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.cxCapacity.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.cxCapacity.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.cxCapacity.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.cxCapacity.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.cxCapacity.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.cxCapacity.planType", dictType = "biz_plan_type")
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.cxCapacity.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;


    /**
     * 排产净需求
     */
    @Excel(name = "ui.data.column.cxCapacity.netQty")
    @ApiModelProperty(value = "总需求量", name = "netQty")
    @TableField(value = "NET_QTY")
    private Long netQty;

    /**
     * 排产净需求(含损耗)
     */
    @Excel(name = "ui.data.column.cxCapacity.lossQty")
    @ApiModelProperty(value = "净需求量", name = "lossQty")
    @TableField(value = "LOSS_QTY")
    private Long lossQty;

    /**
     * 成型机编码
     */
    @Excel(name = "ui.data.column.cxCapacity.cxMachineCode")
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.cxCapacity.beginDay")
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    @TableField(value = "BEGIN_DAY")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.cxCapacity.endDay")
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;

    /**
     * 分配天数
     */
    @Excel(name = "ui.data.column.cxCapacity.allotDays")
    @ApiModelProperty(value = "分配天数", name = "allotDays")
    @TableField(value = "ALLOT_DAYS")
    private Integer allotDays;

}