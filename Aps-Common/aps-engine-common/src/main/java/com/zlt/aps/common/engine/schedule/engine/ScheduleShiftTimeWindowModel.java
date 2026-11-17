package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/** TM/TC 班次时间窗口公共运行态模型。 */
@Data
public class ScheduleShiftTimeWindowModel {
    /**
     * 排程班次序号（如 1~6）。
     */
    protected Integer shiftOrder;
    /** 班次编码（如早/中/夜班次编码）。 */
    protected String shiftCode;
    /** 班次计划开始时间（格式 HH:mm:ss 或 HH:mm）。 */
    protected String planStartTime;
    /** 班次计划结束时间（格式 HH:mm:ss 或 HH:mm）。 */
    protected String planEndTime;
    /** 是否跨日标识（如 1 表示跨日，0 表示当日）。 */
    protected String crossDayFlag;
    /** 班次可用标准时长（小时）。 */
    protected BigDecimal shiftHours;
}
