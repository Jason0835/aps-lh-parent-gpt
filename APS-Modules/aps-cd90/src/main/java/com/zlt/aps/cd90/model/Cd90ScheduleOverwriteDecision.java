package com.zlt.aps.cd90.model;

import lombok.Builder;
import lombok.Data;

/** 自动排程旧结果覆盖决策。 */
@Data
@Builder
public class Cd90ScheduleOverwriteDecision {
    /** 是否需要用户确认。 */
    private boolean needConfirm;
    /** 是否禁止覆盖。 */
    private boolean rejected;
    /** 决策说明。 */
    private String message;
}
