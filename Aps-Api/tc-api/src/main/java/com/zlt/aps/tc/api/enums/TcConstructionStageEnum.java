package com.zlt.aps.tc.api.enums;

/**
 * 胎侧施工阶段枚举。
 */
public enum TcConstructionStageEnum {

    /** 实验规格施工阶段。 */
    EXPERIMENT("01", "实验规格");

    private final String code;

    private final String desc;

    TcConstructionStageEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取施工阶段编码。
     *
     * @return 施工阶段编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取施工阶段说明。
     *
     * @return 施工阶段说明
     */
    public String getDesc() {
        return desc;
    }
}
