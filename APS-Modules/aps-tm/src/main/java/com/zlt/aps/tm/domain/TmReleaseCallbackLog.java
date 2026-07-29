package com.zlt.aps.tm.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 胎面排程发布MES反馈去重日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_TM_RELEASE_CALLBACK_LOG")
public class TmReleaseCallbackLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** MES幂等键。 */
    @TableField("IDEMPOTENCY_KEY")
    private String idempotencyKey;

    /** 回调版本。 */
    @TableField("CALLBACK_VERSION")
    private String callbackVersion;

    /** 回调状态。 */
    @TableField("CALLBACK_STATUS")
    private String callbackStatus;

    /** 回调原始JSON。 */
    @TableField("CALLBACK_JSON")
    private String callbackJson;

    /** 是否实际应用到结果。 */
    @TableField("APPLIED_FLAG")
    private String appliedFlag;

    /** 未应用原因。 */
    @TableField("IGNORED_REASON")
    private String ignoredReason;
}
