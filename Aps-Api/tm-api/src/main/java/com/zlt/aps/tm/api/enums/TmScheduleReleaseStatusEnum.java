package com.zlt.aps.tm.api.enums;

/**
 * 胎面排程发布状态枚举。
 *
 * <p>编码与字典 `IS_RELEASE` 保持一致，用于代码内部判断和解释输出，不修改现有字典。</p>
 */
public enum TmScheduleReleaseStatusEnum {

    /** 未发布 */
    NOT_RELEASED("0", "未发布"),

    /** 已发布 */
    RELEASED("1", "已发布"),

    /** 发布失败 */
    RELEASE_FAILED("2", "发布失败"),

    /** 发布中 */
    RELEASING("3", "发布中"),

    /** 超时失败 */
    TIMEOUT_FAILED("4", "超时失败"),

    /** 待发布 */
    WAIT_RELEASE("5", "待发布");

    private final String code;

    private final String desc;

    TmScheduleReleaseStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
