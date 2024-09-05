package com.zlt.aps.cx.engine.enums;


import lombok.Getter;

/**
 * 班次枚举类
 */
@Getter
public enum AdjustTypeEnums {

    CHANGE_QTY(1,"调量"),CHANGE_LH_MACHINE(2,"调整硫化机台");

    /**
     * 班次下标
     */
    private int changeType;
    /**
     * 班次名称
     */
    private String changeTypeName;

    private AdjustTypeEnums(int changeType, String changeTypeName){
        this.changeType=changeType;
        this.changeTypeName=changeTypeName;
    }

    /**
     * 根据下标获取
     * @param changeType
     * @return
     */
    public static AdjustTypeEnums getChangeTypeEnums(int changeType) {
        for (AdjustTypeEnums adjustType : AdjustTypeEnums.values()) {
            if (adjustType.getChangeType() == changeType) {
                return adjustType;
            }
        }
        return null;
    }
}
