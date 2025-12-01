package com.zlt.aps.factory.enums;

/**
 * 拼模业务场景
 *
 * @author ZLT
 * @date 20250728
 */
public enum AssemblingMouldBusinessTypeEnum {
    /**
     * 单模规格拼模排产--只有一副模具
     */
    SINGLE_MOULD_PRODUCT("singleMould", "单模规格拼模排产"),
    /**
     * 多模量小规格的拼模排产
     */
    MULTI_MOULD_SMALL_QTY("multiMouldSmallQty", "多模量小规格拼模排产");

    private String assemblingMouldType;

    private String remark;

    AssemblingMouldBusinessTypeEnum(String assemblingMouldType, String remark) {
        this.assemblingMouldType = assemblingMouldType;
        this.remark = remark;
    }
}
