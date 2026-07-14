package com.zlt.aps.tm.api.enums;

/**
 * 胎面自动排程问题级别枚举。
 */
public enum TmAutoScheduleIssueLevelEnum {

    /** 阻断执行的错误。 */
    ERROR("ERROR", "错误"),

    /** 允许继续排程的警告。 */
    WARN("WARN", "警告");

    private final String code;

    private final String desc;

    TmAutoScheduleIssueLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取问题级别编码。
     *
     * @return 问题级别编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取问题级别说明。
     *
     * @return 问题级别说明
     */
    public String getDesc() {
        return desc;
    }
}
