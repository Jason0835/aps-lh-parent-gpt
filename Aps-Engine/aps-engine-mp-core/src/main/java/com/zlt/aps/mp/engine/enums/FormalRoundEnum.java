package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 排产轮次枚举定义类
 * 01 续作Sku
 * 02 不同结构主花纹比例调整
 * 03 同规格同花纹
 * 04 同模具
 * 05 最低实单硫化机台数排产
 * 06 前半段-优先级顺序排产
 * 07 后半段-优先级顺序排产
 *
 * @author ZLT
 * @date 20260416
 */
@Getter
public enum FormalRoundEnum {
    /**
     * 01 纯续作Sku
     */
    CONTINUE_SKU("01", "纯续作Sku"),
    /**
     * 02 不同分组共用模具比例调整(TBR：结构+主花纹)
     */
    GROUP_SHARE_RATIO_ADJUST("02", "不同分组共用模具比例调整"),
    /**
     * 03 分组下同规格同花纹
     */
    GROUP_SAME_SPECIFICATIONS_PATTERN("03", "分组下同规格同花纹"),
    /**
     * 04 分组下同模具
     */
    GROUP_SHARE_MOULD("04", "分组下同模具"),
    /**
     * 05 实单最低硫化机台数
     */
    ACTUAL_MIN_LH_MACHINE("05", "实单最低硫化机台数"),
    /**
     * 06 结构优先级排产前半段
     */
    FIRST_HALF_PRIORITY("06", "结构优先级排产前半段"),
    /**
     * 07 结构优先级排产后半段
     */
    LATTER_HALF_PRIORITY("07", "结构优先级排产后半段"),
    /**
     * 08 一次性排产完毕
     */
    DISPOSABLE_LH_MACHINE("08", "一次性排产完毕");

    /**
     * 轮次代码
     */
    private String roundCode;
    /**
     * 轮次描述
     */
    private String roundDesc;

    FormalRoundEnum(String roundCode, String roundDesc) {
        this.roundCode = roundCode;
        this.roundDesc = roundDesc;
    }
}
