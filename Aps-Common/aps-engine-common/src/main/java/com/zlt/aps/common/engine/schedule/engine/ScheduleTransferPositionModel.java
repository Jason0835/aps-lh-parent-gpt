package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * TM/TC 人工转机台位置公共运行态模型。
 *
 * <p>只承载目标班次和插入锚点，不修改任务链本身。</p>
 */
@Data
public class ScheduleTransferPositionModel implements TaskChainTransferPosition {

    /** 目标班次顺序。 */
    private Integer shiftOrder;

    /** 目标锚点任务标识。 */
    private String anchorTaskId;
}
