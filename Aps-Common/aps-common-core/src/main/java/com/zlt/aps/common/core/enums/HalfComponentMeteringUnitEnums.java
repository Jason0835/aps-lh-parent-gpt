package com.zlt.aps.common.core.enums;

import lombok.Getter;

/**
 * 半部件工序代码对应计量单位枚举
 *
 * @author Chen
 * @since 2025/8/4
 */
@Getter
public enum HalfComponentMeteringUnitEnums {

    /**
     * 胎面计量单位
     */
    TM("tm", "米", "treadConsumeQty"),

    /**
     * 胎侧计量单位
     */
    TC("tc", "米", "sideWallConsumeQty"),

    /**
     * 内衬计量单位
     */
    NC("nc", "米", "sideWallConsumeQty"),

    /**
     * 钢丝斜裁计量单位
     */
    CD15("cd15", "米", "fitConsumeQty"),

    /**
     * 纤维直裁计量单位
     */
    CD90("cd90", "米", "sideWallConsumeQty"),

    /**
     * 钢带压延计量单位
     */
    GDYY("gdyy", "米", "gdyyCxConsumeQty"),

    /**
     * 纤维压延计量单位
     */
    XWYY("xwyy", "米", "sideWallConsumeQty"),

    /**
     * 胎圈计量单位
     */
    TQ("tq", "副(一副两个)", "cxConsumeQty"),

    /**
     * 钢丝圈计量单位
     */
    GSQ("gsq", "副(一副两个)", ""),

    /**
     * 密炼计量单位
     */
    ML("ml", "车", ""),

    /**
     * 成型计量单位
     */
    CX("cx", "条", ""),

    /**
     * 硫化计量单位
     */
    LH("lh", "条", ""),

    ;

    /**
     * 工序代号
     */
    private final String productProcess;

    /**
     * 计量单位
     */
    private final String meteringUnit;

    /**
     * 成型消耗量字段名称
     */
    private final String consumeFieldName;

    HalfComponentMeteringUnitEnums(String productProcess, String meteringUnit, String consumeFieldName) {
        this.productProcess = productProcess;
        this.meteringUnit = meteringUnit;
        this.consumeFieldName = consumeFieldName;
    }

    /**
     * 根据半部件编号获取对应的计量单位枚举
     *
     * @param productProcess 半部件编号
     * @return 结果
     */
    public static HalfComponentMeteringUnitEnums getEnumByProductProcess(String productProcess) {
        if (productProcess == null) {
            return null;
        }
        for (HalfComponentMeteringUnitEnums enums : HalfComponentMeteringUnitEnums.values()) {
            if (enums.getProductProcess().equals(productProcess)) {
                return enums;
            }
        }
        return null;
    }
}
