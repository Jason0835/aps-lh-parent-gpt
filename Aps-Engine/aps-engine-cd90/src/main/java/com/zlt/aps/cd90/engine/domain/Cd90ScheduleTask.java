package com.zlt.aps.cd90.engine.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直裁自动排程异步任务。
 */
@Data
@TableName("t_cd90_schedule_task")
public class Cd90ScheduleTask extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对外任务ID。 */
    @TableField("TASK_ID")
    private String taskId;
    /** 工厂编码。 */
    @TableField("FACTORY_CODE")
    private String factoryCode;
    /** 排程日期。 */
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;
    /** 触发类型：TIMER或MANUAL。 */
    @TableField("TRIGGER_TYPE")
    private String triggerType;
    /** 任务状态。 */
    @TableField("TASK_STATUS")
    private String taskStatus;
    /** 执行进度，范围0至100。 */
    @TableField("PROGRESS")
    private Integer progress;
    /** 当前阶段编码。 */
    @TableField("CURRENT_STAGE")
    private String currentStage;
    /** 当前阶段中文名称。 */
    @TableField("CURRENT_STAGE_NAME")
    private String currentStageName;
    /** 成功后生成的排程批次号。 */
    @TableField("BATCH_NO")
    private String batchNo;
    /** 失败错误摘要，不保存完整异常堆栈。 */
    @TableField("ERROR_MESSAGE")
    private String errorMessage;
    /** 请求参数JSON快照。 */
    @TableField("REQUEST_SNAPSHOT")
    private String requestSnapshot;
    /** 开始执行时间。 */
    @TableField("START_TIME")
    private Date startTime;
    /** 完成或失败时间。 */
    @TableField("END_TIME")
    private Date endTime;
    /** 后台任务最后心跳时间。 */
    @TableField("LAST_HEARTBEAT_TIME")
    private Date lastHeartbeatTime;
}
