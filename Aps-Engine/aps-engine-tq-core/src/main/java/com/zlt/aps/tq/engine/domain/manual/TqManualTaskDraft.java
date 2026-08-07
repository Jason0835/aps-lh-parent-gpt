package com.zlt.aps.tq.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎圈人工滚动任务草稿。
 *
 * <p>与数据库实体 TqScheduleResult 解耦的运行态任务片段。
 * 计划量使用 BigDecimal 便于滚动计算，持久化时由 Service 层转 Integer。</p>
 */
@Data
public class TqManualTaskDraft {

    /** 本次计算内唯一任务标识 */
    private String taskId;

    /** 同一横向结果行的稳定分组标识（既有=排程记录ID；新插单=MANUAL:xxx；转机台=MOVE:xxx） */
    private String resultGroupKey;

    /** 来源排程结果ID，新插单任务为空 */
    private Long sourceResultId;

    /** 来源班次 */
    private Integer sourceShiftOrder;

    /** 来源班内顺序 */
    private Integer sourceSequence;

    /** 当前机台编码 */
    private String machineCode;

    /** 当前班次（1~6） */
    private Integer shiftOrder;

    /** 当前班内顺序 */
    private Integer sequence;

    /** 最早允许排入班次（多班插单使用） */
    private Integer minimumShiftOrder;

    /** 计划量（BigDecimal，持久化时转 Integer） */
    private BigDecimal planQty;

    /** 机台生产速度（件/小时） */
    private BigDecimal machineSpeed;

    /** 已完成量 */
    private BigDecimal finishQty;

    /** 胎圈编码 */
    private String beadCode;

    /** 三角胶口型板编码（胎圈规格切换判断依据） */
    private String triangleGlueCode;

    /** 英寸尺寸 */
    private String proSize;

    /** 数据来源（"1"=插单） */
    private String dataSource;

    /** 原因分析 */
    private String analysis;

    /** 未排原因编码 */
    private String unplannedReasonCode;

    /** 未排原因描述 */
    private String unplannedReasonDesc;

    /** 来源班次开始时间，仅在任务未跨班时回写 */
    private Date sourceStartTime;

    /** 来源班次结束时间，仅在任务未跨班时回写 */
    private Date sourceEndTime;

    /** 是否人工插单任务 */
    private boolean insertTask;

    /** 是否跨班顺延任务 */
    private boolean carryoverTask;

    /** 同顺序时是否优先进入任务链 */
    private boolean operationPriority;

    /** 批量命令内的稳定优先级 */
    private Integer operationOrder;

    /**
     * 创建与当前任务完全解耦的副本。
     *
     * @return 任务副本
     */
    public TqManualTaskDraft copy() {
        TqManualTaskDraft target = new TqManualTaskDraft();
        target.setTaskId(this.taskId);
        target.setResultGroupKey(this.resultGroupKey);
        target.setSourceResultId(this.sourceResultId);
        target.setSourceShiftOrder(this.sourceShiftOrder);
        target.setSourceSequence(this.sourceSequence);
        target.setMachineCode(this.machineCode);
        target.setShiftOrder(this.shiftOrder);
        target.setSequence(this.sequence);
        target.setMinimumShiftOrder(this.minimumShiftOrder);
        target.setPlanQty(this.planQty);
        target.setMachineSpeed(this.machineSpeed);
        target.setFinishQty(this.finishQty);
        target.setBeadCode(this.beadCode);
        target.setTriangleGlueCode(this.triangleGlueCode);
        target.setProSize(this.proSize);
        target.setDataSource(this.dataSource);
        target.setAnalysis(this.analysis);
        target.setUnplannedReasonCode(this.unplannedReasonCode);
        target.setUnplannedReasonDesc(this.unplannedReasonDesc);
        target.setSourceStartTime(this.sourceStartTime);
        target.setSourceEndTime(this.sourceEndTime);
        target.setInsertTask(this.insertTask);
        target.setCarryoverTask(this.carryoverTask);
        target.setOperationPriority(this.operationPriority);
        target.setOperationOrder(this.operationOrder);
        return target;
    }
}
