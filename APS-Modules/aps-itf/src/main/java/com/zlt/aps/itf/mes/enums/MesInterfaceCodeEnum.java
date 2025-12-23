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
     * 同步SKU与模具关系
     */
    SYNC_PRODUCT_MOD_RELATION("PRODUCT_MOD_RELATION", "mesItfService", "syncProductModRelation", "同步SKU与模具关系"),

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

    /**
     * 同步特殊材料库存-MES提供视图同步，不通过MQ
     */
    SYNC_SPEC_STOCK("SPEC_STOCK_SYNC", "mesItfService", "syncRawSpecialMaterialStock", "同步特殊材料库存"),

    /**
     * 同步原材料出库-MES提供视图同步，不通过MQ
     */
    SYNC_ORI_MATERIAL_OUT("ORI_MATERIAL_OUT_SYNC", "mesItfService", "syncRawSpecialMaterialStock", "同步特殊材料库存"),

    /**
     * SKU与施工关系表
     */
    LH_CONSTRUCTION_INFO("LH_CONSTRUCTION_INFO", "mesBomItfService", "syncLhConstructionInfo", "SKU与施工关系表"),

    /**
     * 半部件BOM接口
     */
    CONSTRUCTION_INFO("CONSTRUCTION_INFO", "mesBomItfService", "syncConstructionInfo", "半部件BOM接口"),

    /**
     * 成型及半部件BOM施工信息同步
     */
    BOM_INFO("BOM_INFO", "mesBomItfService", "syncBomInfo", "成型及半部件BOM施工信息同步"),

    /**
     * 成型及半部件BOM施工信息同步
     */
    BAS_MATERIAL("BAS_MATERIAL", "mesBomItfService", "syncMaterial", "成品物料信息同步"),

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
