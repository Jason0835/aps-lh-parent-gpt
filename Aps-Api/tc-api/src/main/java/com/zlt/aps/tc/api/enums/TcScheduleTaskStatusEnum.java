package com.zlt.aps.tc.api.enums;

/**
 * 胎侧排程任务状态枚举。
 *
 * <p>用于解释表 `task_status` 字段和后续页面展示，不在骨架阶段驱动具体生产状态流转。</p>
 */
public enum TcScheduleTaskStatusEnum {

    /** 已计划 */
    PLANNED("PLANNED", "已计划"),

    /** 无需排产 */
    NO_PRODUCTION_NEEDED("NO_PRODUCTION_NEEDED", "无需排产"),

    /** 已锁定 */
    LOCKED("LOCKED", "已锁定"),

    /** 生产中 */
    RUNNING("RUNNING", "生产中"),

    /** 部分完成 */
    PART_FINISHED("PART_FINISHED", "部分完成"),

    /** 已完成 */
    FINISHED("FINISHED", "已完成"),

    /** 已取消 */
    CANCELLED("CANCELLED", "已取消"),

    /** 已拆分 */
    SPLIT("SPLIT", "已拆分");

    private final String code;

    private final String desc;

    TcScheduleTaskStatusEnum(String code, String desc) {
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
