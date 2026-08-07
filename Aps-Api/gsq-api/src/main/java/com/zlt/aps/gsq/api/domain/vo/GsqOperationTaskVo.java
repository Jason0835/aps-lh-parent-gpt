package com.zlt.aps.gsq.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 钢丝圈人工操作异步任务响应。
 *
 * <p>对齐胎侧 {@code TcOperationTaskVo}，用于 Controller 层 4 类人工操作
 * （插单/调量/转机台/删除）端点返回，前端按 taskId 轮询任务进度与结果。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "钢丝圈人工操作异步任务响应", description = "钢丝圈人工操作异步任务进度和执行结果")
public class GsqOperationTaskVo implements Serializable {

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

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty("排程日期")
    private Date scheduleDate;
}
