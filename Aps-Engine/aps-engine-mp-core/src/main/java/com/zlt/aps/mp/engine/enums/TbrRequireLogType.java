package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 需求日志类型枚举定义类
 *
 * @author ZLT
 * 20260325
 */
@Getter
public enum TbrRequireLogType {

    /**
     * 1001 需求测算阶段
     */
    REQUIRE_ESTIMATE("1001", "需求测算阶段");

    private String typeValue;

    private String desc;

    TbrRequireLogType(String typeValue, String desc) {
        this.typeValue = typeValue;
        this.desc = desc;
    }
}
