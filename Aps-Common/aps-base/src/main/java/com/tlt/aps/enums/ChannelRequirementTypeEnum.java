package com.tlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 渠道需求类型枚举类
 * 并不是根据销售订单的渠道来判定
 * OE库位-统一为OE配套
 * 内销库位，根据渠道，途虎为途虎，快准为快准，其它为RT
 * 外销库位，根据品牌区分贴牌与非贴牌
 *
 * @author ZLT
 * 20250923
 */
public enum ChannelRequirementTypeEnum {
    /**
     * OE配套
     */
    OE("0101", "OE配套"),
    /**
     * 外贸贴牌
     */
    FOREIGN_OEM("0201", "外贸贴牌"),
    /**
     * 外贸非贴牌
     */
    FOREIGN_NO_OEM("0202", "外贸非贴牌"),
    /**
     * 内销-途虎
     */
    DOMESTIC_TF("0301", "途虎"),
    /**
     * 内销快准
     */
    DOMESTIC_KZ("0302", "快准"),
    /**
     * 内销RT
     */
    DOMESTIC_RT("0303", "RT");

    private String code;

    private String name;
    /**
     * 内销-线上途虎
     */
    private static final String TF = "02";
    /**
     * 内销-快准
     */
    private static final String KZ = "02";

    /**
     * 根据需求渠道值，获取销售订单渠道类型
     *
     * @param value
     * @return
     */
    public static ChannelRequirementTypeEnum getInstance(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (ChannelRequirementTypeEnum type : values()) {
            if (type.getCode().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据需求的库位、渠道、品牌，得到需求渠道类型
     *
     * @param locationType       库位
     * @param channelCode        渠道
     * @param brandCode          品牌
     * @param foreignOemBrandSet 外销贴牌品牌
     * @return
     */
    public static ChannelRequirementTypeEnum getInstance(String locationType, String channelCode, String brandCode, Set<String> foreignOemBrandSet) {
        if (StringUtils.isBlank(locationType) || StringUtils.isBlank(channelCode) || StringUtils.isBlank(brandCode)) {
            return null;
        }
        LocationTypeEnum locationTypeEnum = LocationTypeEnum.getEnumByValue(locationType);
        if (null == locationTypeEnum) {
            return null;
        }
        //OE
        if (LocationTypeEnum.OE_LOCATION == locationTypeEnum) {
            return ChannelRequirementTypeEnum.OE;
        }
        //内销
        if (LocationTypeEnum.DOMESTIC_LOCATION == locationTypeEnum) {
            if (TF.equals(channelCode)) {
                return ChannelRequirementTypeEnum.DOMESTIC_TF;
            }
            if (KZ.equals(channelCode)) {
                return ChannelRequirementTypeEnum.DOMESTIC_KZ;
            }
            return ChannelRequirementTypeEnum.DOMESTIC_RT;
        }
        //外贸
        if (foreignOemBrandSet.contains(brandCode)) {
            return ChannelRequirementTypeEnum.FOREIGN_OEM;
        }
        return ChannelRequirementTypeEnum.FOREIGN_NO_OEM;
    }


    ChannelRequirementTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
