package com.zlt.aps.itf.mes.enums;

import lombok.Getter;

/**
 * MES接口码枚举类
 *
 * @author Chen
 * @since 2025/12/16
 */
@Getter
public enum MesInterfaceCodeEnum {

    /**
     * 同步SAP与模具关系
     */
    SYNC_PRODUCT_MOD_RELATION("PRODUCT_MOD_RELATION", "syncProductModRelation", "PRODUCT_MOD_RELATION", "同步SAP与模具关系"),

    /**
     * 同步模具台账
     */
    SYNC_MODEL_INFO("TIRE_MOD_INFO", "syncModelInfo", "TIRE_MOD_INFO", "同步模具台账"),

    ;

    private final String code;
    private final String methodName;
    private final String tableName;
    private final String desc;

    MesInterfaceCodeEnum(String code, String methodName, String tableName, String desc) {
        this.code = code;
        this.methodName = methodName;
        this.tableName = tableName;
        this.desc = desc;
    }

    public static MesInterfaceCodeEnum getByCode(String code) {
        for (MesInterfaceCodeEnum value : MesInterfaceCodeEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
