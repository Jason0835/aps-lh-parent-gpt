package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧发布MES反馈请求。
 */
@Data
@ApiModel(value = "胎侧发布MES反馈请求", description = "按数据版本回传的幂等发布结果")
public class TcReleaseFeedbackVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发布数据版本。 */
    @ApiModelProperty(value = "发布数据版本", name = "dataVersion")
    private String dataVersion;

    /** 回调版本。 */
    @ApiModelProperty(value = "回调版本", name = "callbackVersion")
    private String callbackVersion;

    /** 单条反馈列表。 */
    @ApiModelProperty(value = "单条反馈列表", name = "items")
    private List<TcReleaseFeedbackItemVo> items = new ArrayList<>();
}
