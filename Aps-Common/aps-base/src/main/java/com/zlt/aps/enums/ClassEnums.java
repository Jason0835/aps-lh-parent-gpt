package com.zlt.aps.enums;

/**
 * 班次枚举
 *
 * @author zlt
 * @since 2024/1/17
 */
public enum ClassEnums {

    /**
     * 一班
     */
    ONE_CLS(1,"一班"),

    /**
     * 二班
     */
    TWO_CLS(2,"二班"),

    /**
     * 三班
     */
    THREE_CLS(3,"三班"),

    /**
     * 四班
     */
    FOUR_CLS(4,"四班"),

    /**
     * 五班
     */
    FRI_CLS(5,"五班"),

    /**
     * 六班
     */
    SUN_CLS(6,"六班");

    private final Integer code;
    private final String desc;

    ClassEnums(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ClassEnums getByCode(Integer code){
        for (ClassEnums enums:values()){
            if (enums.getCode().equals(code)){
                return enums;
            }
        }
        return null;
    }
}
