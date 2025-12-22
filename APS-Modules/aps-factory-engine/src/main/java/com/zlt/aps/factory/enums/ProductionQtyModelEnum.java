package com.zlt.aps.factory.enums;

import lombok.Getter;

/**
 * 排产量模式
 * 0 高优先级量
 * 1 净需求量
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
    NET_QTY(1, "总净需求量");

    private Integer modelCode;

    private String desc;

    ProductionQtyModelEnum(Integer modelCode, String desc) {
        this.modelCode = modelCode;
        this.desc = desc;
    }
}
