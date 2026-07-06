package com.zlt.aps.mdm.api.enums;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

/**
 * 工艺类型枚举
 *
 * @author ZLT
 * 20260702
 */
@Getter
public enum ProcessCodeEnum {

    /** 长度 */
    LENGTH("1", "长度"),
    /** 宽度 */
    WIDTH("2", "宽度"),
    /** 幅宽 */
    FABRIC_WIDTH("3", "幅宽"),
    ;

    private String code;
    private String name;

    ProcessCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码获取对应的工艺类型枚举实例
     *
     * @param code 工艺类型编码 1=长度, 2=宽度, 3=幅宽
     * @return 工艺类型枚举，未匹配返回 null
     */
    public static ProcessCodeEnum getInstance(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (ProcessCodeEnum type : ProcessCodeEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
