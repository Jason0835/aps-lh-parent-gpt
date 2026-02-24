package com.zlt.aps.enums;

/**
 * 品名枚举类
 * @author Chad
 * 2021年7月26日11:26:28
 */
public enum ProductTypeEnum {
    /**
     * 全钢
     */
    WHOLE_STEEL("TBR", "全钢"),
    /**
     * 半钢
     */
    SEMI_STEEL("PCR", "半钢"),
    /**
     *特胎
     */
    SPECIAL_TYPE("OTR", "特胎"),
    /**
     * 斜交
     */
    OBLIQUE_INTERSECTION("TBB", "斜交");

    private String value;
    private String name;

    ProductTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static ProductTypeEnum getEnumByValue(String value){
        if (value == null){
            return null;
        }

        for (ProductTypeEnum productTypeEnum : ProductTypeEnum.values()) {
            if (productTypeEnum.getValue().equals(value)){
                return productTypeEnum;
            }
        }

        return null;
    }
}
