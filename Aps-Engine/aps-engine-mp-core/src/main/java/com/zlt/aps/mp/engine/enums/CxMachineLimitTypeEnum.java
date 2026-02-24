package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 成型机台限制枚举类型定义
 * 01 胎胚种类数限制
 * 02 最大硫化机台数限制
 * 03 实单最低硫化机台数限制
 *
 * @author ZLT
 * @date 20251227
 */
@Getter
public enum CxMachineLimitTypeEnum {
    /**
     * 01 胎胚种类数限制
     */
    MAX_EMBRYO_SIZE("01", "胎胚种类数限制"),
    /**
     * 02 最大硫化机台数限制
     */
    MAX_LH_COUNT("02", "最大硫化机台数限制"),
    /**
     * 03 实单最低硫化机台数限制
     */
    MIN_LH_COUNT("03", "实单最低硫化机台数限制");

    private String type;

    private String desc;

    CxMachineLimitTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
