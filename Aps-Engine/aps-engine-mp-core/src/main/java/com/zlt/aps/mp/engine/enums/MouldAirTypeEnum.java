package com.zlt.aps.mp.engine.enums;

/**
 * 模具汽套类型
 *
 * @author ZLT
 * @date 20250220
 */
public enum MouldAirTypeEnum {
    /**
     * 弹簧汽套模具
     */
    AIR("1"),

    /**
     * 非弹簧汽套模具
     */
    NO_AIR("2"),

    /**
     * 普通模具
     */
    NORMAL("3");

    private String value;

    public String getValue() {
        return value;
    }

    MouldAirTypeEnum(String value) {
        this.value = value;
    }

    public static MouldAirTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }

        for (MouldAirTypeEnum airTypeEnum : MouldAirTypeEnum.values()) {
            if (airTypeEnum.getValue().equals(value)) {
                return airTypeEnum;
            }
        }

        return null;
    }
}
