package com.zlt.aps.tm.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎面自动排程异步任务对象。
 *
 * <p>用于记录一次自动排程后台任务的状态、进度、最终响应和异常明细，
 * 前端通过任务 ID 轮询该对象展示进度。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TM_AUTO_SCHEDULE_TASK")
@ApiModel(value = "胎面自动排程异步任务对象", description = "胎面自动排程异步任务对象")
public class TmAutoScheduleTask extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对外任务 ID */
    @TableField("TASK_ID")
    @ApiModelProperty(value = "对外任务ID", name = "taskId")
    private String taskId;

    /** 工厂编码 */
    @TableField("FACTORY_CODE")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /** 排程日期 */
    @TableField("SCHEDULE_DATE")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private Date scheduleDate;

    /** 批次号 */
    @TableField("BATCH_NO")
    @ApiModelProperty(value = "批次号", name = "batchNo")
    private String batchNo;

    /** 追踪标识 */
    @TableField("TRACE_ID")
    @ApiModelProperty(value = "追踪标识", name = "traceId")
    private String traceId;

    /** 任务状态 */
    @TableField("TASK_STATUS")
    @ApiModelProperty(value = "任务状态", name = "taskStatus")
    private String taskStatus;

    /** 执行进度 */
    @TableField("PROGRESS")
    @ApiModelProperty(value = "执行进度", name = "progress")
    private Integer progress;

    /** 当前阶段编码 */
    @TableField("CURRENT_STAGE")
    @ApiModelProperty(value = "当前阶段编码", name = "currentStage")
    private String currentStage;

    /** 当前阶段名称 */
    @TableField("CURRENT_STAGE_NAME")
    @ApiModelProperty(value = "当前阶段名称", name = "currentStageName")
    private String currentStageName;

    /** 最终响应 JSON */
    @TableField("RESULT_JSON")
    @ApiModelProperty(value = "最终响应JSON", name = "resultJson")
    private String resultJson;

    /** 异常明细 JSON */
    @TableField("ISSUE_JSON")
    @ApiModelProperty(value = "异常明细JSON", name = "issueJson")
    private String issueJson;

    /** 错误摘要 */
    @TableField("ERROR_MESSAGE")
    @ApiModelProperty(value = "错误摘要", name = "errorMessage")
    private String errorMessage;

    /** 请求快照 */
    @TableField("REQUEST_SNAPSHOT")
    @ApiModelProperty(value = "请求快照", name = "requestSnapshot")
    private String requestSnapshot;

    /** 开始执行时间 */
    @TableField("START_TIME")
    @ApiModelProperty(value = "开始执行时间", name = "startTime")
    private Date startTime;

    /** 结束时间 */
    @TableField("END_TIME")
    @ApiModelProperty(value = "结束时间", name = "endTime")
    private Date endTime;

    /** 最后心跳时间 */
    @TableField("LAST_HEARTBEAT_TIME")
    @ApiModelProperty(value = "最后心跳时间", name = "lastHeartbeatTime")
    private Date lastHeartbeatTime;
}