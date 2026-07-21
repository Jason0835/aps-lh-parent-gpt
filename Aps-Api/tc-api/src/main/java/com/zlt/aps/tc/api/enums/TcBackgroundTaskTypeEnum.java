package com.zlt.aps.tc.api.enums;

import lombok.Getter;

/**
 * 胎侧后台任务类型。
 */
@Getter
public enum TcBackgroundTaskTypeEnum {

    /** 自动排程。 */
    AUTO_PLAN("AUTO_PLAN"),

    /** 排程发布。 */
    RELEASE("RELEASE"),

    /** 自动滚动更新。 */
    AUTO_ROLLING("AUTO_ROLLING");

    /** 类型编码。 */
    private final String code;

    TcBackgroundTaskTypeEnum(String code) {
        this.code = code;
    }
}
