package com.zlt.aps.monthplan.api.enums;

/**
 * 月计划调整方式枚举类
 *
 * @author ZLT
 * @date 20250529
 */
public enum MonthPlanAdjustTypeEnum {
    /**
     * 调减，计划减量
     */
    SUBTRACT(0, "调减"),
    /**
     * 调增，计划增量
     */
    ADD(1, "调增");

    private Integer adjustType;

    private String desc;

    MonthPlanAdjustTypeEnum(Integer adjustType, String desc) {
        this.adjustType = adjustType;
        this.desc = desc;
    }

    public Integer getAdjustType() {
        return adjustType;
    }

    public String getDesc() {
        return desc;
    }
}
