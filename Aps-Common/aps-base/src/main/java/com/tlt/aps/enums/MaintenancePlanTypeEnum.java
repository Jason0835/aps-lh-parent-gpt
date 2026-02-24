package com.tlt.aps.enums;

import lombok.Getter;

/**
 * 设备维护保养类型
 */
@Getter
public enum MaintenancePlanTypeEnum {
    REPAIR(0, "维修"),
    MAINTENANCE(1, "保养");

    private Integer code;
    private String desc;

    MaintenancePlanTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
