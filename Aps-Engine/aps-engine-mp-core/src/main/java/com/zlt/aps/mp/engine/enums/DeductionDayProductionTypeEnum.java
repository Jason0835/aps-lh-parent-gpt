package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 日排产扣除类型
 * 1、分组强制收尾
 * 2、分组延长重排产
 *
 * @author ZLT
 * @date 20260329
 */
@Getter
public enum DeductionDayProductionTypeEnum {
    /**
     * 01 强制收尾
     */
    FORCED_CLOSURE("01", "强制收尾"),
    /**
     * 02 延长重排探测
     */
    TIME_EXTENSION_REST("02", "延长重排探测");

    private String type;

    private String desc;

    DeductionDayProductionTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
