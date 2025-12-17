package com.tlt.aps.enums;

import lombok.Getter;

/**
 * 成型固定优先级
 * 值越低，优先级越高
 *
 * @author ZLT
 * 20251216
 */
@Getter
public enum CxMachineFixedPriorityEnum {
    /**
     * 0 固定SKU
     */
    FIXED_SKU(0, "固定SKU"),
    /**
     * 1 固定结构1
     */
    FIXED_STRUCTURE_FIRST(1, "固定结构1"),
    /**
     * 2 固定结构2
     */
    FIXED_STRUCTURE_SECOND(2, "固定结构2"),
    /**
     * 3 固定结构3
     */
    FIXED_STRUCTURE_THIRD(3, "固定结构3"),
    /**
     * 2ⁿ-1,n=31 默认值
     */
    DEFAULT(Integer.MAX_VALUE,"默认值");

    private Integer priorityValue;

    private String desc;

    CxMachineFixedPriorityEnum(Integer priorityValue, String desc) {
        this.priorityValue = priorityValue;
        this.desc = desc;
    }
}
