/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 班次枚举
 *
 * @author zlt
 */
@Getter
@AllArgsConstructor
public enum ShiftEnum {

    NIGHT_SHIFT("01", "夜班", "夜班 Ca đêm", "22:00", "06:00"),
    MORNING_SHIFT("02", "早班", "早班 Ca sáng", "06:00", "14:00"),
    AFTERNOON_SHIFT("03", "中班", "中班 Ca chiều", "14:00", "22:00");

    /** 班次编码 */
    private final String code;

    /** 班次描述 */
    private final String description;

    /**
     * 模具交替计划导出使用的中越双语班次名称
     */
    private final String mouldChangePlanExcelText;

    /** 开始时间 */
    private final String startTime;

    /** 结束时间 */
    private final String endTime;

    /**
     * 根据编码获取枚举
     *
     * @param code 班次编码
     * @return 班次枚举，未找到返回null
     */
    public static ShiftEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ShiftEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据模具交替计划模板文本获取班次枚举，支持中文短标签和中越双语文本。
     *
     * @param text 模板中的班次文本
     * @return 匹配的班次枚举，空值或不支持的文本返回null
     */
    public static ShiftEnum getByMouldChangePlanExcelText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalizedText = text.trim();
        for (ShiftEnum shiftEnum : values()) {
            if (shiftEnum.mouldChangePlanExcelText != null
                    && (shiftEnum.description.equals(normalizedText)
                    || shiftEnum.mouldChangePlanExcelText.equals(normalizedText))) {
                return shiftEnum;
            }
        }
        return null;
    }

    /**
     * 根据模具交替计划库表班次值获取枚举，兼容历史单字符班次编码。
     *
     * @param code 库表班次值
     * @return 匹配的班次枚举，空值或不支持的编码返回null
     */
    public static ShiftEnum getByMouldChangePlanStoredCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalizedCode = code.trim();
        ShiftEnum shiftEnum = getByCode(normalizedCode);
        if (shiftEnum == null && normalizedCode.length() == 1) {
            shiftEnum = getByCode("0" + normalizedCode);
        }
        return shiftEnum;
    }
}
