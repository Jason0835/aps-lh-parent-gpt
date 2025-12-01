package com.zlt.aps.factory.enums;

/**
 * 排产限制类型定义类
 * 0 无限制
 * 1 每天新增规格数限制
 *
 * @author ZLT
 * @date 20251013
 */
public enum ProductionLimitTypeEnum {
    /**
     * 0 无限制
     */
    NO_LIMIT(0, "无限制"),
    /**
     * 1 每天新增规格数限制
     */
    DAY_ADD_PRODUCT_CODE_NUMBER_LIMIT(1, "每天新增规格数限制"),
    /**
     * 2 每天总产能数限制
     */
    DAY_CAPACITY_LIMIT(2, "每天总产能数限制"),
    /**
     * 3 每天寸口产能数数限制(维度：寸口|*|工装类别|*|成型法|*|胎体布层级)
     */
    DAY_PRO_SIZE_CAPACITY_LIMIT(3, "每天寸口产能数数限制"),
    /**
     * 4 每天成型硫化配比数限制(维度：寸口|*|工装类别|*|成型法|*|胎体布层级)
     */
    DAY_MOULD_QTY_LIMIT(4, "每天成型硫化配比数限制");

    private Integer limitType;

    private String describe;

    ProductionLimitTypeEnum(Integer limitType, String describe) {
        this.limitType = limitType;
        this.describe = describe;
    }
}
