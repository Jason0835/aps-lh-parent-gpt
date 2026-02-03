package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author hsc
 */
@ApiModel(value = "检测项VO对象", description = "检测项VO对象")
@Data
public class MpCheckItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 检测项
     */
    @ApiModelProperty(value = "检测项", name = "checkItem")
    private String checkItem;

    /**
     * 是否通过
     */
    @ApiModelProperty(value = "是否通过", name = "isPass")
    private boolean isPass;
}
