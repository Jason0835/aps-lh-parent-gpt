package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value = "直裁排程结果日志", description = "直裁排程结果日志")
@TableName("t_cd90_schedule_result_log")
public class Cd90ScheduleResultLog extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 对应排程结果ID */
    @ApiModelProperty("对应排程结果ID")
    @TableField("SCHEDULE_RESULT_ID")
    private Long scheduleResultId;
    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;
    /** 排程日期 */
    @ApiModelProperty("排程日期")
    @TableField("SCHEDULE_DATE")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.scheduleDate")
    private Date scheduleDate;
    /** 批次号 */
    @ApiModelProperty("批次号")
    @TableField("BATCH_NO")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.batchNo")
    private String batchNo;
    /** 工单号 */
    @TableField("ORDER_NO")
    private String orderNo;
    /** 大卷编码 */
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;
    /** 帘布代号 */
    @ApiModelProperty("帘布代号")
    @TableField("CLOTH_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.clothCode")
    private String clothCode;
    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.machineCode")
    private String machineCode;
    /** 库排号 */
    @ApiModelProperty("库排号")
    @TableField("STORAGE_LANE_CODE")
    private String storageLaneCode;
    /** 日志类型 */
    @ApiModelProperty("日志类型")
    @TableField("LOG_TYPE")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.logType")
    private String logType;
    /** 记录人 */
    @ApiModelProperty("记录人")
    @TableField("LOG_BY")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.logBy")
    private String logBy;
    /** 记录时间 */
    @ApiModelProperty("记录时间")
    @TableField("LOG_TIME")
    @Excel(name = "ui.data.column.cd90ScheduleResultLog.logTime")
    private Date logTime;
    /** 自动排程原因编码 */
    @TableField("REASON_CODE")
    private String reasonCode;
    /** 自动排程原因明细JSON */
    @TableField("REASON_DETAIL")
    private String reasonDetail;
}
