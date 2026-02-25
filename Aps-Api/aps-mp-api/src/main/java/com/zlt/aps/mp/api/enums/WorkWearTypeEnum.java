package com.zlt.aps.mp.api.enums;

import lombok.Getter;

/**
 * 成型工装：成型鼓类型
 *
 * @author ZLT
 * @date 20260120
 */
@Getter
public enum WorkWearTypeEnum {
    /**
     * 01 成型鼓
     */
    BUILDING_DRUM("01", "成型鼓"),
    /**
     * 02 胎体鼓
     */
    TIRE_DRUM("02", "胎体鼓"),
    /**
     * 03 带束层鼓
     */
    BELT_DRUM("03", "带束层鼓");

    private String type;

    private String desc;

    WorkWearTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
