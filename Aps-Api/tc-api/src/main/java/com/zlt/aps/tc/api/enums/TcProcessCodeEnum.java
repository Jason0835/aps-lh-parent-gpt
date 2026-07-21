package com.zlt.aps.tc.api.enums;

/**
 * 胎侧自动排程关联工序编码枚举。
 */
public enum TcProcessCodeEnum {

    /** 成型工序。 */
    FORMING("03", "成型"),

    /** 胎侧工序。 */
    SIDEWALL("04", "胎侧");

    private final String code;

    private final String desc;

    TcProcessCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取工序编码。
     *
     * @return 工序编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取工序说明。
     *
     * @return 工序说明
     */
    public String getDesc() {
        return desc;
    }
}
