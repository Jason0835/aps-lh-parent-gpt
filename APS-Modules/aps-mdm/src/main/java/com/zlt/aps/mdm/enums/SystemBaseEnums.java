package com.zlt.aps.mdm.enums;

import lombok.Getter;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化系统基础参数枚举
 * @date 2025/3/13
 */
@Getter
public enum SystemBaseEnums {

    //SQL分割查询长度
    SPLIT_LENGTH("SPLIT_LENGTH",900)
    ;

    SystemBaseEnums(String name, Integer code) {
        this.name = name;
        this.code = code;
    }

    private Integer code;

    private String name;
}
