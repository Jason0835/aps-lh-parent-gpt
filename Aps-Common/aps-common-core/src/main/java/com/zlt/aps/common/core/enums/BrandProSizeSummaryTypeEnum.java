package com.zlt.aps.common.core.enums;

import com.ruoyi.common.utils.StringUtils;

/**
 * @author Chen
 * @date 2025/3/17
 */
public enum BrandProSizeSummaryTypeEnum {

    /**
     * 品牌-尺寸汇总分析类型，1-品牌
     */
    SUMMARY_TYPE_BRAND("1", "brand"),

    /**
     * 品牌-尺寸汇总分析类型，2-寸别
     */
    SUMMARY_TYPE_PRO_SIZE("2", "proSize")
    ;

    private final String typeCode;
    private final String typeName;

    BrandProSizeSummaryTypeEnum(String typeCode, String typeName) {
        this.typeCode = typeCode;
        this.typeName = typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * 根据类型获取对应的名称
     *
     * @param typeCode 类型
     * @return 结果
     */
    public static BrandProSizeSummaryTypeEnum getNameByCode(String typeCode) {
        if (StringUtils.isEmpty(typeCode)) {
            return null;
        }
        for (BrandProSizeSummaryTypeEnum enums : BrandProSizeSummaryTypeEnum.values()) {
            if (enums.getTypeCode().equals(typeCode)) {
                return enums;
            }
        }
        return null;
    }
}
