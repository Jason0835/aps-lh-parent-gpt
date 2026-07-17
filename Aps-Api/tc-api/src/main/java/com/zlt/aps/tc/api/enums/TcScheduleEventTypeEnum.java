package com.zlt.aps.tc.api.enums;

/**
 * 胎侧排程事件类型枚举。
 *
 * <p>用于后续自动排程、插单、调量、转机台、删除、滚动重算和发布回执的日志分类。</p>
 */
public enum TcScheduleEventTypeEnum {

    /** 自动排程 */
    AUTO_SCHEDULE("AUTO_SCHEDULE", "自动排程"),

    /** 人工插单 */
    MANUAL_INSERT("MANUAL_INSERT", "人工插单"),

    /** 调整计划量 */
    CHANGE_QTY("CHANGE_QTY", "调量"),

    /** 转机台 */
    TRANSFER_MACHINE("TRANSFER_MACHINE", "转机台"),

    /** 删除任务 */
    REMOVE_TASK("REMOVE_TASK", "删除任务"),

    /** 局部重算 */
    ROLLING_RECALC("ROLLING_RECALC", "局部重算"),

    /** 发布回执 */
    RELEASE_CALLBACK("RELEASE_CALLBACK", "发布回执");

    private final String code;

    private final String desc;

    TcScheduleEventTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
