package com.zlt.aps.tm.api.enums;

/**
 * 胎面收尾提示枚举。
 */
public enum TmCloseOutTipEnum {

    /** 需要收尾。 */
    NEED("0", "需要收尾"),

    /** 不需要收尾。 */
    NOT_NEED("1", "不需要收尾");

    private final String code;

    private final String desc;

    TmCloseOutTipEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取收尾提示编码。
     *
     * @return 收尾提示编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取收尾提示说明。
     *
     * @return 收尾提示说明
     */
    public String getDesc() {
        return desc;
    }
}
