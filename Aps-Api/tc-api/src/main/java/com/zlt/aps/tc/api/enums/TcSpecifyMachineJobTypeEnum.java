package com.zlt.aps.tc.api.enums;

/**
 * 胎侧指定机台任务类型枚举，对应 {@code JOB_TYPE} 字典。
 */
public enum TcSpecifyMachineJobTypeEnum {

    /** 允许生产。 */
    ALLOW("0", "允许生产"),

    /** 禁止生产。 */
    FORBID("1", "禁止生产");

    private final String code;

    private final String desc;

    TcSpecifyMachineJobTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取任务类型编码。
     *
     * @return 任务类型编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取任务类型说明。
     *
     * @return 任务类型说明
     */
    public String getDesc() {
        return desc;
    }
}
