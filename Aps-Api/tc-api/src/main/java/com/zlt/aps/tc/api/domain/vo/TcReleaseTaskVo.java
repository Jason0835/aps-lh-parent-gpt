package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧发布异步任务响应。
 */
@Data
@ApiModel(value = "胎侧发布异步任务响应", description = "发布任务进度、数据版本和结果摘要")
public class TcReleaseTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID。 */
    private String taskId;

    /** 任务状态。 */
    private String taskStatus;

    /** 执行进度。 */
    private Integer progress;

    /** 当前阶段。 */
    private String currentStage;

    /** 当前阶段名称。 */
    private String currentStageName;

    /** 追踪标识。 */
    private String traceId;

    /** MES数据版本。 */
    private String dataVersion;

    /** 选中数量。 */
    private Integer selectedCount;

    /** 成功数量。 */
    private Integer successCount;

    /** 失败数量。 */
    private Integer failedCount;

    /** 超时数量。 */
    private Integer timeoutCount;

    /** 任务提示。 */
    private String message;

    /** 问题列表。 */
    @ApiModelProperty(value = "问题列表", name = "issues")
    private List<TcAutoScheduleIssueVo> issues = new ArrayList<>();

    /** 任务摘要。 */
    @ApiModelProperty(value = "任务摘要", name = "summary")
    private Map<String, Object> summary = new LinkedHashMap<>();
}
