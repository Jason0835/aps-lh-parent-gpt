package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 续作类型
 * 1、SKU 续作SKU
 * 2、SPEC_PATTERN 分组下同规格同花纹
 * 3、EMBRYO_SHARE_MOULD 分组下共生胎同模具--换活字块
 *
 * @author ZLT
 * @date 20251230
 */
@Getter
public enum ContinueTypeEnum {
    /**
     * SKU 续作SKU
     */
    SAME_SKU("SKU", "续作SKU"),
    /**
     * SPEC_PATTERN 分组下同规格同花纹
     */
    SAME_SPECIFICATIONS_PATTERN("SPEC_PATTERN", "分组下同规格同花纹"),
    /**
     * EMBRYO_SHARE_MOULD 分组下共生胎同模具--换活字块
     */
    SAME_EMBRYO_CODE_SHARE_MOULD("EMBRYO_SHARE_MOULD", "分组下共生胎同模具"),
    /**
     * 非续作SKU
     */
    NO_CONTINUE("NO_CONTINUE","非续作SKU");

    private String continueType;

    private String desc;

    ContinueTypeEnum(String continueType, String desc) {
        this.continueType = continueType;
        this.desc = desc;
    }
}
