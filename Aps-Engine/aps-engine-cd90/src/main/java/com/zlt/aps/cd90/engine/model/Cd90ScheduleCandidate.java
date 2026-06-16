package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当前直裁班次的待排规格候选项。
 */
@Data
@Builder
public class Cd90ScheduleCandidate {

    /** 帘布代码。 */
    private String clothCode;
    /** 大卷代码，对应施工CORD_SPEC。 */
    private String bigRollCode;
    /** 当前直裁班次是否会发生缺料。 */
    private boolean shortageInCurrentShift;
    /** 最早缺料时点。 */
    private LocalDateTime earliestShortageTime;
    /** 库存供应成型时长。 */
    private BigDecimal stockSupplyHours;
}
