package com.zlt.aps.mp.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交替类型
 * @author Sandy
 */
@AllArgsConstructor
@Getter
public enum AlternativeTypeEnum {

    PRO_SIZE_ALTERNATIVE("0", "英寸交替"),
    STRUCT_ALTERNATIVE("1","结构交替")
    ;

    private String code;
    private String name;

}
