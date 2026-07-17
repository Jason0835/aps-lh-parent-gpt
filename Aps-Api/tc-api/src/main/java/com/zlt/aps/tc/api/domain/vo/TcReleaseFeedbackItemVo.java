package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎侧发布单条MES反馈。
 */
@Data
@ApiModel(value = "胎侧发布单条MES反馈", description = "按幂等键返回的MES处理结果")
public class TcReleaseFeedbackItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** MES幂等键。 */
    @ApiModelProperty(value = "MES幂等键", name = "idempotencyKey")
    private String idempotencyKey;

    /** 反馈状态，取值SUCCESS/FAILED/TIMEOUT/UNKNOWN。 */
    @ApiModelProperty(value = "反馈状态", name = "feedbackStatus")
    private String feedbackStatus;

    /** 反馈说明。 */
    @ApiModelProperty(value = "反馈说明", name = "message")
    private String message;
}
