package com.zlt.aps.mp.api.constants;

/**
 * 月计划常量类
 */
public class MonthPlanConstants {

    /**
     * 销售优先级：1-高优先级；
     */
    public static final String SAL_PRIORITY_HIGHT = "1";
    /**
     * 供应链订单类型：2-高优先级；
     */
    public static final String SAL_PRIORITY_CYCLE_STOCK_UP = "2";

    /**
     * 销售优先级：3-中优先级；
     */
    public static final String SAL_PRIORITY_MID = "3";
    /**
     * 供应链订单类型：4-常规储备；
     */
    public static final String SAL_PRIORITY_PRECEDENT_STOCK_UP = "4";
    /**
     *  销售优先级：5-暂缓订单
     */
    public static final String SAL_PRIORITY_POSTPONE = "5";
}
