package com.zlt.aps.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;
import java.util.Arrays;

/**
 * 三班制班次枚举，对应数据字典 class_num_three_plan。
 */
@Getter
@AllArgsConstructor
public enum ThreeShiftEnum {

    /** 夜班，业务日期前一日22:00至业务日期06:00。 */
    NIGHT("01", "夜班", LocalTime.of(22, 0), LocalTime.of(6, 0)),

    /** 早班，业务日期06:00至14:00。 */
    MORNING("02", "早班", LocalTime.of(6, 0), LocalTime.of(14, 0)),

    /** 中班，业务日期14:00至22:00。 */
    MIDDLE("03", "中班", LocalTime.of(14, 0), LocalTime.of(22, 0));

    /** 班次编码。 */
    private final String code;

    /** 班次名称。 */
    private final String name;

    /** 班次开始时间。 */
    private final LocalTime startTime;

    /** 班次结束时间。 */
    private final LocalTime endTime;

    /**
     * 根据班次编码获取枚举。
     *
     * @param code 班次编码
     * @return 班次枚举，未匹配时返回null
     */
    public static ThreeShiftEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(shift -> shift.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断班次是否跨自然日。
     *
     * @return true表示班次开始时间晚于结束时间
     */
    public boolean isCrossDay() {
        return this.startTime.isAfter(this.endTime);
    }
}
