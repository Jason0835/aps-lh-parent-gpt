package com.zlt.aps.itf.mes.enums;

import lombok.Getter;

/**
 * 接口码枚举类
 *
 * @author zlt
 * @since 2025/12/25
 */
@Getter
public enum ItfSyncKeyEnum {

    /**
     * 月计划下发接口
     */
    SYNC_MONTH_PLAN("MONTH_PLAN_ISSUE", "APS", "MES", "月计划下发接口"),
    /**
     * 设备保养计划同步
     */
    DEV_MAINTENANCE_PLAN("DEV_MAINTENANCE_PLAN", "MES", "APS", "设备保养计划同步"),

    /**
     * 胶囊已使用次数同步
     */
    LH_USED_CAPSULE("LH_USED_CAPSULE", "MES", "APS", "胶囊已使用次数同步"),

    /**
     * 模具清洗预警计划同步
     */
    MOULD_CLEAN_PLAN("MOULD_CLEAN_PLAN", "MES", "APS", "模具清洗预警计划同步"),

    /**
     * 结构整车胎面配置同步
     */
    STRUCTURE_TREAD_CONFIG("STRUCTURE_TREAD_CONFIG", "MES", "APS", "结构整车胎面配置同步"),

    /**
     * 生胎库存同步
     */
    EMBRYO_STOCK_SYNC("EMBRYO_STOCK_SYNC", "MES", "APS", "生胎库存同步"),

    /**
     * 成型排程结果下发
     */
    SYNC_CX_SCHEDULE_RESULT("FINISH_SCHE_RST_FBK", "APS", "MES", "成型排程结果下发接口"),

    /**
     * 成型排程完成量同步
     */
    CX_CLASS_SHIFT_FINISH_QTY("FINISH_SCHE_COMPLETE", "MES", "APS", "成型排程完成量同步"),

    /**
     * 硫化排程结果下发
     */
    SYNC_LH_SCHEDULE_RESULT("VULCANIZE_SCHE_RST_FBK", "APS", "MES", "硫化排程结果下发接口"),
    /**
     * 硫化排程完成量同步
     */
    LH_CLASS_SHIFT_FINISH_QTY("LH_CLASS_SHIFT_FINISH_QTY", "MES", "APS", "成型排程完成量同步"),

    /**
     * 成型排程日完成量同步
     */
    CX_SCHE_DAY_FINISH_QTY("CX_SCHE_DAY_FINISH_QTY", "MES", "APS", "成型排程日完成量同步"),

    /**
     * 硫化排程日完成量同步
     */
    LH_SCHE_DAY_FINISH_QTY("LH_SCHE_DAY_FINISH_QTY", "MES", "APS", "硫化排程日完成量同步"),
    ;
	/**
	 * 接口码
	 */
    private final String code;
    /**
     * 数据提供系统
     */
    private final String dataSys;
    /**
     * 数据接收系统
     */
    private final String dockSys;
    /**
     * 接口描述
     */
    private final String desc;

    ItfSyncKeyEnum(String code, String dataSys, String dockSys, String desc) {
        this.code = code;
        this.dataSys = dataSys;
        this.dockSys = dockSys;
        this.desc = desc;
    }

    public static ItfSyncKeyEnum getByCode(String code) {
        for (ItfSyncKeyEnum value : ItfSyncKeyEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
