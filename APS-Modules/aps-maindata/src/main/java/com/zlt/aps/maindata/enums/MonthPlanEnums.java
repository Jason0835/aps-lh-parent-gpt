package com.zlt.aps.maindata.enums;

import lombok.Getter;

/**
 * 月计划参数枚举
 *
 * @author Chen
 * @since 2025/12/9
 */
@Getter
public enum MonthPlanEnums {

    /**
     * SYS0209001 新模具预计到货天数
     */
    MODULE_ARRIVAL_DAYS("SYS0209001", "单位天，新模具预计到货天数"),
    /**
     * SYS0201001 月份周期排产起始日
     */
    PRODUCTION_CYCLE_START("SYS0201001", "排产月份周期开始日"),
    /**
     * SYS0202001 初始化时，是否进行模具预占产能计算
     */
    OPEN_PREEMPTION_MOULD("SYS0202001","初始化时，是否进行模具预占产能计算"),
    /**
     * SYS0202002 日硫化量使用的模式值:M = 使用MES的硫化量 S = 使用标准硫化量 A = 使用APS计算的硫化量；其他则认为采用标准硫化量
     */
    DAY_VULCANIZATION_MODE("SYS0202002", "日硫化量使用的模式值"),
    /**
     * SYS0202003 是否采用损耗率计算损耗
     */
    OPEN_LEVEL_RATIO("SYS0202003", "是否采用损耗率计算损耗");

    private final String code;
    private final String name;

    MonthPlanEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
