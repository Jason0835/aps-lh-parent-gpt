package com.zlt.aps.mdm.enums;

import lombok.Getter;

/**
 * 发布状态枚举，字典：IS_RELEASE
 *
 * @author Chen
 * @since 2026/1/4
 */
@Getter
public enum ReleaseStatusEnum {

    /**
     * 0--未发布
     */
    UN_RELEASE("0", "未发布"),

    /**
     * 1--已发布
     */
    RELEASE("1", "已发布"),

    /**
     * 2-发布失败
     */
    RELEASE_FAIL("2", "发布失败"),

    /**
     * 3-发布中
     */
    RELEASING("3", "发布中"),

    /**
     * 4-超时失败
     */
    TIME_OUT_FAIL("4", "超时失败"),

    /**
     * 5-待发布
     */
    WAIT_RELEASE("5", "待发布"),
    ;

    private final String code;
    private final String name;

    ReleaseStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
