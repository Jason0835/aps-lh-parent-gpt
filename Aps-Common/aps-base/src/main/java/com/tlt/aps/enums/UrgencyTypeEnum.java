package com.tlt.aps.enums;

import lombok.Getter;

/**
 * @author Sandy
 * @date 2026/1/19
 */
@Getter
public enum UrgencyTypeEnum {

    /**
     * 紧急
     */
    URGENCY("01", "紧急"),

    /**
     * 普通
     */
    ORDINARY("04", "普通")
    ;

    /**
     * 编码
     */
    private final String value;

    /**
     * 描述
     */
    private final String name;

    UrgencyTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 根据类别值，获取库位类别枚举实例对象
     *
     * @param value 值
     * @return 返回枚举实例对象
     */
    public static UrgencyTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (UrgencyTypeEnum urgencyTypeEnum : UrgencyTypeEnum.values()) {
            if (urgencyTypeEnum.getValue().equals(value)) {
                return urgencyTypeEnum;
            }
        }

        return null;
    }
}
