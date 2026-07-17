package com.zlt.aps.tc.api.enums;

/**
 * 胎侧机台分配结果状态枚举。
 */
public enum TcMachineAssignStatusEnum {

    /** 已分配机台。 */
    ASSIGNED("ASSIGNED", "已分配"),

    /** 未排产。 */
    UNPLANNED("UNPLANNED", "未排产");

    private final String code;

    private final String desc;

    TcMachineAssignStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取分配状态编码。
     *
     * @return 分配状态编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取分配状态说明。
     *
     * @return 分配状态说明
     */
    public String getDesc() {
        return desc;
    }
}
