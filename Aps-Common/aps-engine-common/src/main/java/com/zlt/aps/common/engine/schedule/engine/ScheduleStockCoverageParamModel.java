package com.zlt.aps.common.engine.schedule.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 剩余库存起排班数参数在单次排程中的有效快照。
 */
@Data
@AllArgsConstructor
public class ScheduleStockCoverageParamModel {

    /**
     * 有效连续班数。
     */
    private int shiftCount;

    /**
     * 参数来源。
     */
    private String source;

    /**
     * 非法配置回退原因。
     */
    private String fallbackReason;
}
