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
     * 同步原材料出库
     */
    SYNC_ORI_MATERIAL_OUT("ORI_MATERIAL_OUT_SYNC", "mesItfService", "syncRawMaterialOutboundRecord", "原材料出库"),

    /**
     * SKU与施工关系表
     */
    LH_CONSTRUCTION_INFO("LH_CONSTRUCTION_INFO", "mesBomItfService", "syncLhConstructionInfo", "SKU与施工关系表"),

    /**
     * 半部件BOM接口
     */
    CONSTRUCTION_INFO("MES_CONSTRUCTION_INFO", "mesBomItfService", "syncConstructionInfo", "半部件BOM接口"),

    /**
     * 成型及半部件BOM施工信息同步
     */
    BOM_INFO("MES_BOM_INFO", "mesBomItfService", "syncBomInfo", "成型及半部件BOM施工信息同步"),

    /**
     * 成品物料信息同步
     */
    BAS_MATERIAL("MES_BAS_MATERIAL", "mesItfService", "syncMaterial", "成品物料信息同步"),

    /**
     * 模壳台账信息同步
     */
    MOLD_SHELL_SYNC("MOLD_SHELL_SYNC", "mesItfService", "syncMoldShell", "模壳台账信息同步"),

    /**
     * 设备保养计划同步
     */
    DEV_MAINTENANCE_PLAN("DEV_MAINTENANCE_PLAN", "mesItfService", "syncDevMaintenancePlan", "设备保养计划同步"),

    /**
     * 胶囊已使用次数同步
     */
    LH_USED_CAPSULE("LH_REPAIR_CAPSULE", "mesItfService", "syncLhRepairCapsule", "胶囊已使用次数同步"),

    /**
     * 模具清洗预警计划同步
     */
    MOULD_CLEAN_PLAN("MOULD_CLEAN_PLAN", "mesItfService", "syncMouldCleanPlan", "模具清洗预警计划同步"),

    /**
     * 结构整车胎面配置同步
     */
    STRUCTURE_TREAD_CONFIG("STRUCTURE_TREAD_CONFIG", "mesItfService", "syncStructureTreadConfig", "结构整车胎面配置同步"),

    /**
     * 生胎库存同步
     */
    EMBRYO_STOCK_SYNC("MES_CX_STOCK", "mesItfService", "syncMesCxStock", "生胎库存同步"),

    /**
     * 成型排程完成量同步
     */
    CX_CLASS_SHIFT_FINISH_QTY("CX_CLASS_SHIFT_FINISH_QTY", "mesItfService", "syncCxClassShiftFinishQty", "成型排程完成量同步"),
    
    /**
     * 硫化排程完成量同步
     */
    LH_CLASS_SHIFT_FINISH_QTY("LH_CLASS_SHIFT_FINISH_QTY", "mesItfService", "syncLhClassShiftFinishQty", "硫化排程完成量同步"),
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
