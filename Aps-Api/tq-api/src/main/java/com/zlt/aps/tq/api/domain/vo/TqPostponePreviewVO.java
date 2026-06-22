package com.zlt.aps.tq.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 胎圈排程跨班次推迟预览VO
 *
 * <p>用于前端预览推迟效果，展示推迟前后的对比信息，供用户确认后再执行。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈排程跨班次推迟预览", description = "跨班次推迟预览结果")
public class TqPostponePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预览批次号（用于确认时关联） */
    @ApiModelProperty(value = "预览批次号", name = "previewBatchNo")
    private String previewBatchNo;

    /** 源机台编号 */
    @ApiModelProperty(value = "源机台编号", name = "sourceMachineCode")
    private String sourceMachineCode;

    /** 源班次索引 */
    @ApiModelProperty(value = "源班次索引", name = "sourceShiftIndex")
    private Integer sourceShiftIndex;

    /** 目标机台编号（通常与源机台相同） */
    @ApiModelProperty(value = "目标机台编号", name = "targetMachineCode")
    private String targetMachineCode;

    /** 目标班次索引 */
    @ApiModelProperty(value = "目标班次索引", name = "targetShiftIndex")
    private Integer targetShiftIndex;

    /** 目标排程日期（跨天时为次日） */
    @ApiModelProperty(value = "目标排程日期", name = "targetScheduleDate")
    private Date targetScheduleDate;

    /** 是否可推迟 */
    @ApiModelProperty(value = "是否可推迟", name = "canPostpone")
    private Boolean canPostpone;

    /** 不可推迟原因（canPostpone=false时填写） */
    @ApiModelProperty(value = "不可推迟原因", name = "cannotReason")
    private String cannotReason;

    /** 推迟任务明细列表 */
    @ApiModelProperty(value = "推迟任务明细列表", name = "postponeDetails")
    private List<PostponeDetail> postponeDetails;

    /** 目标班次剩余可用时长（小时） */
    @ApiModelProperty(value = "目标班次剩余可用时长（小时）", name = "targetRemainHours")
    private Double targetRemainHours;

    /** 推迟后目标班次占用时长（小时） */
    @ApiModelProperty(value = "推迟后目标班次占用时长（小时）", name = "targetUsedHours")
    private Double targetUsedHours;

    /**
     * 推迟任务明细
     */
    @Data
    @ApiModel(value = "推迟任务明细", description = "单个推迟任务的变更明细")
    public static class PostponeDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 排程记录ID */
        @ApiModelProperty(value = "排程记录ID", name = "scheduleId")
        private Long scheduleId;

        /** 胎圈代码 */
        @ApiModelProperty(value = "胎圈代码", name = "beadCode")
        private String beadCode;

        /** 源班次索引 */
        @ApiModelProperty(value = "源班次索引", name = "sourceShiftIndex")
        private Integer sourceShiftIndex;

        /** 源计划量 */
        @ApiModelProperty(value = "源计划量", name = "sourcePlanQty")
        private Double sourcePlanQty;

        /** 源开始时间 */
        @ApiModelProperty(value = "源开始时间", name = "sourceStartTime")
        private Date sourceStartTime;

        /** 源结束时间 */
        @ApiModelProperty(value = "源结束时间", name = "sourceEndTime")
        private Date sourceEndTime;

        /** 目标班次索引 */
        @ApiModelProperty(value = "目标班次索引", name = "targetShiftIndex")
        private Integer targetShiftIndex;

        /** 目标计划量（推迟后的计划量） */
        @ApiModelProperty(value = "目标计划量", name = "targetPlanQty")
        private Double targetPlanQty;

        /** 目标开始时间 */
        @ApiModelProperty(value = "目标开始时间", name = "targetStartTime")
        private Date targetStartTime;

        /** 目标结束时间 */
        @ApiModelProperty(value = "目标结束时间", name = "targetEndTime")
        private Date targetEndTime;

        /** 目标生产顺序 */
        @ApiModelProperty(value = "目标生产顺序", name = "targetSequence")
        private Integer targetSequence;

        /** 推迟类型：1-整体推迟，2-部分推迟（拆分） */
        @ApiModelProperty(value = "推迟类型：1-整体推迟，2-部分推迟", name = "postponeType")
        private String postponeType;

        /** 推迟量（部分推迟时为拆分到下一班的量） */
        @ApiModelProperty(value = "推迟量", name = "postponeQty")
        private Double postponeQty;
    }
}
