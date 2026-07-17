package com.zlt.aps.tc.api.enums;

/**
 * 胎侧自动排程异步任务状态枚举。
 */
public enum TcAutoScheduleTaskStatusEnum {

    /** 等待执行。 */
    PENDING("PENDING", "等待执行", false),

    /** 执行中。 */
    RUNNING("RUNNING", "执行中", false),

    /** 执行成功。 */
    SUCCESS("SUCCESS", "执行成功", true),

    /** 执行失败。 */
    FAILED("FAILED", "执行失败", true);

    private final String code;

    private final String desc;

    private final boolean terminal;

    TcAutoScheduleTaskStatusEnum(String code, String desc, boolean terminal) {
        this.code = code;
        this.desc = desc;
        this.terminal = terminal;
    }

    /**
     * 获取状态编码。
     *
     * @return 状态编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态说明。
     *
     * @return 状态说明
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 判断是否为终态。
     *
     * @return 是终态返回 true，否则返回 false
     */
    public boolean isTerminal() {
        return terminal;
    }
}
