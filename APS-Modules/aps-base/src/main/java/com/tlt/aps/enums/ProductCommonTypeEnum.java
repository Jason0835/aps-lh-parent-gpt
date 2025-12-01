package com.tlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 物料的公用规格类型
 *
 * @author ZLT
 * 20250226
 */
public enum ProductCommonTypeEnum {

    /**
     * 公用规格
     */
    COMMON_TYPE("1", "公用规格", LocationTypeEnum.DOMESTIC_LOCATION),
    /**
     * 内销专用
     */
    DOMESTIC__TYPE("3", "内销专用", LocationTypeEnum.DOMESTIC_LOCATION),
    /**
     * 外销专用
     */
    FOREIGN_TYPE("2", "外销专用", LocationTypeEnum.FOREIGN_LOCATION),
    /**
     * OE专用
     */
    OE_TYPE("4", "OE专用", LocationTypeEnum.OE_LOCATION);

    private String code;

    private String desc;

    private LocationTypeEnum locationType;

    /**
     * 根据公用规格类型，获取对应的备货匹配库位
     *
     * @param code
     * @return
     */
    public static LocationTypeEnum getLocationTypeByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return LocationTypeEnum.DOMESTIC_LOCATION;
        }
        ProductCommonTypeEnum commonType = getInstance(code);
        if (null == commonType) {
            return LocationTypeEnum.DOMESTIC_LOCATION;
        }
        return commonType.getLocationType();
    }

    /**
     * 根据编码获取对应的公用规格枚举实例对象
     *
     * @param code
     * @return
     */
    public static ProductCommonTypeEnum getInstance(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return Arrays.stream(values()).filter(productCommon -> productCommon.getCode().equals(code)).findFirst().orElse(null);
    }

    ProductCommonTypeEnum(String code, String desc, LocationTypeEnum locationType) {
        this.code = code;
        this.desc = desc;
        this.locationType = locationType;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public LocationTypeEnum getLocationType() {
        return locationType;
    }
}
