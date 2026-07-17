package com.zlt.aps.tc.api.enums;

/**
 * 胎侧自动排程规则执行结果枚举。
 */
public enum TcScheduleRuleResultEnum {

    /** 规则通过。 */
    PASS("PASS", "通过"),

    /** 规则跳过。 */
    SKIP("SKIP", "跳过"),

    /** 规则拒绝。 */
    REJECT("REJECT", "拒绝"),

    /** 规则触发任务拆分。 */
    SPLIT("SPLIT", "拆分"),

    /** 规则触发需求滚动。 */
    ROLLING("ROLLING", "滚动");

    private final String code;

    private final String desc;

    TcScheduleRuleResultEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取规则结果编码。
     *
     * @return 规则结果编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取规则结果说明。
     *
     * @return 规则结果说明
     */
    public String getDesc() {
        return desc;
    }
}
