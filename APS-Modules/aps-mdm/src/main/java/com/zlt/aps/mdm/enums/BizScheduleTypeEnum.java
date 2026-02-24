package com.zlt.aps.mdm.enums;

import lombok.Getter;

/**
 * 字典：biz_schedule_type业务排产类型枚举
 * 01-主销产品、02-常规产品、03-常规周期产品、04-波动性产品、05-按单排产产品
 *
 * @author Chen
 * @since 2025/12/11
 */
@Getter
public enum BizScheduleTypeEnum {

    /**
     * 主销产品
     */
    MAIN_SALE_PRODUCT("01", "主销产品"),

    /**
     * 常规产品
     */
    ORDINARY_PRODUCT("02", "常规产品"),

    /**
     * 常规周期产品
     */
    ORDINARY_CYCLE_PRODUCT("03", "常规周期产品"),

    /**
     * 波动性产品
     */
    WAVE_PRODUCT("04", "波动性产品"),

    /**
     * 按单排产产品
     */
    ORDINARY_ORDER_PRODUCT("05", "按单排产产品");

    private final String code;
    private final String name;

    BizScheduleTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
