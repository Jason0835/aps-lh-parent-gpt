package com.zlt.aps.tm.api.enums;

/**
 * 胎面需求量算法枚举。
 */
public enum TmDemandAlgorithmEnum {

    /** 保护班次需求算法。 */
    GUARD("1", "保护班次需求算法"),

    /** 下一班需求算法。 */
    NEXT_SHIFT("2", "下一班需求算法");

    private final String code;

    private final String desc;

    TmDemandAlgorithmEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取算法编码。
     *
     * @return 算法编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取算法说明。
     *
     * @return 算法说明
     */
    public String getDesc() {
        return desc;
    }
}
