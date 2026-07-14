package com.zlt.aps.tm.api.enums;

/**
 * 胎面损耗规则匹配层级枚举。
 */
public enum TmLossMatchLevelEnum {

    /** 机台和胎面同时匹配。 */
    MACHINE_TREAD("MACHINE_TREAD", "机台和胎面"),

    /** 仅胎面匹配。 */
    TREAD("TREAD", "胎面"),

    /** 仅机台匹配。 */
    MACHINE("MACHINE", "机台"),

    /** 默认损耗规则。 */
    DEFAULT("DEFAULT", "默认"),

    /** 未匹配到损耗规则。 */
    NONE("NONE", "未匹配"),

    /** 兼容任务草稿已有损耗值。 */
    LEGACY_TASK("LEGACY_TASK", "任务已有损耗值");

    private final String code;

    private final String desc;

    TmLossMatchLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取匹配层级编码。
     *
     * @return 匹配层级编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取匹配层级说明。
     *
     * @return 匹配层级说明
     */
    public String getDesc() {
        return desc;
    }
}
