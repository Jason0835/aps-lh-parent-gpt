package com.zlt.aps.itf.mes.enums;

import lombok.Getter;

/**
 * 物料类型转换枚举
 *
 * @author Chen
 * @since 2026/1/20
 */
@Getter
public enum MouldCategoryConvertEnum {

    /**
     * 33-无内胎外胎
     */
    TYPE_CODE_33("33", "无内胎外胎", "01"),

    /**
     * 32-有内胎外胎
     */
    TYPE_CODE_32("32", "有内胎外胎", "01"),

    /**
     * 01-外胎
     */
    TYPE_CODE_02("02", "骨架材料", ""),

    /**
     * 21-全钢半成品
     */
    TYPE_CODE_21("21", "全钢半成品", "03"),

    /**
     * 40-天轮轮胎
     */
    TYPE_CODE_40("40", "天轮轮胎", ""),

    /**
     * 35-外胎
     */
    TYPE_CODE_36("36", "内胎", ""),

    /**
     * 34-内胎
     */
    TYPE_CODE_37("37", "垫带", ""),

    /**
     * 22-半钢半成品
     */
    TYPE_CODE_22("22", "半钢半成品", "03"),
    ;

    private final String mesCode;
    private final String mesName;
    private final String code;

    MouldCategoryConvertEnum(String mesCode, String mesName, String code) {
        this.mesCode = mesCode;
        this.mesName = mesName;
        this.code = code;
    }

    public static MouldCategoryConvertEnum getByMesCode(String mesCode) {
        for (MouldCategoryConvertEnum value : MouldCategoryConvertEnum.values()) {
            if (value.getMesCode().equals(mesCode)) {
                return value;
            }
        }
        return null;
    }
}
