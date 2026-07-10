package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CD15 分裁组合，组合内两条结果共用 ORDER_NO/GROUP_NO。
 */
@Data
@Builder
public class Cd15SplitCutGroup {

    /** 分裁组合第一条候选。 */
    private Cd15ScheduleCandidate firstCandidate;
    /** 分裁组合第二条候选。 */
    private Cd15ScheduleCandidate secondCandidate;
    /** 两条候选的组合宽度。 */
    private BigDecimal combinedWidth;
}