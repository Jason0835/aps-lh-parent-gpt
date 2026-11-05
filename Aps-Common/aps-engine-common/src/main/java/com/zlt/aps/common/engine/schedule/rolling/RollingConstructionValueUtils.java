package com.zlt.aps.common.engine.schedule.rolling;

import java.math.BigDecimal;

/**
 * 自动滚动施工字段比较规范化工具。
 */
public final class RollingConstructionValueUtils {

    private RollingConstructionValueUtils() {
    }

    /**
     * 保留编码字段的文本语义，仅清理首尾空白。
     *
     * @param value 编码原始值
     * @return 保留前导零及大小写的文本值
     */
    public static String normalizeCode(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /**
     * 按数值语义规范化施工数值，不进行舍入或容差比较。
     *
     * @param value 施工数值或数值文本
     * @return 末尾零及科学计数法归一后的普通十进制文本；非法值保留原文并标记
     */
    public static String normalizeDecimal(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return "";
        }
        String rawValue = value.toString().trim();
        try {
            return new BigDecimal(rawValue).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            return "NON_NUMERIC:" + rawValue;
        }
    }
}
