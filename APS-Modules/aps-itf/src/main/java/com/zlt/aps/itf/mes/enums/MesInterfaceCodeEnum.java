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
    SYNC_PRODUCT_MOD_RELATION("PRODUCT_MOD_RELATION", "mesItfService", "syncProductModRelation", "同步SAP与模具关系"),

    /**
     * 同步模具台账
     */
    SYNC_MODEL_INFO("TIRE_MOD_INFO", "mesItfService", "syncModelInfo", "同步模具台账"),

    /**
     * 同步成品库存-MES提供视图同步，不通过MQ
     */
    SYNC_PRODUCT_STOCK("MES_PRODUCT_STOCK", "mesItfService", "syncProductStock", "同步成品库存"),

    /**
     * 同步不合格库存-MES提供视图同步，不通过MQ
     */
    SYNC_UNQUALIFIED_STOCK("MES_UNQUALIFIED_STOCK", "mesItfService", "syncUnqualifiedStock", "同步不合格库存"),

    ;

    private final String code;
    private final String serviceName;
    private final String methodName;
    private final String desc;

    MesInterfaceCodeEnum(String code, String serviceName, String methodName, String desc) {
        this.code = code;
        this.serviceName = serviceName;
        this.methodName = methodName;
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
