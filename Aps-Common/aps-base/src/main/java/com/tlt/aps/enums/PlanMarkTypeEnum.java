package com.tlt.aps.enums;

/**
 * SKU计划标记信息
 *
 * @author ZLT
 * 20250925
 */
public enum PlanMarkTypeEnum {
    /**
     * 01 续作
     */
    CONTINUE_PLAN("01", "续作"),
    /**
     * 02 交期
     */
    DELIVERY_DATE_PLAN("02", "交期"),
    /**
     * 03 续作
     */
    IMPORTANT_CUSTOM_PLAN("03", "重要客户"),
    /**
     * 04 必保
     */
    ENSURE_PLAN("04", "必保"),
    /**
     * 05 急单
     */
    EMERGENCY_PLAN("05", "急单"),
    /**
     * 06 欠产
     */
    DEBIT_PLAN("06", "欠产"),
    /**
     * 07 备货
     */
    STOCK_UP_PLAN("07", "备货");

    private String code;

    private String name;

    PlanMarkTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
