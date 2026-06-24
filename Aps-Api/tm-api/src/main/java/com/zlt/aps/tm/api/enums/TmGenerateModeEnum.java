package com.zlt.aps.tm.api.enums;

/**
 * 胎面排程解释生成模式枚举。
 *
 * <p>用于标识解释记录来源，避免生成模式散落硬编码字符串。</p>
 */
public enum TmGenerateModeEnum {

    /** 引擎骨架模式 */
    ENGINE_SKELETON("ENGINE_SKELETON", "引擎骨架"),

    /** 引擎完整模式 */
    ENGINE_FULL("ENGINE_FULL", "引擎完整"),

    /** 人工调整模式 */
    MANUAL("MANUAL", "人工");

    /** 模式编码 */
    private final String code;

    /** 模式名称 */
    private final String desc;

    /**
     * 构造生成模式枚举。
     *
     * @param code 模式编码
     * @param desc 模式名称
     */
    TmGenerateModeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
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
     * 获取模式名称。
     *
     * @return 模式名称
     */
    public String getDesc() {
        return desc;
    }
}
