package com.zlt.aps.tc.api.enums;

/**
 * 胎侧施工版本匹配模式枚举。
 */
public enum TcVersionMatchModeEnum {

    /** 按示方书逐班匹配。 */
    RECIPE("RECIPE", "示方书模式"),

    /** 按物料清单版本匹配。 */
    B("B", "物料清单模式");

    private final String code;

    private final String desc;

    TcVersionMatchModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按配置值解析版本模式，未知值继续回退示方书模式。
     *
     * @param code 配置编码
     * @return 匹配到的版本模式，未知值返回 RECIPE
     */
    public static TcVersionMatchModeEnum resolve(String code) {
        // 兼容早期内部实现使用的 BOM 值，对外配置口径统一为 B。
        if ("BOM".equalsIgnoreCase(code)) {
            return B;
        }
        for (TcVersionMatchModeEnum mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return RECIPE;
    }

    /**
     * 获取模式编码。
     *
     * @return 模式编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取模式说明。
     *
     * @return 模式说明
     */
    public String getDesc() {
        return desc;
    }
}
