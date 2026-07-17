package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎侧人工转机台任务明细。
 */
@Data
@ApiModel(value = "胎侧人工转机台任务明细")
public class TcChangeMachineTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程结果 ID。 */
    @ApiModelProperty(value = "排程结果 ID", required = true)
    private Long resultId;

    /** 待转班次。 */
    @ApiModelProperty(value = "待转班次", required = true)
    private Integer shiftOrder;

    /** 期望任务版本。 */
    @ApiModelProperty(value = "期望任务版本", required = true)
    private Long expectedTaskVersion;
}
