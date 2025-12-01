package com.zlt.mix.common.core.enums;

/**
 * 生产日标记
 */
public enum ProductDayFlagEnum {
    DAY1("1"), DAY2("2");

    private final String code;

    ProductDayFlagEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
