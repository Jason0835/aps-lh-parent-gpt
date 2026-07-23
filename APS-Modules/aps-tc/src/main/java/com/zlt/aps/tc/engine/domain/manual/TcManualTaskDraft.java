package com.zlt.aps.tc.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎侧人工滚动任务片段，与横向结果实体完全解耦。
 */
@Data
public class TcManualTaskDraft {
    /** 运行态唯一任务标识。 */
    private String taskId;
    /** 横向结果稳定分组标识。 */
    private String resultGroupKey;
    /** 可合并业务粒度。 */
    private String mergeGrainKey;
    /** 来源结果 ID。 */
    private Long sourceResultId;
    /** 来源班次。 */
    private Integer sourceShiftOrder;
    /** 来源顺序。 */
    private Integer sourceSequence;
    /** 当前机台。 */
    private String machineCode;
    /** 当前班次。 */
    private Integer shiftOrder;
    /** 当前顺序。 */
    private Integer sequence;
    /** 最早允许排入班次。 */
    private Integer minimumShiftOrder;
    /** 计划量。 */
    private BigDecimal planQty;
    /** 当前胎侧在当前机台的生产速度。 */
    private BigDecimal machineSpeed;
    /** 单套工装可生产长度。 */
    private BigDecimal curlRollLength;
    /** 完成量。 */
    private BigDecimal finishQty;
    /** 胎侧编码。 */
    private String sidewallCode;
    /** 胶料编码。 */
    private String glueCode;
    /** 基部胶编码。 */
    private String baseGlueCode;
    /** 口型板编码。 */
    private String mouthPlateCode;
    /** 数据来源。 */
    private String dataSource;
    /** 原因分析。 */
    private String analysis;
    /** 来源开始时间。 */
    private Date sourceStartTime;
    /** 来源结束时间。 */
    private Date sourceEndTime;
    /** 是否插单任务。 */
    private boolean insertTask;
    /** 是否顺延片段。 */
    private boolean carryoverTask;
    /** 命令优先级。 */
    private Integer operationOrder;

    /**
     * 复制任务，避免计算阶段修改调用方对象。
     *
     * @return 独立任务副本
     */
    public TcManualTaskDraft copy() {
        TcManualTaskDraft target = new TcManualTaskDraft();
        target.setTaskId(this.taskId);
        target.setResultGroupKey(this.resultGroupKey);
        target.setMergeGrainKey(this.mergeGrainKey);
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
        target.setSidewallCode(this.sidewallCode);
        target.setGlueCode(this.glueCode);
        target.setBaseGlueCode(this.baseGlueCode);
        target.setMouthPlateCode(this.mouthPlateCode);
        target.setDataSource(this.dataSource);
        target.setAnalysis(this.analysis);
        target.setSourceStartTime(this.sourceStartTime);
        target.setSourceEndTime(this.sourceEndTime);
        target.setInsertTask(this.insertTask);
        target.setCarryoverTask(this.carryoverTask);
        target.setOperationOrder(this.operationOrder);
        return target;
    }
}
