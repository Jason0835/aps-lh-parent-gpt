package com.zlt.aps.tm.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面人工滚动任务草稿。
 *
 * <p>该对象是与数据库实体解耦的运行态任务片段。一个横向排程结果的每个有效班次
 * 都拆成独立任务，滚动计算过程中不得持有或修改 {@code TmScheduleResult}。</p>
 */
@Data
public class TmManualTaskDraft {

    /** 本次计算内唯一任务标识 */
    private String taskId;

    /** 同一横向结果行的稳定分组标识 */
    private String resultGroupKey;

    /** 来源排程结果ID，新插单任务为空 */
    private Long sourceResultId;

    /** 来源班次 */
    private Integer sourceShiftOrder;

    /** 来源班内顺序 */
    private Integer sourceSequence;

    /** 当前机台编码 */
    private String machineCode;

    /** 当前班次 */
    private Integer shiftOrder;

    /** 当前班内顺序 */
    private Integer sequence;

    /** 最早允许排入班次，仅多班插单和指定目标班次任务使用 */
    private Integer minimumShiftOrder;

    /** 计划量 */
    private BigDecimal planQty;

    /** 当前胎面在当前机台的生产速度 */
    private BigDecimal machineSpeed;

    /** 单套工装可生产长度 */
    private BigDecimal curlRollLength;

    /** 已完成量 */
    private BigDecimal finishQty;

    /** 胎面编码 */
    private String treadCode;

    /** 主胶料编码 */
    private String glueCode;

    /** 基部胶编码 */
    private String baseGlueCode;

    /** 口型板编码 */
    private String mouthPlateCode;

    /** 数据来源 */
    private String dataSource;

    /** 原因分析 */
    private String analysis;

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
    public TmManualTaskDraft copy() {
        TmManualTaskDraft target = new TmManualTaskDraft();
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
        target.setCurlRollLength(this.curlRollLength);
        target.setFinishQty(this.finishQty);
        target.setTreadCode(this.treadCode);
        target.setGlueCode(this.glueCode);
        target.setBaseGlueCode(this.baseGlueCode);
        target.setMouthPlateCode(this.mouthPlateCode);
        target.setDataSource(this.dataSource);
        target.setAnalysis(this.analysis);
        target.setSourceStartTime(this.sourceStartTime);
        target.setSourceEndTime(this.sourceEndTime);
        target.setInsertTask(this.insertTask);
        target.setCarryoverTask(this.carryoverTask);
        target.setOperationPriority(this.operationPriority);
        target.setOperationOrder(this.operationOrder);
        return target;
    }
}
