package com.zlt.aps.common.core.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 同一排程结果行的人工批量调量请求。
 */
@Data
@ApiModel(value = "人工批量调量请求")
public class ChangeQtyBatchRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排程结果 ID；一个请求只能对应一条结果行。
     */
    @ApiModelProperty(value = "排程结果 ID", required = true)
    private Long resultId;

    /**
     * 提交请求时读取到的排程结果版本。
     */
    @ApiModelProperty(value = "期望任务版本", required = true)
    private Long expectedTaskVersion;

    /**
     * 本次调量原因，可为空。
     */
    @ApiModelProperty(value = "操作原因，可为空")
    private String reason;

    /**
     * 班次修改明细，最多包含六个不重复班次。
     */
    @ApiModelProperty(value = "班次修改明细", required = true)
    private List<ChangeQtyTaskVo> taskList = new ArrayList<>();
}
