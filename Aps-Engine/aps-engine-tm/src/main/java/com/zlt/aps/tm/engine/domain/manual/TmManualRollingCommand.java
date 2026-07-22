package com.zlt.aps.tm.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面人工滚动单条命令。
 */
@Data
public class TmManualRollingCommand {

    /** 操作类型 */
    private TmManualRollingOperationEnum operationType;

    /** 来源结果分组 */
    private String resultGroupKey;

    /** 来源班次 */
    private Integer sourceShiftOrder;

    /** 来源机台 */
    private String sourceMachineCode;

    /** 目标机台 */
    private String targetMachineCode;

    /** 目标班次 */
    private Integer targetShiftOrder;

    /** 目标顺序 */
    private Integer targetSequence;

    /** 调整后的计划量 */
    private BigDecimal planQty;

    /** 操作原因分析 */
    private String analysis;

    /** 新插单任务 */
    private TmManualTaskDraft insertTask;

    /** 批量命令内的稳定执行顺序 */
    private Integer commandOrder;
}
