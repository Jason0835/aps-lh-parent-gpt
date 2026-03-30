package com.zlt.aps.lh.api.enums;

import lombok.Getter;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/3/17
 */
@Getter
public enum ChangeTypeEnum implements PublicEnum{

    //拆模换
    DEMOULDING_AND_REPLACEMENT("拆模换","1")
    ;
    private String code;

    private String name;

    ChangeTypeEnum(String name, String code) {
        this.code = code;
        this.name = name;
    }
}
