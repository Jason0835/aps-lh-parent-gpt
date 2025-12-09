package com.zlt.aps.maindata.enums;

import lombok.Getter;

/**
 * @author Chen
 * @since 2025/12/9
 */
@Getter
public enum MonthPlanEnums {

    /**
     * 新模具预计到货天数
     */
    MODULE_ARRIVAL_DAYS("SYS0209001", "单位天，新模具预计到货天数"),

    ;

    private final String code;
    private final String name;

    MonthPlanEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
