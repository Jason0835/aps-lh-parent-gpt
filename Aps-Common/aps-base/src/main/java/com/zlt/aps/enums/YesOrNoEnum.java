package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 是或否的枚举类
 *
 * @author ZLT
 * @date 20250212
 */
public enum YesOrNoEnum {
    /**
     * 1 是
     */
    YES("1", "是", 1),
    /**
     * 0 否
     */
    NO("0", "否", 0);

    private String code;
    private String name;
    private Integer value;

    YesOrNoEnum(String code, String name, Integer value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getValue() {
        return value;
    }

    /**
     * 根据值获取对应枚举类，空对象则为NO，匹配不到则为空
     *
     * @param value
     * @return
     */
    public static YesOrNoEnum getEnumByValue(Integer value) {
        if (null == value) {
            return YesOrNoEnum.NO;
        }
        for (YesOrNoEnum type : YesOrNoEnum.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据值获取对应枚举类，空对象则为NO，匹配不到则为空
     *
     * @param code
     * @return
     */
    public static YesOrNoEnum getEnumByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return YesOrNoEnum.NO;
        }
        for (YesOrNoEnum type : YesOrNoEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
