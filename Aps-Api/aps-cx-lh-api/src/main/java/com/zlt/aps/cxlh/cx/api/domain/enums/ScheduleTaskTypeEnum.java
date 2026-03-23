package com.zlt.aps.cxlh.cx.api.domain.enums;

import lombok.Getter;

/**
 * 排产任务类型枚举
 * 对应PDF：续作任务/新增任务/试制任务
 * @author 金宇全钢成型排产系统
 * @date 2026-03-23
 */
@Getter
public enum ScheduleTaskTypeEnum {
    CONTINUE("01", "续作任务", "IS_CONTINUE=1"),
    NEW("02", "新增任务", "IS_CONTINUE=0"),
    TRIAL("03", "试制任务", "IS_TRIAL=1");

    private final String code;
    private final String name;
    private final String condition;

    ScheduleTaskTypeEnum(String code, String name, String condition) {
        this.code = code;
        this.name = name;
        this.condition = condition;
    }

    public static ScheduleTaskTypeEnum getByCode(String code) {
        for (ScheduleTaskTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}