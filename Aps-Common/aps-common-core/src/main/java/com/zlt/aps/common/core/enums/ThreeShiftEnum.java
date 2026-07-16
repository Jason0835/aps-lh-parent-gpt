package com.zlt.aps.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 标准三班编码。
 *
 * <p>该枚举只校验数据字典 {@code class_num_three_plan} 的标准编码，
 * 班次名称、时间和跨日规则由各业务班次配置表维护。</p>
 */
@Getter
@AllArgsConstructor
public enum ThreeShiftEnum {

    /** 夜班。 */
    NIGHT("01"),

    /** 早班。 */
    MORNING("02"),

    /** 中班。 */
    MIDDLE("03");

    /** 班次编码。 */
    private final String code;

    /**
     * 根据班次编码获取枚举。
     *
     * @param code 班次编码
     * @return 班次枚举，未匹配时返回 null
     */
    public static ThreeShiftEnum getByCode(String code) {
        return Arrays.stream(values())
                .filter(shift -> shift.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否为标准三班编码。
     *
     * @param code 班次编码
     * @return true 表示编码属于 01、02、03
     */
    public static boolean isValidCode(String code) {
        return getByCode(code) != null;
    }
}
