package com.zlt.aps.enums;

import lombok.Getter;

/**
 * 排产分组类型
 * 01 周期结构 02 常规结构
 *
 * @author zlt
 * @since 20260107
 */
@Getter
public enum ProductionGroupTypeEnum {
    /**
     * 01 周期结构
     */
    CYCLE("01", "周期结构"),
    /**
     * 02 常规结构
     */
    CONVENTION("02", "常规结构");

    private String groupType;

    private String desc;

    ProductionGroupTypeEnum(String groupType, String desc) {
        this.groupType = groupType;
        this.desc = desc;
    }
}
