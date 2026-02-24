package com.zlt.aps.factory.enums;

/**
 * 质量等级枚举定义类
 *
 * @author ZLT
 * @date 20251204
 */
public enum LeveCodeEnum {
    /**
     * 合格品
     */
    A("98", "A"),
    /**
     * 不良品
     */
    A0("99", "A0");

    private String value;
    private String name;

    LeveCodeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static LeveCodeEnum getEnumByValue(String value){
        if (value == null){
            return null;
        }
        for (LeveCodeEnum leveCodeEnum : LeveCodeEnum.values()) {
            if (leveCodeEnum.getValue().equals(value)){
                return leveCodeEnum;
            }
        }
        return null;
    }
}
