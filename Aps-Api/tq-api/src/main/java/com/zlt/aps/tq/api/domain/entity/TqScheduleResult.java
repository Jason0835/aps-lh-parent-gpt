package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈排程结果实体类
 *
 * @author APS
 */
@ApiModel(value = "胎圈排程结果对象", description = "胎圈排程结果表实体对象")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_TQ_SCHEDULE_RESULT")
public class TqScheduleResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期(胎圈生产第一天=D+1) */
    @Excel(name = "ui.data.column.tqScheduleResult.scheduleDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 成型批次号 */
    @Excel(name = "ui.data.column.tqScheduleResult.cxBatchNo")
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /** 胎圈批次号 */
    @Excel(name = "ui.data.column.tqScheduleResult.batchNo")
    @ApiModelProperty(value = "胎圈批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @Excel(name = "ui.data.column.tqScheduleResult.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 胎圈代码 */
    @Excel(name = "ui.data.column.tqScheduleResult.beadCode")
    @ApiModelProperty(value = "胎圈代码", name = "beadCode")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.tqScheduleResult.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 三角胶口型板代码 */
    @Excel(name = "ui.data.column.tqScheduleResult.triangleGlueCode")
    @ApiModelProperty(value = "三角胶口型板代码", name = "triangleGlueCode")
    @TableField(value = "TRIANGLE_GLUE_CODE")
    private String triangleGlueCode;

    /** 英寸 */
    @Excel(name = "ui.data.column.tqScheduleResult.proSize")
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 机台编号 */
    @Excel(name = "ui.data.column.tqScheduleResult.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 月计划剩余量 */
    @Excel(name = "ui.data.column.tqScheduleResult.monthSurplusQty")
    @ApiModelProperty(value = "月计划剩余量", name = "monthSurplusQty")
    @TableField(value = "MONTH_SURPLUS_QTY")
    private Integer monthSurplusQty;

    /** 1班顺序 */
    @Excel(name = "ui.data.column.tqScheduleResult.class1Sequence")
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    @TableField(value = "CLASS1_SEQUENCE")
    private Integer class1Sequence;

    /** 1班计划量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class1PlanQty")
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /** 1班完成量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class1FinishQty")
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /** 1班原因分析 */
    @Excel(name = "ui.data.column.tqScheduleResult.class1Analysis")
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 2班顺序 */
    @Excel(name = "ui.data.column.tqScheduleResult.class2Sequence")
    @ApiModelProperty(value = "2班顺序", name = "class2Sequence")
    @TableField(value = "CLASS2_SEQUENCE")
    private Integer class2Sequence;

    /** 2班计划量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class2PlanQty")
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /** 2班完成量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class2FinishQty")
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /** 2班原因分析 */
    @Excel(name = "ui.data.column.tqScheduleResult.class2Analysis")
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 3班顺序 */
    @Excel(name = "ui.data.column.tqScheduleResult.class3Sequence")
    @ApiModelProperty(value = "3班顺序", name = "class3Sequence")
    @TableField(value = "CLASS3_SEQUENCE")
    private Integer class3Sequence;

    /** 3班计划量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class3PlanQty")
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /** 3班完成量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class3FinishQty")
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /** 3班原因分析 */
    @Excel(name = "ui.data.column.tqScheduleResult.class3Analysis")
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 4班顺序 */
    @Excel(name = "ui.data.column.tqScheduleResult.class4Sequence")
    @ApiModelProperty(value = "4班顺序", name = "class4Sequence")
    @TableField(value = "CLASS4_SEQUENCE")
    private Integer class4Sequence;

    /** 4班计划量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class4PlanQty")
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /** 4班完成量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class4FinishQty")
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /** 4班原因分析 */
    @Excel(name = "ui.data.column.tqScheduleResult.class4Analysis")
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 5班顺序 */
    @Excel(name = "ui.data.column.tqScheduleResult.class5Sequence")
    @ApiModelProperty(value = "5班顺序", name = "class5Sequence")
    @TableField(value = "CLASS5_SEQUENCE")
    private Integer class5Sequence;

    /** 5班计划量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class5PlanQty")
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /** 5班完成量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class5FinishQty")
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /** 5班原因分析 */
    @Excel(name = "ui.data.column.tqScheduleResult.class5Analysis")
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 6班顺序 */
    @Excel(name = "ui.data.column.tqScheduleResult.class6Sequence")
    @ApiModelProperty(value = "6班顺序", name = "class6Sequence")
    @TableField(value = "CLASS6_SEQUENCE")
    private Integer class6Sequence;

    /** 6班计划量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class6PlanQty")
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /** 6班完成量 */
    @Excel(name = "ui.data.column.tqScheduleResult.class6FinishQty")
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /** 6班原因分析 */
    @Excel(name = "ui.data.column.tqScheduleResult.class6Analysis")
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    // ==================== 6个班次的预计开始/结束时间和任务状态（滚动更新使用） ====================

    /** 1班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班预计开始时间", name = "class1StartTime")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /** 1班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班预计结束时间", name = "class1EndTime")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

    /** 1班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.tqScheduleResult.class1TaskStatus", dictType = "TQ_TASK_STATUS")
    @ApiModelProperty(value = "1班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class1TaskStatus")
    @TableField(value = "CLASS1_TASK_STATUS")
    private String class1TaskStatus;

    /** 2班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班预计开始时间", name = "class2StartTime")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /** 2班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班预计结束时间", name = "class2EndTime")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

    /** 2班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.tqScheduleResult.class2TaskStatus", dictType = "TQ_TASK_STATUS")
    @ApiModelProperty(value = "2班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class2TaskStatus")
    @TableField(value = "CLASS2_TASK_STATUS")
    private String class2TaskStatus;

    /** 3班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班预计开始时间", name = "class3StartTime")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /** 3班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班预计结束时间", name = "class3EndTime")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

    /** 3班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.tqScheduleResult.class3TaskStatus", dictType = "TQ_TASK_STATUS")
    @ApiModelProperty(value = "3班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class3TaskStatus")
    @TableField(value = "CLASS3_TASK_STATUS")
    private String class3TaskStatus;

    /** 4班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班预计开始时间", name = "class4StartTime")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /** 4班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班预计结束时间", name = "class4EndTime")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

    /** 4班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.tqScheduleResult.class4TaskStatus", dictType = "TQ_TASK_STATUS")
    @ApiModelProperty(value = "4班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class4TaskStatus")
    @TableField(value = "CLASS4_TASK_STATUS")
    private String class4TaskStatus;

    /** 5班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班预计开始时间", name = "class5StartTime")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /** 5班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班预计结束时间", name = "class5EndTime")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

    /** 5班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.tqScheduleResult.class5TaskStatus", dictType = "TQ_TASK_STATUS")
    @ApiModelProperty(value = "5班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class5TaskStatus")
    @TableField(value = "CLASS5_TASK_STATUS")
    private String class5TaskStatus;

    /** 6班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班预计开始时间", name = "class6StartTime")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /** 6班预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班预计结束时间", name = "class6EndTime")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

    /** 6班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    @Excel(name = "ui.data.column.tqScheduleResult.class6TaskStatus", dictType = "TQ_TASK_STATUS")
    @ApiModelProperty(value = "6班任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟）", name = "class6TaskStatus")
    @TableField(value = "CLASS6_TASK_STATUS")
    private String class6TaskStatus;

    /** 数据来源：0-自动排程，1-插单，2-导入 */
    @Excel(name = "ui.data.column.tqScheduleResult.dataSource", dictType = "TQ_DATA_SOURCE")
    @ApiModelProperty(value = "数据来源：0-自动排程，1-插单，2-导入", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 是否发布 */
    @Excel(name = "ui.data.column.tqScheduleResult.isRelease", dictType = "IS_RELEASE")
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 库存量 */
    @Excel(name = "ui.data.column.tqScheduleResult.stockQty")
    @ApiModelProperty(value = "库存量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /** 排程日期查询（精确匹配） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(exist = false)
    private Date scheduleDateQuery;

    /** 分厂编码（查询参数，非数据库字段） */
    @TableField(exist = false)
    private String factoryCode;

    /** 机台名称（反显） */
    @TableField(exist = false)
    private String machineName;
}
