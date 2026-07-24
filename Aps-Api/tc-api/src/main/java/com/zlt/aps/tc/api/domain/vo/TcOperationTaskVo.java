package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎侧人工操作异步任务响应。
 */
@Data
@ApiModel(value = "胎侧人工操作异步任务响应", description = "胎侧人工操作异步任务进度和执行结果")
public class TcOperationTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("任务编号")
    private String taskId;

    @ApiModelProperty("任务类型")
    private String taskType;

    @ApiModelProperty("任务状态")
    private String taskStatus;

    @ApiModelProperty("执行进度")
    private Integer progress;

    @ApiModelProperty("当前阶段编码")
    private String currentStage;

    @ApiModelProperty("当前阶段名称")
    private String currentStageName;

    @ApiModelProperty("提示或错误信息")
    private String message;

    @ApiModelProperty("最终影响行数")
    private Integer affectedCount;

    @ApiModelProperty("工厂编码")
    private String factoryCode;

    @ApiModelProperty("排程日期")
    private Date scheduleDate;
}
