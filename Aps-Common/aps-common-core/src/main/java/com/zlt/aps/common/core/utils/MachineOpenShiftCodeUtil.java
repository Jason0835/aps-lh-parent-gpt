package com.zlt.aps.common.core.utils;

import java.util.Locale;
import java.util.Set;

/**
 * 机台开机班次编码匹配工具。
 *
 * <p>兼容历史数字开机班次编码和当前班次配置编码：01 表示夜班、02 表示早班、03 表示中班。
 * 未命中映射时仍保留原有班次编码精确匹配能力。</p>
 */
public final class MachineOpenShiftCodeUtil {

    /** 历史夜班开机编码。 */
    private static final String LEGACY_NIGHT_SHIFT_CODE = "01";

    /** 历史早班开机编码。 */
    private static final String LEGACY_DAY_SHIFT_CODE = "02";

    /** 历史中班开机编码。 */
    private static final String LEGACY_AFTERNOON_SHIFT_CODE = "03";

    /**
     * 工具类不允许实例化。
     */
    private MachineOpenShiftCodeUtil() {
    }

    /**
     * 判断机台是否开放指定班次。
     *
     * @param openShiftCodes 机台维护的开机班次编码集合
     * @param shiftCode 当前班次配置编码
     * @return true 表示机台可承接当前班次；false 表示未开放或班次信息缺失
     */
    public static boolean isMachineShiftOpen(Set<String> openShiftCodes, String shiftCode) {
        String normalizedShiftCode = normalize(shiftCode);
        if (openShiftCodes == null || openShiftCodes.isEmpty() || normalizedShiftCode == null) {
            return false;
        }
        if (contains(openShiftCodes, normalizedShiftCode)) {
            return true;
        }
        String legacyOpenShiftCode = resolveLegacyOpenShiftCode(normalizedShiftCode);
        return legacyOpenShiftCode != null && contains(openShiftCodes, legacyOpenShiftCode);
    }

    /**
     * 将当前班次配置编码转换为历史数字开机班次编码。
     *
     * @param shiftCode 班次配置编码，例如 AFTERNOON_D1、NIGHT_D2、DAY_D2
     * @return 对应历史编码；非标准班次编码返回 null
     */
    public static String resolveLegacyOpenShiftCode(String shiftCode) {
        String normalizedShiftCode = normalize(shiftCode);
        if (normalizedShiftCode == null) {
            return null;
        }
        if (normalizedShiftCode.startsWith("NIGHT_")) {
            return LEGACY_NIGHT_SHIFT_CODE;
        }
        if (normalizedShiftCode.startsWith("DAY_")) {
            return LEGACY_DAY_SHIFT_CODE;
        }
        if (normalizedShiftCode.startsWith("AFTERNOON_")) {
            return LEGACY_AFTERNOON_SHIFT_CODE;
        }
        return null;
    }

    /**
     * 判断编码集合中是否包含指定编码。
     *
     * @param openShiftCodes 开机班次编码集合
     * @param targetShiftCode 待匹配班次编码
     * @return true 表示包含
     */
    private static boolean contains(Set<String> openShiftCodes, String targetShiftCode) {
        return openShiftCodes.stream()
                .map(MachineOpenShiftCodeUtil::normalize)
                .anyMatch(targetShiftCode::equals);
    }

    /**
     * 规范化班次编码。
     *
     * @param shiftCode 原始班次编码
     * @return 去空格并转大写后的编码；空值返回 null
     */
    private static String normalize(String shiftCode) {
        if (shiftCode == null || shiftCode.trim().isEmpty()) {
            return null;
        }
        return shiftCode.trim().toUpperCase(Locale.ROOT);
    }
}
