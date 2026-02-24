package com.zlt.aps.enums;

/**
 * @author Chen
 */
public enum CommonTypeEnum {

    /**
     * 公用规格
     */
    COMMON_SPEC("", 1, "公用规格"),

    /**
     * 外销专用
     */
    OUT("outGrossRate", 2, "外销专用"),

    /**
     * 内销专用
     */
    IN("inGrossRate", 3, "内销专用"),

    /**
     * OE专用
     */
    OE("oeGrossRate", 4, "OE专用");

    private final String fieldName;
    private final Integer commonType;
    private final String dictLabel;

    CommonTypeEnum(String fieldName, Integer commonType, String dictLabel) {
        this.fieldName = fieldName;
        this.commonType = commonType;
        this.dictLabel = dictLabel;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Integer getCommonType() {
        return commonType;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    /**
     * 根据公用类型字典值，获取公用类型字段名称
     *
     * @param commonType 公用类型字典值
     * @return 公用类型字段名称
     */
    public static String getFieldNameByCommonType(Integer commonType) {
        if (commonType == null) {
            return null;
        }

        for (CommonTypeEnum commonTypeEnum : CommonTypeEnum.values()) {
            if (commonTypeEnum.getCommonType().equals(commonType)) {
                return commonTypeEnum.getFieldName();
            }
        }

        return null;
    }

    /**
     * 根据公用类型字典值，获取公用类型字段名称
     *
     * @param fieldName 公用类型字典值
     * @return 公用类型字段名称
     */
    public static Integer getCommonTypeByFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }

        for (CommonTypeEnum commonTypeEnum : CommonTypeEnum.values()) {
            if (commonTypeEnum.getFieldName().equals(fieldName)) {
                return commonTypeEnum.getCommonType();
            }
        }

        return null;
    }
}
