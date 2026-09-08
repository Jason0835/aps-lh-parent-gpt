package com.zlt.aps.common.engine.enums;

/**
 * 自动排程问题类别枚举。
 */
public enum ScheduleIssueCategoryEnum {

    /** 施工信息必要字段缺失。 */
    CONSTRUCTION_FIELD_MISSING("CONSTRUCTION_FIELD_MISSING", "施工信息必要字段缺失",
            "ui.schedule.issue.suggestion.constructionFieldMissing"),

    /** 未匹配到施工信息。 */
    CONSTRUCTION_MISSING("CONSTRUCTION_MISSING", "未匹配到施工信息",
            "ui.schedule.issue.suggestion.constructionMissing"),

    /** 缺少库存快照。 */
    STOCK_MISSING("STOCK_MISSING", "缺少库存快照", "ui.schedule.issue.suggestion.stockMissing"),

    /** 计划量汇总组生产属性冲突。 */
    PLAN_GROUP_ATTRIBUTE_CONFLICT("PLAN_GROUP_ATTRIBUTE_CONFLICT", "计划量汇总组生产属性冲突",
            "ui.schedule.issue.suggestion.planGroupAttributeConflict"),

    /** 自动排程业务异常。 */
    AUTO_SCHEDULE_BUSINESS_ERROR("AUTO_SCHEDULE_BUSINESS_ERROR", "自动排程业务异常",
            "ui.schedule.issue.suggestion.businessError"),

    /** 自动排程技术异常。 */
    AUTO_SCHEDULE_SYSTEM_ERROR("AUTO_SCHEDULE_SYSTEM_ERROR", "自动排程技术异常",
            "ui.schedule.issue.suggestion.systemError"),

    /** 核心持久化失败。 */
    PERSIST_FAILED("PERSIST_FAILED", "核心持久化失败", "ui.schedule.issue.suggestion.persistFailed"),

    /** 部分排程结果持久化失败。 */
    PERSIST_PARTIAL_FAILED("PERSIST_PARTIAL_FAILED", "部分排程结果持久化失败",
            "ui.schedule.issue.suggestion.persistFailed");

    private final String code;

    private final String desc;

    /**
     * 建议处理国际化键。
     */
    private final String suggestion;

    ScheduleIssueCategoryEnum(String code, String desc, String suggestion) {
        this.code = code;
        this.desc = desc;
        this.suggestion = suggestion;
    }

    /**
     * 获取问题类别编码。
     *
     * @return 问题类别编码
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 获取问题类别说明。
     *
     * @return 问题类别说明
     */
    public String getDesc() {
        return this.desc;
    }

    /**
     * 按问题类别编码查找枚举。
     *
     * @param code 问题类别编码
     * @return 匹配的枚举；未匹配时返回 null
     */
    public static ScheduleIssueCategoryEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ScheduleIssueCategoryEnum category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }

    /**
     * 获取建议处理国际化键。
     *
     * @return 建议处理国际化键
     */
    public String getSuggestion() {
        return this.suggestion;
    }
}
