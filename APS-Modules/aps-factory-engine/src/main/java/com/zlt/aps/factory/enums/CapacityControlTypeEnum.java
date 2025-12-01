package com.zlt.aps.factory.enums;

/**
 * 产能控制类型
 *
 * @author ZLT
 * @date 20250714
 */
public enum CapacityControlTypeEnum {
    /**
     * sizeCapacityControl 寸口产能控制
     */
    SIZE_CAPACITY_CONTROL("sizeCapacityControl", "寸口产能控制"),
    /**
     * tireTypeCapacityControl 特殊轮胎类型产能控制
     */
    TIRE_CAPACITY_CONTROL("tireTypeCapacityControl", "特殊轮胎类型产能控制");

    private String capacityControlType;

    private String remark;

    CapacityControlTypeEnum(String capacityControlType, String remark) {
        this.capacityControlType = capacityControlType;
        this.remark = remark;
    }

    public String getCapacityControlType() {
        return capacityControlType;
    }

    public String getRemark() {
        return remark;
    }
}
