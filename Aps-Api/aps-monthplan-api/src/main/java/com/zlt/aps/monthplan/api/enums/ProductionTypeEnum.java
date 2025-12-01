package com.zlt.aps.monthplan.api.enums;

import java.util.Arrays;

/**
 * 模具排产类型区分
 *
 * @author ZLT
 * @date 20250312
 */
public enum ProductionTypeEnum {
    /**
     * 0 正常日
     */
    GENERAL_DAY(0, "正常日"),
    /**
     * 1 停工日
     */
    STOP_DAY(MouldNoProductionType.STOP_DAY.getType(), "停工日"),
    /**
     * 2 维修日
     */
    MAINTENANCE_DAY(MouldNoProductionType.MAINTENANCE_DAY.getType(), "维修日"),
    /**
     * 3 洗模日
     */
    MOULD_CLEANING_DAY(MouldNoProductionType.MOULD_CLEANING_DAY.getType(), "洗模日"),
    /**
     * 已排产完毕日
     */
    FINISH_DAY(4, "已排产完毕日");

    private Integer type;

    private String desc;

    ProductionTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 是否为正常排产日
     * 20250411 因洗模日不在是一整天洗模，故而导致洗模日还可排产。因此洗模日也当做正常排产日
     * 洗模日与正常日排产都是正常排产日
     *
     * @return
     */
    public boolean isNormalProduction() {
        if (ProductionTypeEnum.GENERAL_DAY == this) {
            return true;
        }
        if (ProductionTypeEnum.MOULD_CLEANING_DAY == this) {
            return true;
        }
        return false;
    }

    /**
     * 根据类型，获取对应枚举实例
     *
     * @param type
     * @return
     */
    public static ProductionTypeEnum getInstance(Integer type) {
        if (null == type) {
            return ProductionTypeEnum.GENERAL_DAY;
        }
        return Arrays.stream(values()).filter(productionType -> productionType.getType().equals(type)).findFirst().orElse(ProductionTypeEnum.GENERAL_DAY);
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
