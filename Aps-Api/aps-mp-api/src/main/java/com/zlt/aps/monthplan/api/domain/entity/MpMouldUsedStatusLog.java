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
 * 文件名称：MpMouldUseStatusLog.java
 * 描    述：S2-0406.排产过程_模具可用状态日志对象 t_mp_mould_use_status_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-01-16
 */

@Data
@TableName(value = "T_MP_MOULD_USE_STATUS_LOG")
@ApiModel(value = "S2-0406.排产过程_模具可用状态日志对象", description = "S2-0406.排产过程_模具可用状态日志对象")
public class MpMouldUsedStatusLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 型腔模号
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.mouldCode")
    @ApiModelProperty(value = "型腔模号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 需求计划版本
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 排产版本号
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.productionVersion")
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.planType")
    @ApiModelProperty(value = "计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 模具关联 01 Sku与模具 02 新模具到货
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.relationType")
    @ApiModelProperty(value = "模具关联 01 Sku与模具 02 新模具到货", name = "relationType")
    @TableField(value = "RELATION_TYPE")
    private String relationType;

    /**
     * 模具状态
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.mouldStatus")
    @ApiModelProperty(value = "模具状态", name = "mouldStatus")
    @TableField(value = "MOULD_STATUS")
    private String mouldStatus;

    /**
     * 可用天数
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.usedDays")
    @ApiModelProperty(value = "可用天数", name = "usedDays")
    @TableField(value = "USED_DAYS")
    private Integer usedDays;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 模具类型
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.mouldType")
    @ApiModelProperty(value = "模具类型", name = "mouldType")
    @TableField(value = "MOULD_TYPE")
    private String mouldType;

    /**
     * 归属工厂编号
     */
    @Excel(name = "ui.data.column.MpFactoryMouldUsedLog.owerFactoryCode")
    @ApiModelProperty(value = "归属工厂编号", name = "owerFactoryCode")
    @TableField(value = "OWER_FACTORY_CODE")
    private String owerFactoryCode;

}