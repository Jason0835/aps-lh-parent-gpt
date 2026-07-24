package com.zlt.aps.tm.api.enums;

/**
 * 胎面自动排程问题类别枚举。
 */
public enum TmAutoScheduleIssueCategoryEnum {

    /** 施工信息必要字段缺失。 */
    CONSTRUCTION_FIELD_MISSING("CONSTRUCTION_FIELD_MISSING", "施工信息必要字段缺失"),

    /** 未匹配到施工信息。 */
    CONSTRUCTION_MISSING("CONSTRUCTION_MISSING", "未匹配到施工信息"),

    /** 缺少库存快照。 */
    STOCK_MISSING("STOCK_MISSING", "缺少库存快照"),

    /** 部分排程结果持久化失败。 */
    PERSIST_PARTIAL_FAILED("PERSIST_PARTIAL_FAILED", "部分排程结果持久化失败");

    private final String code;

    private final String desc;

    TmAutoScheduleIssueCategoryEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取问题类别编码。
     *
     * @return 问题类别编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取问题类别说明。
     *
     * @return 问题类别说明
     */
    public String getDesc() {
        return desc;
    }
}
