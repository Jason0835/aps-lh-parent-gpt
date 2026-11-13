package com.zlt.aps.common.core.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 人工操作异步任务响应。
 */
@Data
@ApiModel(value = "人工操作异步任务响应", description = "人工操作异步任务进度和执行结果")
public class OperationTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务编号。
     */
    @ApiModelProperty("任务编号")
    private String taskId;

    /**
     * 任务类型。
     */
    @ApiModelProperty("任务类型")
    private String taskType;

    /**
     * 任务状态。
     */
    @ApiModelProperty("任务状态")
    private String taskStatus;

    /**
     * 执行进度。
     */
    @ApiModelProperty("执行进度")
    private Integer progress;

    /**
     * 当前阶段编码。
     */
    @ApiModelProperty("当前阶段编码")
    private String currentStage;

    /**
     * 当前阶段名称。
     */
    @ApiModelProperty("当前阶段名称")
    private String currentStageName;

    /**
     * 提示或错误信息。
     */
    @ApiModelProperty("提示或错误信息")
    private String message;

    /**
     * 最终影响行数。
     */
    @ApiModelProperty("最终影响行数")
    private Integer affectedCount;

    /**
     * 问题明细数量，WARN 不代表任务失败。
     */
    @ApiModelProperty("问题明细数量")
    private Integer issueCount = 0;

    /**
     * 人工操作产生的问题明细。
     */
    @ApiModelProperty("问题明细")
    private List<AutoScheduleIssueVo> issues = new ArrayList<>();

    /**
     * 工厂编码。
     */
    @ApiModelProperty("工厂编码")
    private String factoryCode;

    /**
     * 排程日期。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty("排程日期")
    private LocalDate scheduleDate;

    /**
     * 设置问题明细并同步维护问题数量。
     *
     * @param issues 问题明细
     */
    public void setIssues(List<AutoScheduleIssueVo> issues) {
        this.issues = issues == null ? new ArrayList<>() : issues;
        this.issueCount = this.issues.size();
    }
}
