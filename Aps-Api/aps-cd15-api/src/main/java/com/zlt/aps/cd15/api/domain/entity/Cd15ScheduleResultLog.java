package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** CD15斜裁排程结果日志。 */
@Data
@ApiModel(value = "CD15斜裁排程结果日志", description = "CD15斜裁排程结果日志")
@TableName("t_cd15_schedule_result_log")
public class Cd15ScheduleResultLog extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对应排程结果ID */
    @ApiModelProperty("对应排程结果ID")
    @TableField("SCHEDULE_RESULT_ID")
    private Long scheduleResultId;

    /** 任务ID */
    @ApiModelProperty("任务ID")
    @TableField("TASK_ID")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.taskId")
    private String taskId;

    /** 日志类型 */
    @ApiModelProperty("日志类型")
    @TableField("LOG_TYPE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.logType")
    private String logType;

    /** 记录人 */
    @ApiModelProperty("记录人")
    @TableField("LOG_BY")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.logBy")
    private String logBy;

    /** 记录时间 */
    @ApiModelProperty("记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("LOG_TIME")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.logTime")
    private Date logTime;

    /** 原因编码 */
    @ApiModelProperty("原因编码")
    @TableField("REASON_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.reasonCode")
    private String reasonCode;

    /** 原因明细 */
    @ApiModelProperty("原因明细")
    @TableField("REASON_DETAIL")
    private String reasonDetail;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.scheduleDate")
    private Date scheduleDate;

    /** 成型批次号 */
    @ApiModelProperty("成型批次号")
    @TableField("CX_BATCH_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.cxBatchNo")
    private String cxBatchNo;

    /** 排程批次号 */
    @ApiModelProperty("排程批次号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.batchNo")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty("工单号")
    @TableField("ORDER_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.orderNo")
    private String orderNo;

    /** 分裁组号 */
    @ApiModelProperty("分裁组号")
    @TableField("GROUP_NO")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.groupNo")
    private String groupNo;

    /** 大卷代码 */
    @ApiModelProperty("大卷代码")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.bigRollCode")
    private String bigRollCode;

    /** 钢带代码 */
    @ApiModelProperty("钢带代码")
    @TableField("STEEL_STRIP_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.steelStripCode")
    private String steelStripCode;

    /** 裁断角度 */
    @ApiModelProperty("裁断角度")
    @TableField("CUTTING_ANGLE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.cuttingAngle")
    private String cuttingAngle;

    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.machineCode")
    private String machineCode;

    /** 库排号 */
    @ApiModelProperty("库排号")
    @TableField("STORAGE_LANE_CODE")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.storageLaneCode")
    private String storageLaneCode;

    /** 库存数量 */
    @ApiModelProperty("库存数量")
    @TableField("STOCK_QTY")
    private BigDecimal stockQty;

    /** 来源类型 */
    @ApiModelProperty("来源类型")
    @TableField("SOURCE_TYPE")
    private String sourceType;

    /** 放行状态 */
    @ApiModelProperty("放行状态")
    @TableField("RELEASE_STATUS")
    private String releaseStatus;

    /** 生产状态 */
    @ApiModelProperty("生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    /** 班次字段 */
    @ApiModelProperty("班次字段")
    @TableField("CLASS_FIELD")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.classField")
    private String classField;

    /** 变更前JSON */
    @ApiModelProperty("变更前JSON")
    @TableField("BEFORE_JSON")
    private String beforeJson;

    /** 变更后JSON */
    @ApiModelProperty("变更后JSON")
    @TableField("AFTER_JSON")
    private String afterJson;

    /** 变更原因 */
    @ApiModelProperty("变更原因")
    @TableField("CHANGE_REASON")
    @Excel(name = "ui.data.column.cd15ScheduleResultLog.changeReason")
    private String changeReason;
}