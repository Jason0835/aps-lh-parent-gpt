package com.zlt.aps.monthplan.api.enums;

/**
 * 销售订单的数据来源
 *
 * @author ZLT
 * 20250306
 */
public enum SaleOrderSourceTypeEnum {
    /**
     * 1 内销系统
     */
    DOMESTIC_SYSTEM(1, "内销系统"),
    /**
     * 2 外销系统
     */
    FOREIGN_SYSTEM(2, "外销系统"),
    /**
     * 3 导入
     */
    IMPORT(3, "导入");
    /**
     * 来源类型
     */
    private Integer sourceType;
    /**
     * 备注说明
     */
    private String desc;

    SaleOrderSourceTypeEnum(Integer sourceType, String desc) {
        this.sourceType = sourceType;
        this.desc = desc;
    }

    /**
     * 根据类型值，获取枚举实例对象，默认为导入
     *
     * @param sourceType
     * @return
     */
    public static SaleOrderSourceTypeEnum getInstance(Integer sourceType) {
        if (null == sourceType) {
            return SaleOrderSourceTypeEnum.IMPORT;
        }
        for (SaleOrderSourceTypeEnum type : values()) {
            if (type.getSourceType().equals(sourceType)) {
                return type;
            }
        }
        return SaleOrderSourceTypeEnum.IMPORT;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public String getDesc() {
        return desc;
    }
}
