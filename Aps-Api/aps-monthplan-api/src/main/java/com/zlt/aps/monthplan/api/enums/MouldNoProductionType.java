package com.zlt.aps.monthplan.api.enums;

/**
 * 模具不可排产日类型
 *
 * @author ZLT
 * @date 20250308
 */
public enum MouldNoProductionType {
    /**
     * 1 停工日
     */
    STOP_DAY(1, "停工日"),
    /**
     * 2 维修日
     */
    MAINTENANCE_DAY(2, "维修日"),
    /**
     * 3 洗模日
     */
    MOULD_CLEANING_DAY(3, "洗模日");

    private Integer type;

    private String desc;

    MouldNoProductionType(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
