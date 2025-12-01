package com.zlt.aps.mps.common;

/**
 * 物料类型
 * 
 * @Description
 * @Author zlt
 * @Date 2025-03-26
 */
public enum MaterTypeEnum {
    /**
     * 胎胚
     */
    GT("GT"),
    /**
     * 内衬
     */
    FG("FG"),
    /**
     * 胎面
     */
    TR("TR"),
    /**
     * 胎体布
     */
    FN("FN"),
    /**
     * 胎圈
     */
    HS("HS"),
    /**
     * 钢丝圈
     */
    BH("BH"),
    /**
     * 胎侧
     */
    BSW("BSW"),
    /**
     * 1#带束层
     */
    BA("BA"),
    /**
     * 2#带束层
     */
    BB("BB"),
    /**
     * 帘布大卷
     */
    PA("PA"),
    /**
     * 钢丝大卷
     */
    SD("SD"),
    /**
     * 边胶
     */
    EN("EN"),
    /**
     * 三角胶
     */
    BX("BX");

    private String code;

    private MaterTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
