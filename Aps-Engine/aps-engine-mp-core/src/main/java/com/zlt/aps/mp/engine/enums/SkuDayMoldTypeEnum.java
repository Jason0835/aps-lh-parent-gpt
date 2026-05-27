package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * Sku日排产模具类型
 * 1 降模 后一日相比前一日使用模具数减少
 * 2 增模 后一日相比前一日使用模具数增多
 *
 * @author ZLT
 * @date 20260526
 */
@Getter
public enum SkuDayMoldTypeEnum {

    /**
     * 1 降模
     */
    REDUCED_MOLD(1, "降模"),
    /**
     * 2 增模
     */
    ADD_MOLD(2, "增模");

    private Integer type;

    private String desc;

    SkuDayMoldTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
