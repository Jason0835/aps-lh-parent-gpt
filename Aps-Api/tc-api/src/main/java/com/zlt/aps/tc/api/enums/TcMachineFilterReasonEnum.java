package com.zlt.aps.tc.api.enums;

/**
 * 胎侧机台过滤原因枚举。
 */
public enum TcMachineFilterReasonEnum {

    /** 机台未启用。 */
    MACHINE_DISABLED("MACHINE_DISABLED", "机台未启用"),

    /** 机台当前班次未开机。 */
    MACHINE_SHIFT_NOT_OPEN("MACHINE_SHIFT_NOT_OPEN", "机台当前班次未开机"),

    /** 工作日历配置当前结果班次停产。 */
    WORK_CALENDAR_SHIFT_STOPPED("WORK_CALENDAR_SHIFT_STOPPED", "工作日历当前班次停产"),

    /** 无剩余产能。 */
    NO_REMAIN_CAPACITY("NO_REMAIN_CAPACITY", "机台剩余产能不足"),

    /** 口型板不匹配。 */
    MOUTH_PLATE_NOT_MATCH("MOUTH_PLATE_NOT_MATCH", "口型板不匹配"),

    /** 胶料机台关系不匹配。 */
    GLUE_MACHINE_NOT_MATCH("GLUE_MACHINE_NOT_MATCH", "胶料机台关系不匹配"),

    /** 胎侧与垫胶共用机台当前班次不允许生产胎侧。 */
    SHARED_MACHINE_SHIFT_NOT_MATCH("SHARED_MACHINE_SHIFT_NOT_MATCH", "胎侧与垫胶共用机台班次不匹配"),

    /** 未命中选择定点机台。 */
    FIXED_MACHINE_NOT_SELECTED("FIXED_MACHINE_NOT_SELECTED", "未命中选择定点生产机台"),

    /** 命中禁止生产定点机台。 */
    FIXED_MACHINE_EXCLUDED("FIXED_MACHINE_EXCLUDED", "命中定点不可生产机台"),

    /** 默认规则通过。 */
    DEFAULT_PASS("DEFAULT_PASS", "通过默认过滤规则"),

    /** 无法识别的过滤原因。 */
    UNKNOWN("UNKNOWN", "未知原因");

    private final String code;

    private final String desc;

    TcMachineFilterReasonEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取过滤原因编码。
     *
     * @return 过滤原因编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取过滤原因说明。
     *
     * @return 过滤原因说明
     */
    public String getDesc() {
        return desc;
    }
}
