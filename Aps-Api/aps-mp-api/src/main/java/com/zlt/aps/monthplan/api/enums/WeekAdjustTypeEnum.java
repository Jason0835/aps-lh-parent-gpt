package com.zlt.aps.monthplan.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;

/**
 * 周程滚动调整类型枚举
 * @author wengpc
 */
@AllArgsConstructor
@Getter
public enum WeekAdjustTypeEnum {

    STRUCTURE_IN("01", "结构内"),
    STRUCTURE_OUT("02","结构外"),
    ;

    private String code;
    private String name;

    public static WeekAdjustTypeEnum getByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return Arrays.stream(WeekAdjustTypeEnum.values())
                .filter(item -> item.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

}
