package com.zlt.aps.factory.enums;

/**
 * 上模方式枚举定义
 *
 * @author ZLT
 * @date 20250219
 */
public enum MouldProductionTypeEnum {
    /**
     * 按计划评估模数（降模生产）
     */
    ESTIMATE_MOULDS(1),

    /**
     * 按品种最多可用模数（集中上模）
     */
    MOST_MOULDS(2);

    private int value;

    MouldProductionTypeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * 根据值获取上模方式实例对象
     *
     * @param value
     * @return
     */
    public static MouldProductionTypeEnum getEnumByValue(Integer value) {
        if (null == value) {
            return null;
        }
        for (MouldProductionTypeEnum mouldProductionTypeEnum : MouldProductionTypeEnum.values()) {
            if (mouldProductionTypeEnum.getValue() == value.intValue()) {
                return mouldProductionTypeEnum;
            }
        }
        return null;
    }
}
