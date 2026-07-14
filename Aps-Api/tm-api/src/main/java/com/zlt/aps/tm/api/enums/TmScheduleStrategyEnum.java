package com.zlt.aps.tm.api.enums;

/**
 * 胎面自动排程内置策略编码枚举。
 */
public enum TmScheduleStrategyEnum {

    /** 默认策略。 */
    DEFAULT("DEFAULT", "默认策略"),

    /** 连续性优先链任务策略。 */
    CONTINUITY_FIRST("CONTINUITY_FIRST", "连续性优先"),

    /** 紧急任务优先链任务策略。 */
    EMERGENCY_FIRST("EMERGENCY_FIRST", "紧急任务优先");

    private final String code;

    private final String desc;

    TmScheduleStrategyEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取策略说明。
     *
     * @return 策略说明
     */
    public String getDesc() {
        return desc;
    }
}
