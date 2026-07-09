package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎侧调度员排程操作日志对象 T_TC_DISPATCHER_LOG
 */
@ApiModel(value = "胎侧调度员排程操作日志对象", description = "胎侧调度员排程操作日志对象")
@Data
@TableName(value = "T_TC_DISPATCHER_LOG")
public class TcDispatcherLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.batchNo")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 排程结果ID */
    @ApiModelProperty(value = "排程结果ID", name = "scheduleId")
    @TableField(value = "SCHEDULE_ID")
    private Long scheduleId;

    /** 操作类型：0-转机台、1-调量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.operType", dictType = "DISPATCHER_OPER_TYPE")
    @ApiModelProperty(value = "操作类型：0-转机台、1-调量", name = "operType")
    @TableField(value = "OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tc.dispatcherLog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 胎侧编码 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.sidewallCode")
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    /** 操作前机台编码，多个逗号分割 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeMachineCode")
    @ApiModelProperty(value = "操作前机台编码，多个逗号分割", name = "beforeMachineCode")
    @TableField(value = "BEFORE_MACHINE_CODE")
    private String beforeMachineCode;

    /** 操作后机台编码，多个逗号分割 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterMachineCode")
    @ApiModelProperty(value = "操作后机台编码，多个逗号分割", name = "afterMachineCode")
    @TableField(value = "AFTER_MACHINE_CODE")
    private String afterMachineCode;

    /** 操作前1班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeClass1PlanQty")
    @ApiModelProperty(value = "操作前1班计划量", name = "beforeClass1PlanQty")
    @TableField(value = "BEFORE_CLASS1_PLAN_QTY")
    private BigDecimal beforeClass1PlanQty;

    /** 操作前2班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeClass2PlanQty")
    @ApiModelProperty(value = "操作前2班计划量", name = "beforeClass2PlanQty")
    @TableField(value = "BEFORE_CLASS2_PLAN_QTY")
    private BigDecimal beforeClass2PlanQty;

    /** 操作前3班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeClass3PlanQty")
    @ApiModelProperty(value = "操作前3班计划量", name = "beforeClass3PlanQty")
    @TableField(value = "BEFORE_CLASS3_PLAN_QTY")
    private BigDecimal beforeClass3PlanQty;

    /** 操作前4班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeClass4PlanQty")
    @ApiModelProperty(value = "操作前4班计划量", name = "beforeClass4PlanQty")
    @TableField(value = "BEFORE_CLASS4_PLAN_QTY")
    private BigDecimal beforeClass4PlanQty;

    /** 操作前5班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeClass5PlanQty")
    @ApiModelProperty(value = "操作前5班计划量", name = "beforeClass5PlanQty")
    @TableField(value = "BEFORE_CLASS5_PLAN_QTY")
    private BigDecimal beforeClass5PlanQty;

    /** 操作前6班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.beforeClass6PlanQty")
    @ApiModelProperty(value = "操作前6班计划量", name = "beforeClass6PlanQty")
    @TableField(value = "BEFORE_CLASS6_PLAN_QTY")
    private BigDecimal beforeClass6PlanQty;

    /** 操作后1班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterClass1PlanQty")
    @ApiModelProperty(value = "操作后1班计划量", name = "afterClass1PlanQty")
    @TableField(value = "AFTER_CLASS1_PLAN_QTY")
    private BigDecimal afterClass1PlanQty;

    /** 操作后2班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterClass2PlanQty")
    @ApiModelProperty(value = "操作后2班计划量", name = "afterClass2PlanQty")
    @TableField(value = "AFTER_CLASS2_PLAN_QTY")
    private BigDecimal afterClass2PlanQty;

    /** 操作后3班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterClass3PlanQty")
    @ApiModelProperty(value = "操作后3班计划量", name = "afterClass3PlanQty")
    @TableField(value = "AFTER_CLASS3_PLAN_QTY")
    private BigDecimal afterClass3PlanQty;

    /** 操作后4班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterClass4PlanQty")
    @ApiModelProperty(value = "操作后4班计划量", name = "afterClass4PlanQty")
    @TableField(value = "AFTER_CLASS4_PLAN_QTY")
    private BigDecimal afterClass4PlanQty;

    /** 操作后5班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterClass5PlanQty")
    @ApiModelProperty(value = "操作后5班计划量", name = "afterClass5PlanQty")
    @TableField(value = "AFTER_CLASS5_PLAN_QTY")
    private BigDecimal afterClass5PlanQty;

    /** 操作后6班计划量 */
    @Excel(name = "ui.data.column.tc.dispatcherLog.afterClass6PlanQty")
    @ApiModelProperty(value = "操作后6班计划量", name = "afterClass6PlanQty")
    @TableField(value = "AFTER_CLASS6_PLAN_QTY")
    private BigDecimal afterClass6PlanQty;

    /** 查询条件：开始时间 */
    @TableField(exist = false)
    private transient String startTime;

    /** 查询条件：结束时间 */
    @TableField(exist = false)
    private transient String endTime;
}