package com.zlt.aps.enums;

import lombok.Getter;

/**
 * 排产计划类型
 * 01 正常 02 订单预测 03 实单模拟
 *
 * @author zlt
 * @since 20251211
 */
@Getter
public enum ProductionPlanType {
    /**
     * 01 正常
     */
    NORMAL("01", "正常"),
    /**
     * 02 订单预测
     */
    PREDICTION("02", "订单预测"),
    /**
     * 03 实单模拟
     */
    SIMULATE("03", "实单模拟"),
    /**
     * 04、周程调整
     */
    ADJUST("04","周程调整");

    private String planType;

    private String desc;

    ProductionPlanType(String planType, String desc) {
        this.planType = planType;
        this.desc = desc;
    }
}
