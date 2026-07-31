package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 排产Sku共用优先类型
 * 01 共用模具或是同胎胚
 * 02 同成型编号
 *
 * @author ZLT
 * @date 20260731
 */
@Getter
public enum ProductionShareTypeEnum {
    /**
     * 01 共用模具或是同胎胚
     */
    SHARE_MOLD_OR_EMBRYO("01", "共用模具或是同胎胚"),
    /**
     * 02 同成型编号
     */
    SHARE_FORMING_NO("02", "同成型编号");

    private String shareType;

    private String desc;

    ProductionShareTypeEnum(String shareType, String desc) {
        this.shareType = shareType;
        this.desc = desc;
    }
}
