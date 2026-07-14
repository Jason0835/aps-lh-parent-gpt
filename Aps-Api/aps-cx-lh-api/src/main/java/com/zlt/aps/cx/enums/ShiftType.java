package com.zlt.aps.cx.enums;

import lombok.Getter;

/**
 * 班次类型枚举 — {@link com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper#determineShiftType} 的返回值，
 * 驱动开停产分支。
 *
 * <p>判定仅依赖相邻班次的 SHIFT_FLAG，<b>不</b>使用 DAY_FLAG。
 *
 * @author APS Team
 */
@Getter
public enum ShiftType {
    /** 本班 SHIFT_FLAG=0：该班次不排产或走停产精排策略 */
    CLOSED("停产班"),
    /** 本班=1 且上一班=0：开产后的第一个生产班次（首班产能、关键产品规则） */
    OPEN_START("开产首个班次"),
    /** 本班=1 且下一班=0：停产前的最后一个生产班次（停锅反推、跨天封顶） */
    BEFORE_CLOSE("停产前一个班次"),
    /** 本班=1 且上下班班均为 1：常规定额/波浪分配 */
    NORMAL("正常班");

    private final String desc;

    ShiftType(String desc) {
        this.desc = desc;
    }
}
