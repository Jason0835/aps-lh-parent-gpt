package com.zlt.aps.tm.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 胎面排程发布任务结果明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TM_RELEASE_TASK_DETAIL")
public class TmReleaseTaskDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 发布任务ID。 */
    @TableField("TASK_ID")
    private String taskId;

    /** 排程结果ID。 */
    @TableField("RESULT_ID")
    private Long resultId;

    /** 排程批次号。 */
    @TableField("BATCH_NO")
    private String batchNo;

    /** 排程工单号。 */
    @TableField("ORDER_NO")
    private String orderNo;

    /** 提交发布时的结果任务版本。 */
    @TableField("TASK_VERSION")
    private Long taskVersion;

    /** MES幂等键。 */
    @TableField("IDEMPOTENCY_KEY")
    private String idempotencyKey;

    /** 发布前状态。 */
    @TableField("SOURCE_STATUS")
    private String sourceStatus;

    /** 当前MES反馈状态。 */
    @TableField("CALLBACK_STATUS")
    private String callbackStatus;

    /** 当前MES反馈说明。 */
    @TableField("CALLBACK_MESSAGE")
    private String callbackMessage;

    /** 当前MES反馈版本。 */
    @TableField("CALLBACK_VERSION")
    private String callbackVersion;

    /** 下发MES载荷快照JSON。 */
    @TableField("ISSUE_PAYLOAD_JSON")
    private String issuePayloadJson;

    /** 发布前状态快照。 */
    @TableField("BEFORE_STATUS")
    private String beforeStatus;

    /** 发布后状态快照。 */
    @TableField("AFTER_STATUS")
    private String afterStatus;
}
