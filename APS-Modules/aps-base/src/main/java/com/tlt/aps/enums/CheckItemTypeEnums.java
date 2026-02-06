package com.tlt.aps.enums;

import lombok.Getter;

/**
 * 检测项类型枚举
 *
 * @author hsc
 * @since 2026/1/29
 */
@Getter
public enum CheckItemTypeEnums {


    SPECIAL_RAW_MATERIAL_DATA("01", "特殊原材料数据"),
    PRODUCTION_CALENDAR_DATA("02", "生产日历数据"),
    BASIC_DATA_OF_MOLDING_MACHINE("03", "成型机基础数据"),
    REPAIR_DATA_OF_MOLDING_MACHINE("04", "成型机维修数据"),
    EQUIPMENT_LEDGER_DATA("05", "工装台账数据"),
    INIT_DATA("06", "初始化数据(物料信息、模具关系配置、模具到货计划、施工关系配置、日硫化产能配置)"),
    MOLD_ALLOCATION_RATIO_DATA("07", "模具分配比例配置数据"),
    MOLD_SHELL_DATA("08", "模壳数据"),
    CAPSULE_CHUCK_DATA("09", "胶囊卡盘数据"),
    SULFURIZATION_RATIO_DATA("10", "结构成型硫化配比数据"),
    OTHER_PARAMS_CONFIG("11", "其他参数配置");


    private String code;
    private String name;

    CheckItemTypeEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
