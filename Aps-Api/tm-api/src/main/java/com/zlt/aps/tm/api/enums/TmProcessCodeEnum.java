package com.zlt.aps.tm.api.enums;

/**
 * 胎面自动排程关联工序编码枚举。
 */
public enum TmProcessCodeEnum {

    /** 成型工序。 */
    FORMING("03", "成型"),

    /** 胎面工序。 */
    TREAD("04", "胎面");

    private final String code;

    private final String desc;

    TmProcessCodeEnum(String code, String desc) {
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
