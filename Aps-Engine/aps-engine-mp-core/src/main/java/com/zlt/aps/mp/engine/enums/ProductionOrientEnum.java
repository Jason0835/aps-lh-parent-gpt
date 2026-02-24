package com.zlt.aps.mp.engine.enums;

/**
 * 排产方向枚举定义类
 * 0 正向 从月初到月末方式
 * 1 逆向 从月末到月初方式
 *
 * @author ZLT
 * @date 20250219
 */
public enum ProductionOrientEnum {
    /**
     * 正向排产
     */
    FORWARD(0, "正向排产"),
    /**
     * 逆向排产
     */
    REVERSE(1, "反向排产");

    private Integer value;

    private String desc;

    ProductionOrientEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
}
