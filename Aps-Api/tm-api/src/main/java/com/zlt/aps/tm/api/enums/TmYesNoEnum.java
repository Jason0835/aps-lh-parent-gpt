package com.zlt.aps.tm.api.enums;

/**
 * 胎面自动排程是非字典枚举，对应 {@code biz_yes_no}。
 */
public enum TmYesNoEnum {

    /** 是。 */
    YES("1", "是"),

    /** 否。 */
    NO("0", "否");

    private final String code;

    private final String desc;

    TmYesNoEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取字典编码。
     *
     * @return 字典编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取字典说明。
     *
     * @return 字典说明
     */
    public String getDesc() {
        return desc;
    }
}
