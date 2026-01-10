package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureLog.java
 * 描    述：调整-操作日志对象 t_mp_adjust_structure_log
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "调整-操作日志对象", description = "调整-操作日志对象 ")
@Data
@TableName(value = "T_MP_ADJUST_STRUCTURE_LOG")
public class MpAdjustStructureLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 最新需求计划版本
     */
    @ApiModelProperty(value = "最新需求计划版本", name = "lastMonthPlanVersion")
    @TableField(value = "LAST_MONTH_PLAN_VERSION")
    private String lastMonthPlanVersion;

    /**
     * 排产计划版本
     */
    @ApiModelProperty(value = "排产计划版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 调整版本
     */
    @ApiModelProperty(value = "调整版本", name = "adjVersion")
    @TableField(value = "ADJ_VERSION")
    private String adjVersion;

     /** 产品结构 */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 排产机台,多个机台用逗号分隔 */
    @ApiModelProperty(value = "排产机台,多个机台用逗号分隔", name = "scheduledMachines")
    @TableField(value = "SCHEDULED_MACHINES")
    private String scheduledMachines;

    /**
     * 调整前开始日期
     */
    @ApiModelProperty(value = "调整前开始日期", name = "beforeBeginDay")
    @TableField(value = "BEFORE_BEGIN_DAY")
    private Integer beforeBeginDay;

    /**
     * 调整前结束日期
     */
    @ApiModelProperty(value = "调整前结束日期", name = "beforeEndDay")
    @TableField(value = "BEFORE_END_DAY")
    private Integer beforeEndDay;

    /**
     * 调整后开始日期
     */
    @ApiModelProperty(value = "调整后开始日期", name = "afterBeginDay")
    @TableField(value = "AFTER_BEGIN_DAY")
    private Integer afterBeginDay;

    /**
     * 调整后结束日期
     */
    @ApiModelProperty(value = "调整后结束日期", name = "afterEndDay")
    @TableField(value = "AFTER_END_DAY")
    private Integer afterEndDay;

    /** 是否发生平移 */
    @ApiModelProperty(value = "是否发生平移", name = "isTranslation")
    @TableField(value = "IS_TRANSLATION")
    private String isTranslation;

    /** 操作动作 */
    @ApiModelProperty(value = "操作动作", name = "action")
    @TableField(value = "ACTION")
    private String action;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", name = "operator")
    @TableField(value = "OPERATOR")
    private String operator;

    /** 详细日志 */
    @ApiModelProperty(value = "详细日志", name = "logDetail")
    @TableField(value = "LOG_DETAIL")
    private String logDetail;
}