package com.tlt.aps.enums;

/**
 * 调整类型枚举
 *
 * @author zlt
 * @since 2024/1/17
 */
public enum AdjustTypeEnums {

    /**
     * 部分调整，机台检修
     */
    PART_ADJUST("0","部分调整，机台检修"),

    /**
     * 全部调整，机台坏了
     */
    ALL_ADJUST("1","全部调整，机台坏了"),
;

    private final String code;
    private final String desc;

    AdjustTypeEnums(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AdjustTypeEnums getByCode(String code){
        for (AdjustTypeEnums enums:values()){
            if (enums.getCode().equals(code)){
                return enums;
            }
        }
        return null;
    }
}
