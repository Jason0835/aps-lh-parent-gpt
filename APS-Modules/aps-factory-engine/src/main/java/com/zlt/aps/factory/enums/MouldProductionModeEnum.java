package com.zlt.aps.factory.enums;

/**
 * 排产模数
 *
 * @author ZLT
 * @date 20250219
 */
public enum MouldProductionModeEnum {
    /**
     * 1-双模具:有双数模必须排双数;只有1付模具，才能排单模
     */
    DOUBLE_MOULD(1),
    /**
     * 2-单模具：按1个模具直接开始排
     */
    SINGLE_MOULD(2);

    private int value;

    MouldProductionModeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * 根据值获取排产模块实例对象
     *
     * @param value
     * @return
     */
    public static MouldProductionModeEnum getEnumByValue(int value) {
        for (MouldProductionModeEnum mouldProductionModeEnum : MouldProductionModeEnum.values()) {
            if (mouldProductionModeEnum.getValue() == value) {
                return mouldProductionModeEnum;
            }
        }
        return null;
    }
}
