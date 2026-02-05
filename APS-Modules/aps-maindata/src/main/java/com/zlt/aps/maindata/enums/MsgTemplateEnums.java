package com.zlt.aps.maindata.enums;

import lombok.Getter;

/**
 * 消息中心模板
 *
 * @author Ncik
 * @since 2026/1/29
 */
@Getter
public enum MsgTemplateEnums {

    /**
     * 原材料用量偏差预警
     */
    RAW_WARNING_RECORD("RAW_WARNING_RECORD", "原材料用量偏差预警"),

    /**
     * 原材料新材料预警
     */
    RAW_NEW_WARNING("RAW_NEW_WARNING", "原材料新材料预警"),

    /**
     * 原材料新材料提醒
     */
    MP_SKU_REMAIN_QTY_NO_FULL("MP_SKU_REMAIN_QTY_NO_FULL", "SKU原余量小于调整次日至锁定截止日的计划量提醒");

    private final String code;
    private final String name;

    MsgTemplateEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
