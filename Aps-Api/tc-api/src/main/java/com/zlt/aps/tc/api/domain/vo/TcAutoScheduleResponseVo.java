package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎侧自动排程响应对象。
 *
 * <p>用于返回自动排程校验、执行结果和前端是否需要二次确认的信息。
 * 该对象不持有数据库实体和任务链指针。</p>
 */
@Data
@ApiModel(value = "胎侧自动排程响应对象", description = "胎侧自动排程响应对象")
public class TcAutoScheduleResponseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对外任务 ID */
    @ApiModelProperty(value = "对外任务ID", name = "taskId")
    private String taskId;

    /** 任务状态 */
    @ApiModelProperty(value = "任务状态", name = "taskStatus")
    private String taskStatus;

    /** 执行进度 */
    @ApiModelProperty(value = "执行进度", name = "progress")
    private Integer progress;

    /** 当前阶段编码 */
    @ApiModelProperty(value = "当前阶段编码", name = "currentStage")
    private String currentStage;

    /** 当前阶段名称 */
    @ApiModelProperty(value = "当前阶段名称", name = "currentStageName")
    private String currentStageName;

    /** 异常明细数量 */
    @ApiModelProperty(value = "异常明细数量", name = "issueCount")
    private Integer issueCount;

    /** 异常明细 */
    @ApiModelProperty(value = "异常明细", name = "issues")
    private List<TcAutoScheduleIssueVo> issues = new ArrayList<>();

    /** 是否成功 */
    @ApiModelProperty(value = "是否成功", name = "success")
    private Boolean success;

    /** 是否需要前端确认覆盖旧批次 */
    @ApiModelProperty(value = "是否需要前端确认覆盖旧批次", name = "confirmRequired")
    private Boolean confirmRequired;

    /** 批次号 */
    @ApiModelProperty(value = "批次号", name = "batchNo")
    private String batchNo;

    /** 追踪标识 */
    @ApiModelProperty(value = "追踪标识", name = "traceId")
    private String traceId;

    /** 排程结果数量 */
    @ApiModelProperty(value = "排程结果数量", name = "resultCount")
    private Integer resultCount;

    /** 未排任务数量 */
    @ApiModelProperty(value = "未排任务数量", name = "unplannedCount")
    private Integer unplannedCount;

    /** 本批次覆盖率、未排率、利用率等质量指标 */
    @ApiModelProperty(value = "排程质量指标摘要", name = "summary")
    private Map<String, Object> summary = new LinkedHashMap<>();

    /** 响应消息 */
    @ApiModelProperty(value = "响应消息", name = "message")
    private String message;
}
