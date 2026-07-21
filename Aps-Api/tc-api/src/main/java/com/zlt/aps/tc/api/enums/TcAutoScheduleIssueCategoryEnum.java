package com.zlt.aps.tc.api.enums;

/**
 * 胎侧自动排程问题类别枚举。
 */
public enum TcAutoScheduleIssueCategoryEnum {

    /** 施工信息必要字段缺失。 */
    CONSTRUCTION_FIELD_MISSING("CONSTRUCTION_FIELD_MISSING", "施工信息必要字段缺失"),

    /** 未匹配到施工信息。 */
    CONSTRUCTION_MISSING("CONSTRUCTION_MISSING", "未匹配到施工信息"),

    /** 缺少库存快照，按配置决定继续或阻断。 */
    STOCK_MISSING("STOCK_MISSING", "缺少库存快照"),

    /** 核心持久化失败。 */
    PERSIST_FAILED("PERSIST_FAILED", "核心持久化失败"),

    /** 部分排程结果持久化失败。 */
    PERSIST_PARTIAL_FAILED("PERSIST_PARTIAL_FAILED", "部分排程结果持久化失败");

    private final String code;

    private final String desc;

    TcAutoScheduleIssueCategoryEnum(String code, String desc) {
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
