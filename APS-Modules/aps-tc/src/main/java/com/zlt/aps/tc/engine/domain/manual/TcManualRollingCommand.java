package com.zlt.aps.tc.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧人工滚动单条命令。
 */
@Data
public class TcManualRollingCommand {
    /** 操作类型。 */
    private TcManualRollingOperationEnum operationType;
    /** 来源结果分组。 */
    private String resultGroupKey;
    /** 来源班次。 */
    private Integer sourceShiftOrder;
    /** 来源机台。 */
    private String sourceMachineCode;
    /** 目标机台。 */
    private String targetMachineCode;
    /** 目标班次。 */
    private Integer targetShiftOrder;
    /** 目标顺序。 */
    private Integer targetSequence;
    /** 调整后计划量。 */
    private BigDecimal planQty;
    /** 原因分析。 */
    private String analysis;
    /** 新插单任务。 */
    private TcManualTaskDraft insertTask;
    /** 批内稳定顺序。 */
    private Integer commandOrder;
}
