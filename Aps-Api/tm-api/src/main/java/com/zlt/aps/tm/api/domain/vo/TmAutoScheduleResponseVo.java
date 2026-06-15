package com.zlt.aps.tm.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎面自动排程响应对象。
 *
 * <p>用于返回本次排程批次、追踪标识和结果汇总。该对象只作为契约层返回值，
 * 不承载数据库实体和任务链指针。</p>
 */
@Data
@ApiModel(value = "胎面自动排程响应对象", description = "胎面自动排程响应对象")
public class TmAutoScheduleResponseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    @ApiModelProperty(value = "是否成功", name = "success")
    private Boolean success;

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

    /** 响应消息 */
    @ApiModelProperty(value = "响应消息", name = "message")
    private String message;
}
