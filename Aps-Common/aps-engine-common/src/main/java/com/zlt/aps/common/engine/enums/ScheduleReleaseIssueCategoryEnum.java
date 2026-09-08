package com.zlt.aps.common.engine.enums;

/**
 * TM/TC 排程结果下发问题类别枚举。
 *
 * <p>下发问题与自动排程问题属于不同业务阶段，因此单独维护类别和建议文案，
 * 避免将 MES 下发状态混入自动排程问题枚举。</p>
 */
public enum ScheduleReleaseIssueCategoryEnum {

    /**
     * 下发前校验失败。
     */
    VALIDATION_FAILED("VALIDATION_FAILED", "下发前校验失败",
            "ui.schedule.issue.suggestion.releaseValidationFailed"),

    /**
     * MES 反馈超时。
     */
    MES_TIMEOUT("MES_TIMEOUT", "MES反馈超时", "ui.schedule.issue.suggestion.releaseMesTimeout"),

    /**
     * MES 拒绝接收。
     */
    MES_REJECTED("MES_REJECTED", "MES拒绝接收", "ui.schedule.issue.suggestion.releaseMesRejected");

    private final String code;

    private final String desc;

    /**
     * 建议处理国际化键。
     */
    private final String suggestion;

    ScheduleReleaseIssueCategoryEnum(String code, String desc, String suggestion) {
        this.code = code;
        this.desc = desc;
        this.suggestion = suggestion;
    }

    /**
     * 按问题类别编码查找枚举。
     *
     * @param code 问题类别编码
     * @return 匹配的枚举；未匹配时返回 null
     */
    public static ScheduleReleaseIssueCategoryEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ScheduleReleaseIssueCategoryEnum category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
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
     * 获取建议处理国际化键。
     *
     * @return 建议处理国际化键
     */
    public String getSuggestion() {
        return this.suggestion;
    }
}
