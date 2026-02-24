package com.zlt.aps.mdm.enums;

import lombok.Getter;

import java.util.Arrays;


/**
 * 事件模块类型枚举
 *
 * @author chen
 */
@Getter
public enum EventModuleTypeEnum {

    /**
     * 月计划
     */
    MONTH_PLAN("01", "月计划"),

    ;

    /**
     * 编号
     */
    private final String code;

    /**
     * 名称
     */
    private final String value;

    EventModuleTypeEnum(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public static EventModuleTypeEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

}
