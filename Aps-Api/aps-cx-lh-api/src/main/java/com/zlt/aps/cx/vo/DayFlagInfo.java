package com.zlt.aps.cx.vo;

import java.time.LocalDate;

/**
 * 按天停产标识查询结果 - 供 {@code ScheduleDayTypeHelper.findNearestDayFlag} / {@code isStopDay} 等天级兼容 API 使用。
 *
 * @author APS Team
 */
public class DayFlagInfo {
    /** 向前扫描命中的、带 DAY_FLAG 的日期 */
    public final LocalDate nearestDate;
    /** DAY_FLAG：0=停，1=开 */
    public final String dayFlag;

    public DayFlagInfo(LocalDate nearestDate, String dayFlag) {
        this.nearestDate = nearestDate;
        this.dayFlag = dayFlag;
    }
}
