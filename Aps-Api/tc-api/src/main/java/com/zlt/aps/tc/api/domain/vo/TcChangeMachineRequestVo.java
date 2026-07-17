package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧人工批量普通转机台请求。
 */
@Data
@ApiModel(value = "胎侧人工批量普通转机台请求")
public class TcChangeMachineRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待转任务。 */
    @ApiModelProperty(value = "待转任务", required = true)
    private List<TcChangeMachineTaskVo> taskList = new ArrayList<>();

    /** 目标机台编码。 */
    @ApiModelProperty(value = "目标机台编码", required = true)
    private String targetMachineCode;

    /** 操作原因。 */
    @ApiModelProperty(value = "操作原因", required = true)
    private String reason;
}
