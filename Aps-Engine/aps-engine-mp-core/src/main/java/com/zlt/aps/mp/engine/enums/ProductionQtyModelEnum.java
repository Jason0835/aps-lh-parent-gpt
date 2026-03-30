package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 排产量模式
 * 0 高优先级量
 * 1 净需求量
 * 2 常规储备
 *
 * @author ZLT
 * @date 20250221
 */
@Getter
public enum ProductionQtyModelEnum {
    /**
     * 0 高优先级量
     */
    HEIGHT_QTY(0, "高优先级量"),
    /**
     * 1 总净需求量
     */
    NET_QTY(1, "总净需求量"),
    /**
     * 2 剩余可搭配量
     */
    REMAIN_MATCHING_QTY(2, "剩余可搭配量");

    private Integer modelCode;

    private String desc;

    ProductionQtyModelEnum(Integer modelCode, String desc) {
        this.modelCode = modelCode;
        this.desc = desc;
    }
}
