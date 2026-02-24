package com.zlt.aps.enums;

import lombok.Getter;

/**
 * @author Chen
 * @date 2025/3/25
 */
@Getter
public enum TireTypeEnum {

    /**
     * 半钢子午PC轮胎
     */
    PC("0", "半钢子午PC轮胎"),

    /**
     * 半钢子午SUV轮胎
     */
    SUV("1", "半钢子午SUV轮胎"),

    /**
     * 半钢子午WIN轮胎
     */
    WIN("2", "半钢子午WIN轮胎"),

    /**
     * 半钢子午WIN轮胎
     */
    LT("3", "半钢子午LT轮胎"),

    /**
     * 半钢子午LT+WIN轮胎
     */
    LT_WIN("4", "半钢子午LT+WIN轮胎"),

    ;

    /**
     * 编码
     */
    private final String value;

    /**
     * 描述
     */
    private final String name;

    TireTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 根据类别值，获取库位类别枚举实例对象
     *
     * @param value 值
     * @return 返回枚举实例对象
     */
    public static TireTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (TireTypeEnum productTypeEnum : TireTypeEnum.values()) {
            if (productTypeEnum.getValue().equals(value)) {
                return productTypeEnum;
            }
        }

        return null;
    }
}
