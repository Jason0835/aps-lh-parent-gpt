package com.zlt.aps.common.engine.schedule.constraint;

import lombok.Data;

/**
 * 收尾任务两类含损耗候选及最终选择结果。
 */
@Data
public class ScheduleTailPlanQtyAdjustmentResult {

    /**
     * 计划量候选的损耗结算结果。
     */
    private SchedulePlanQtyAdjustmentResult planCandidate;

    /**
     * 收尾上限候选的损耗结算结果。
     */
    private SchedulePlanQtyAdjustmentResult tailLimitCandidate;

    /**
     * 取小后的最终候选结果。
     */
    private SchedulePlanQtyAdjustmentResult selectedCandidate;
}
