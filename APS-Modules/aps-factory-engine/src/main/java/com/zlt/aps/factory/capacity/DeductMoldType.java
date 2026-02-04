package com.zlt.aps.factory.capacity;

/**
 * 减模分类枚举
 */
public enum DeductMoldType {

    /**
     * 可以匹配增模和换活字块
     */
    NORMAL,

    /**
     * 只能匹配增模 (属于W_only)
     */
    ONLY_INCREASE,

    /**
     * 只能匹配换活字块 (属于Z_only)
     */
    ONLY_CHANGE,

    /**
     * 不能匹配任何资源 (属于I类)
     */
    ISOLATED
}
