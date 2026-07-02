package com.zlt.aps.mdm.api.enums;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

/**
 * BOM类型定义类
 *
 * @author ZLT
 * 20260702
 */
@Getter
public enum BomTypeEnum {
    /**
     * 胎胚
     */
    EMBRYO("01", "胎胚")
    ;
    private String code;
    private String mesCode;

    BomTypeEnum(String code, String mesCode) {
        this.code = code;
        this.mesCode = mesCode;
    }

    /**
     * 根据业务编码，获取对应的业务排序枚举实例对象
     *
     * @param code
     * @return
     */
    public static BomTypeEnum getInstance(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (BomTypeEnum businessType : BomTypeEnum.values()) {
            if (businessType.getCode().equals(code)) {
                return businessType;
            }
        }
        return null;
    }
}