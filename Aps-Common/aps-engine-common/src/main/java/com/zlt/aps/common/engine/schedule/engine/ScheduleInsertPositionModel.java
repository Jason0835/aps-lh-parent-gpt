package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * TM/TC 人工插单位置公共运行态模型。
 *
 * <p>只承载目标机台、目标班次和锚点任务，不修改任务链本身。</p>
 */
@Data
public class ScheduleInsertPositionModel implements TaskChainInsertPosition {

    /** 目标机台编码。 */
    private String machineCode;

    /** 目标班次顺序。 */
    private Integer shiftOrder;

    /** 锚点任务标识。 */
    private String anchorTaskId;
}
