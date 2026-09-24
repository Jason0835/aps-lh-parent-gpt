package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/** 按时间下机决策；窗口无合法班次时不虚构下机时间。 */
@Data
public class OffMachineDecision implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 实际下机业务日；窗口内不可下机时为空。 */
    private LocalDate planDate;
    /** 实际下机班次类型，沿用 ShiftEnum；不可下机时为空。 */
    private String shift;
    /** 实际下机时刻；不可下机时为空。 */
    private Date offMachineTime;
    /** 已提交的占用截止；不可下机时保持占用到窗口末端。 */
    private Date occupancyEndTime;
    /** 命中的业务规则或窗口内无法安排的原因。 */
    private String reason;
}
