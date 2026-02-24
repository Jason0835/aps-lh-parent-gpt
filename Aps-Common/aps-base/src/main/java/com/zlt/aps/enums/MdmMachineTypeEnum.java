package com.zlt.aps.enums;

import lombok.Getter;

/**
 * 维护设备类型-mdm_machine_type
 *
 * @author Liam
 * @since 2025/2/27
 */
@Getter
public enum MdmMachineTypeEnum {
    MOLDING("成型", 0),
    VULCANIZING("硫化", 1),
    MODEL("模具", 2),
    CLEAN_MOLD("洗模", 3),
    COMBINE_LH("硫化+洗模",99);

    private final String name;
    private final Integer value;

    MdmMachineTypeEnum(String name, Integer value) {
        this.name = name;
        this.value = value;
    }
}
