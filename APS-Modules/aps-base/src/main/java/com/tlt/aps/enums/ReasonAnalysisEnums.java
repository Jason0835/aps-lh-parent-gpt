package com.tlt.aps.enums;

/**
 * 原因分析枚举
 *
 * @author zlt
 * @since 2024/1/17
 */
public enum ReasonAnalysisEnums {

    /**
     * 换模
     */
    CHANGE_MOULD(1,"换模"),

    /**
     * 开班
     */
    OPEN_CLASS(2,"开班"),

    /**
     * 开汽
     */
    OPEN_STREAM(3,"开汽");

    private final Integer code;
    private final String desc;

    ReasonAnalysisEnums(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ReasonAnalysisEnums getByCode(Integer code){
        for (ReasonAnalysisEnums enums:values()){
            if (enums.getCode().equals(code)){
                return enums;
            }
        }
        return null;
    }
}
