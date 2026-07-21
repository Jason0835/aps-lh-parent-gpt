package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新任务链中的斜裁分裁组合。
 */
@Data
@Builder
public class Cd15SplitCutGroup {

    /** 第一条候选。 */
    private Cd15ScheduleCandidate firstCandidate;
    /** 第二条候选。 */
    private Cd15ScheduleCandidate secondCandidate;
    /** 两条钢带的组合有效宽度，单位毫米。 */
    private BigDecimal combinedWidth;
    /** 分裁组合稳定键。 */
    private String groupKey;
}