package com.zlt.aps.tc.api.enums;

/**
 * 胎侧自动排程参数值来源枚举。
 */
public enum TcParamValueSourceEnum {

    /** 参数快照来源于胎侧参数表。 */
    TABLE("T_TC_PARAMS", "参数表"),

    /** 规则取值来源于有效参数值。 */
    PARAM("PARAM", "参数值"),

    /** 来源于代码默认值。 */
    DEFAULT("DEFAULT", "默认值");

    private final String code;

    private final String desc;

    TcParamValueSourceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取来源编码。
     *
     * @return 来源编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取来源说明。
     *
     * @return 来源说明
     */
    public String getDesc() {
        return desc;
    }
}
