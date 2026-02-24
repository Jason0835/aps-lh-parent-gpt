package com.zlt.aps.factory.daylimit;

import lombok.Getter;

/**
 * 结构分配产能限制类型枚举定义类
 * 00 无限制
 * 01 成型工装(成型鼓)数量限制
 * 02 日产能上限限制
 * 03 日产能上限、成型工装(成型鼓)数量多重限制
 *
 * @author ZLT
 * 20260125
 */
@Getter
public enum GroupAllocationCapacityLimitTypeEnum {
    /**
     * 00 无限制
     */
    NO_LIMIT("00", "无限制"),
    /**
     * -00 没进入限制业务条件
     */
    NO_ENTER_LIMIT("-00", "没进入限制业务条件"),
    /**
     * 01 成型工装(成型鼓)数量限制
     */
    TIRE_DRUM_LIMIT("01", "成型工装(成型鼓)数量限制"),
    /**
     * 02 日产能上限限制
     */
    DAY_MAX_CAPACITY_LIMIT("02", "日产能上限限制"),
    /**
     * 03 日产能上限、成型工装(成型鼓)数量多重限制
     */
    DAY_MAX_CAPACITY_TIRE_DRUM_LIMIT("03", "日产能上限、成型工装(成型鼓)数量多重限制");
    /**
     * 限制类型
     */
    private String limitType;
    /**
     * 限制描述
     */
    private String limitDesc;

    GroupAllocationCapacityLimitTypeEnum(String limitType, String limitDesc) {
        this.limitType = limitType;
        this.limitDesc = limitDesc;
    }
}
