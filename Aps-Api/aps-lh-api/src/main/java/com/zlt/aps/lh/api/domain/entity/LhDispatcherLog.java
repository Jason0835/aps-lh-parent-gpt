package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDispatcherLog.java
 * 描    述：硫化调度员排程操作日志对象 t_lh_dispatcher_log
 *@author zlt
 *@date 2025-03-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "硫化调度员排程操作日志对象", description = "硫化调度员排程操作日志对象 ")
@Data
@TableName(value = "T_LH_DISPATCHER_LOG")
public class LhDispatcherLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 分厂编号 */
     @Excel(name = "ui.data.column.lhDispatcherLog.factoryCode", dictType = "biz_factory_name")
     @ApiModelProperty(value = "分厂编号", name = "factoryCode")
     @TableField(value = "FACTORY_CODE")
     private String factoryCode;

    /** 排程ID，对应排产表的ID */
    // @Excel(name = "ui.data.column.lhDispatcherLog.scheduleId")
    @ApiModelProperty(value = "排程ID，对应排产表的ID", name = "scheduleId")
    @TableField(value = "SCHEDULE_ID")
    private Long scheduleId;

    /** 操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE */
    @Excel(name = "ui.data.column.lhDispatcherLog.operType", dictType = "DISPATCHER_OPER_TYPE")
    @ApiModelProperty(value = "操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE", name = "operType")
    @TableField(value = "OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhDispatcherLog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 物料编号 */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 规格代码 */
    @Excel(name = "ui.data.column.lhDispatcherLog.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 操作前硫化机台 */
    @Excel(name = "ui.data.column.lhDispatcherLog.beforeMachineCode")
    @ApiModelProperty(value = "操作前硫化机台", name = "beforeMachineCode")
    @TableField(value = "BEFORE_MACHINE_CODE")
    private String beforeMachineCode;

    /** 操作前一班计划量 */
    @Excel(name = "ui.data.column.lhDispatcherLog.beforeClass1Plan")
    @ApiModelProperty(value = "操作前一班计划量", name = "beforeClass1Plan")
    @TableField(value = "BEFORE_CLASS1_PLAN")
    private Integer beforeClass1Plan;

    /** 操作前二班计划量 */
    @Excel(name = "ui.data.column.lhDispatcherLog.beforeClass2Plan")
    @ApiModelProperty(value = "操作前二班计划量", name = "beforeClass2Plan")
    @TableField(value = "BEFORE_CLASS2_PLAN")
    private Integer beforeClass2Plan;

    /** 操作三班计划量 */
    @Excel(name = "ui.data.column.lhDispatcherLog.beforeClass3Plan")
    @ApiModelProperty(value = "操作三班计划量", name = "beforeClass3Plan")
    @TableField(value = "BEFORE_CLASS3_PLAN")
    private Integer beforeClass3Plan;

    /** 操作前次日一班计划量 */
    // @Excel(name = "ui.data.column.lhDispatcherLog.beforeClass4Plan")
    @ApiModelProperty(value = "操作前次日一班计划量", name = "beforeClass4Plan")
    @TableField(value = "BEFORE_CLASS4_PLAN")
    private Integer beforeClass4Plan;

    /** 操作前次日二班计划量 */
    // @Excel(name = "ui.data.column.lhDispatcherLog.beforeClass5Plan")
    @ApiModelProperty(value = "操作前次日二班计划量", name = "beforeClass4Plan")
    @TableField(value = "BEFORE_CLASS5_PLAN")
    private Integer beforeClass5Plan;

    /** 操作前次日三班计划量 */
    // @Excel(name = "ui.data.column.lhDispatcherLog.beforeClass6Plan")
    @ApiModelProperty(value = "操作前次日三班计划量", name = "beforeClass6Plan")
    @TableField(value = "BEFORE_CLASS6_PLAN")
    private Integer beforeClass6Plan;

    /** 操作后机台编号 */
    @Excel(name = "ui.data.column.lhDispatcherLog.afterMachineCode")
    @ApiModelProperty(value = "操作后机台编号", name = "afterMachineCode")
    @TableField(value = "AFTER_MACHINE_CODE")
    private String afterMachineCode;

    /** 操作后一班计划量 */
    @Excel(name = "ui.data.column.lhDispatcherLog.afterClass1Plan")
    @ApiModelProperty(value = "操作后一班计划量", name = "afterClass1Plan")
    @TableField(value = "AFTER_CLASS1_PLAN")
    private Integer afterClass1Plan;

    /** 操作后二班计划量 */
    @Excel(name = "ui.data.column.lhDispatcherLog.afterClass2Plan")
    @ApiModelProperty(value = "操作后二班计划量", name = "afterClass2Plan")
    @TableField(value = "AFTER_CLASS2_PLAN")
    private Integer afterClass2Plan;

    /** 操作后三班计划量 */
    @Excel(name = "ui.data.column.lhDispatcherLog.afterClass3Plan")
    @ApiModelProperty(value = "操作后三班计划量", name = "afterClass3Plan")
    @TableField(value = "AFTER_CLASS3_PLAN")
    private Integer afterClass3Plan;

    /** 操作后次日一班计划量 */
    // @Excel(name = "ui.data.column.lhDispatcherLog.afterClass4Plan")
    @ApiModelProperty(value = "操作后次日一班计划量", name = "afterClass4Plan")
    @TableField(value = "AFTER_CLASS4_PLAN")
    private Integer afterClass4Plan;

    /** 操作后次日二班计划量 */
    // @Excel(name = "ui.data.column.lhDispatcherLog.afterClass5Plan")
    @ApiModelProperty(value = "操作后次日二班计划量", name = "afterClass5Plan")
    @TableField(value = "AFTER_CLASS5_PLAN")
    private Integer afterClass5Plan;

    /** 操作后次日三班计划量 */
    // @Excel(name = "ui.data.column.lhDispatcherLog.afterClass6Plan")
    @ApiModelProperty(value = "操作后次日三班计划量", name = "afterClass6Plan")
    @TableField(value = "AFTER_CLASS6_PLAN")
    private Integer afterClass6Plan;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @TableField(exist = false)
    private Date createTimeStart;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @TableField(exist = false)
    private Date createTimeEnd;

    @ApiModelProperty(value = "是否交期")
    @TableField(value = "IS_DELIVERY")
    private String isDelivery;
}