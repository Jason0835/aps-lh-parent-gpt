package com.zlt.aps.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * 库位列表枚举类，字典：biz_stor_type
 *
 * @author ZLT
 * 20250217
 */
public enum LocationTypeEnum {
    /**
     * 内销 1
     */
    DOMESTIC_LOCATION("1", "内销"),
    /**
     * 外销 2
     */
    FOREIGN_LOCATION("2", "外销"),
    /**
     * OE 3
     */
    OE_LOCATION("3", "OE");
    /**
     * 编码
     */
    private String value;
    /**
     * 描述
     */
    private String name;

    LocationTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据类别值，获取库位类别枚举实例对象
     *
     * @param value
     * @return
     */
    public static LocationTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }

        for (LocationTypeEnum productTypeEnum : LocationTypeEnum.values()) {
            if (productTypeEnum.getValue().equals(value)) {
                return productTypeEnum;
            }
        }

        return null;
    }

    /**
     * 按外销-OE-内销顺序排序
     *
     * @return
     */
    public static List<LocationTypeEnum> getStockUpSort() {
        List<LocationTypeEnum> sortList = new ArrayList<>();
        sortList.add(FOREIGN_LOCATION);
        sortList.add(OE_LOCATION);
        sortList.add(DOMESTIC_LOCATION);
        return sortList;
    }

    /**
     * 根据类别名称，获取库位类别枚举实例对象
     *
     * @param name 名称
     * @return 结果
     */
    public static LocationTypeEnum getEnumByName(String name) {
        if (name == null) {
            return null;
        }

        for (LocationTypeEnum productTypeEnum : LocationTypeEnum.values()) {
            if (productTypeEnum.getName().equals(name)) {
                return productTypeEnum;
            }
        }

        return null;
    }
}
