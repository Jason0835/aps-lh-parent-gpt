package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 成型法枚举定义类
 *
 * @author ZLT
 * 20250307
 */
public enum FormingMethodTypeEnum {
    /**
     * 1 一次法
     */
    SINGLE_STAGE_TIRE("1", 1, "一次法"),
    /**
     * 2 二次法
     */
    TWO_STAGE_TIRE("2", 2, "二次法");

    private String methodValue;

    private Integer formingMethod;

    private String methodDesc;

    FormingMethodTypeEnum(String methodValue, Integer formingMethod, String methodDesc) {
        this.methodValue = methodValue;
        this.formingMethod = formingMethod;
        this.methodDesc = methodDesc;
    }

    /**
     * 根据成型法值，得到成型法实例
     *
     * @param methodValue
     * @return
     */
    public static FormingMethodTypeEnum getInstance(String methodValue) {
        if (null == methodValue) {
            return null;
        }
        return Arrays.stream(values()).filter(methodType -> methodType.getMethodValue().equals(methodValue)).findFirst().orElse(null);
    }

    /**
     * 根据成型法，取得另外一个成型法的实例
     * 因目前成型法只有一次法和二次法
     *
     * @param methodValue
     * @return
     */
    public static FormingMethodTypeEnum getChangeType(String methodValue) {
        if (StringUtils.isBlank(methodValue)) {
            return FormingMethodTypeEnum.SINGLE_STAGE_TIRE;
        }
        FormingMethodTypeEnum match = null;
        for (FormingMethodTypeEnum type : values()) {
            if (type.getMethodValue().equals(methodValue)) {
                match = type;
                break;
            }
        }
        if (null == match) {
            return FormingMethodTypeEnum.SINGLE_STAGE_TIRE;
        }
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE == match) {
            return FormingMethodTypeEnum.TWO_STAGE_TIRE;
        }
        return FormingMethodTypeEnum.SINGLE_STAGE_TIRE;
    }

    /**
     * 成型法-值
     *
     * @return
     */
    public String getMethodValue() {
        return methodValue;
    }

    /**
     * 成型法-值
     *
     * @return
     */
    public Integer getFormingMethod() {
        return formingMethod;
    }

    /**
     * 成型法-说明
     *
     * @return
     */
    public String getMethodDesc() {
        return methodDesc;
    }
}
