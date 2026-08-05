package com.zlt.aps.gsq.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钢丝圈人工滚动单条命令。
 */
@Data
public class GsqManualRollingCommand {

    /** 操作类型 */
    private GsqManualRollingOperationEnum operationType;

    /** 来源结果分组（既有结果=排程记录ID字符串；插单=MANUAL:xxx） */
    private String resultGroupKey;

    /** 来源班次 */
    private Integer sourceShiftOrder;

    /** 来源机台 */
    private String sourceMachineCode;

    /** 目标机台 */
    private String targetMachineCode;

    /** 目标班次 */
    private Integer targetShiftOrder;

    /** 目标顺序（锚点之后的 sequence；null 时追加链尾） */
    private Integer targetSequence;

    /** 目标锚点任务ID（与 targetSequence 二选一，锚点优先） */
    private String anchorTaskId;

    /** 调整后的计划量 */
    private BigDecimal planQty;

    /** 操作原因分析 */
    private String analysis;

    /** 新插单任务（仅 INSERT 使用） */
    private GsqManualTaskDraft insertTask;

    /** 批量命令内的稳定执行顺序 */
    private Integer commandOrder;
}
