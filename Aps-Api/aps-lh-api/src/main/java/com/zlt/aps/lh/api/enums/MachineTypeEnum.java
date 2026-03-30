package com.zlt.aps.lh.api.enums;

import lombok.Getter;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化机机械类型枚举
 * @date 2025/3/10
 */
@Getter
public enum MachineTypeEnum implements PublicEnum{


    //机械
    MACHINERY("机械", "MACHINERY"),
    //液压
    HYDRAULIC_PRESSURE("液压", "HYDRAULIC_PRESSURE"),
    ;
    private String code;

    private String name;

    MachineTypeEnum(String name, String code) {
        this.code = code;
        this.name = name;
    }
}
