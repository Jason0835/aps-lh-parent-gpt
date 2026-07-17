package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧自动滚动任务响应。
 */
@Data
@ApiModel(value = "胎侧自动滚动任务响应", description = "自动滚动任务进度和调整摘要")
public class TcRollingTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID。 */
    private String taskId;

    /** 任务状态。 */
    private String taskStatus;

    /** 进度。 */
    private Integer progress;

    /** 当前阶段。 */
    private String currentStage;

    /** 工厂编码。 */
    private String factoryCode;

    /** 目标班次序号。 */
    private Integer targetShiftOrder;

    /** 输入指纹。 */
    private String inputVersion;

    /** 调增数量。 */
    private BigDecimal increasedQty;

    /** 调减数量。 */
    private BigDecimal reducedQty;

    /** 问题列表。 */
    @ApiModelProperty(value = "问题列表", name = "issues")
    private List<TcAutoScheduleIssueVo> issues = new ArrayList<>();

    /** 调整摘要。 */
    @ApiModelProperty(value = "调整摘要", name = "summary")
    private Map<String, Object> summary = new LinkedHashMap<>();
}
