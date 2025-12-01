package com.tlt.aps.enums;

/**
 * 参数数据类型枚举
 *
 * @author ZLT
 * @date 20250220
 */
public enum SysParamDataTypeEnum {

    /**
     * 0-字符型
     */
    STRING(0),
    /**
     * 1-整型
     */
    INTEGER(1),
    /**
     * 2-数值型
     */
    NUMBER(2),

    /**
     * 3-日期型
     */
    DATE(3),

    /**
     * 4-时间型
     */
    TIME(4),

    /**
     * 5-日期时间型
     */
    DATETIME(5),

    /**
     * 6-布尔型
     */
    BOOLEAN(6),

    /**
     * 7-CUSTOM类型，用于自定义格式如x:y
     */
    CUSTOM(7);

    private Integer value;

    SysParamDataTypeEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static SysParamDataTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }

        for (SysParamDataTypeEnum sysParamDataTypeEnum : SysParamDataTypeEnum.values()) {
            if (sysParamDataTypeEnum.getValue() == value) {
                return sysParamDataTypeEnum;
            }

        }

        return null;
    }
}
