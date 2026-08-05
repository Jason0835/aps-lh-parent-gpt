package com.zlt.aps.gsq.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 钢丝圈排程转机台DTO
 *
 * @author APS
 */
@Data
@ApiModel(value = "钢丝圈排程转机台DTO", description = "钢丝圈排程转机台请求参数")
public class GsqChangeMachineDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程记录ID */
    @ApiModelProperty(value = "排程记录ID", name = "id")
    private Long id;

    /** 排程日期（格式：yyyy-MM-dd） */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private String scheduleDate;

    /** 原机台编号 */
    @ApiModelProperty(value = "原机台编号", name = "oldMachineCode")
    private String oldMachineCode;

    /** 新机台编号 */
    @ApiModelProperty(value = "新机台编号", name = "newMachineCode")
    private String newMachineCode;

    /** 目标班次（1~6，为空时保持原班次） */
    @ApiModelProperty(value = "目标班次（1~6，为空时保持原班次）", name = "targetShiftOrder")
    private Integer targetShiftOrder;

    /** 目标顺序（锚点之后的 sequence；为空时追加链尾） */
    @ApiModelProperty(value = "目标顺序（锚点之后的 sequence；为空时追加链尾）", name = "targetSequence")
    private Integer targetSequence;

    /** 锚点任务ID（与 targetSequence 二选一，锚点优先） */
    @ApiModelProperty(value = "锚点任务ID（与 targetSequence 二选一，锚点优先）", name = "anchorTaskId")
    private String anchorTaskId;
}
