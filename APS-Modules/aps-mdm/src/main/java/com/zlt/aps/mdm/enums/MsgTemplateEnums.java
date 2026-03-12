package com.zlt.aps.mdm.enums;

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
     * SKU原余量小于调整次日至锁定截止日的计划量提醒
     */
    MP_SKU_REMAIN_QTY_NO_FULL("MP_SKU_REMAIN_QTY_NO_FULL", "SKU原余量小于调整次日至锁定截止日的计划量提醒"),
    /**
     * 产量预测生成成功通知
     */
    MP_CREATE_PRODUCTION_PREDICT("MP_CREATE_PRODUCTION_PREDICT", "产量预测"),
    /**
     * 实单模拟排产成功通知
     */
    MP_CREATE_SIMULATED_PRODUCTION("MP_CREATE_SIMULATED_PRODUCTION","实单模拟排产"),

    /**
     * 工作日历提醒通知
     */
    WORK_CALENDAR_NOTICE("WORK_CALENDAR_NOTICE", "工作日历提醒通知"),
    ;

    private final String code;
    private final String name;

    MsgTemplateEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
