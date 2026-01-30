package com.zlt.aps.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据来源
 */
@Getter
@AllArgsConstructor
public enum DataSourceEnum {

    /**
     * 手工新增
     */
    HAND("01","手工新增"),
    /**
     * 自动生成
     */
    AUTO("02","自动生成"),
    /**
     * 导入
     */
    IMPORT("03","导入"),
    /**
     * 接口同步
     */
    SYNC("04","接口同步"),
    ;

    private String code;
    private String name;

}
