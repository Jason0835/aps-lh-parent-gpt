package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/** TM/TC 班次时间窗口公共运行态模型。 */
@Data
public class ScheduleShiftTimeWindowModel {
    protected Integer shiftOrder;
    protected String shiftCode;
    protected String planStartTime;
    protected String planEndTime;
    protected String crossDayFlag;
    protected BigDecimal shiftHours;
}
