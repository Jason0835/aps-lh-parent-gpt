package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * CD15 班次展示与日期类型转换工具。
 */
public final class Cd15ShiftDisplayHelper {

    private Cd15ShiftDisplayHelper() {
    }

    /**
     * 获取动态班次描述中的展示名称。
     *
     * @param shift 班次描述
     * @return 班次展示名称
     */
    public static String shiftDisplayName(Cd15ShiftDescriptor shift) {
        if (shift == null) {
            return null;
        }
        return StringUtils.hasText(shift.getShiftDisplayName())
                ? shift.getShiftDisplayName() : shift.getClassField();
    }

    /**
     * LocalDate 转 Date。
     *
     * @param value 日期
     * @return Date
     */
    public static Date toDate(LocalDate value) {
        return value == null ? null
                : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
