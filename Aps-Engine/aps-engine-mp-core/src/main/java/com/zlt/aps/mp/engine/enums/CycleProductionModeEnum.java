package com.zlt.aps.mp.engine.enums;

import java.util.Arrays;

/**
 * 周期结构排产模式枚举定义类
 * 1 只排高优先级需求量-即只排高
 * 2 只排实单需求量-即只排高+中
 * 3 排净需求量-即(高+中+周期储备)
 *
 * @author ZLT
 * @date 20260709
 */
public enum CycleProductionModeEnum {
    /**
     * 1 只排高优先级需求量-即只排高
     */
    ONLY_HIGH(1, "只排高优先级需求量-即只排高"),
    /**
     * 2 只排实单需求量-即只排高+中
     */
    ONLY_ACTUAL(2, "只排实单需求量-即只排高+中"),
    /**
     * 3 排净需求量-即(高+中+周期储备)
     */
    ALL(3, "排净需求量-即(高+中+周期储备)");

    private Integer value;

    private String desc;

    /**
     * 获取对应排产类型，默认ALL
     * 为空或是不匹配，则为ALL
     *
     * @param typeValue
     * @return
     */
    public static CycleProductionModeEnum getInstance(Integer typeValue) {
        if (null == typeValue) {
            return ALL;
        }
        return Arrays.stream(values()).filter(mode -> mode.getValue().equals(typeValue)).findFirst().orElse(ALL);
    }

    CycleProductionModeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
