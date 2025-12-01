package com.zlt.aps.common.core.enums;

import lombok.Getter;

/**
 * 半部件工序代码对应计量单位枚举
 *
 * @author Chen
 * @since 2025/8/4
 */
@Getter
public enum HalfComponentCodeEnums {

    /**
     * 胎面计量单位
     */
    TM("tm", "胎面", "treadCode"),

    /**
     * 胎侧计量单位
     */
    TC("tc", "胎侧", "sidewallCode"),

    /**
     * 内衬计量单位
     */
    NC("nc", "内衬", "insideCode"),

    /**
     * 钢丝斜裁计量单位
     */
    CD15_1("cd15_1", "钢丝斜裁", "beltCode1"),

    /**
     * 钢丝斜裁计量单位
     */
    CD15_2("cd15_2", "钢丝斜裁", "beltCode2"),

    /**
     * 纤维直裁计量单位
     */
    CD90("cd90", "纤维直裁", "clothCode"),

    /**
     * 钢带压延计量单位
     */
    GDYY("gdyy", "钢带压延", "bigRollCode"),

    /**
     * 纤维压延计量单位
     */
    XWYY("xwyy", "纤维压延", "bigRollCode"),

    /**
     * 胎圈计量单位
     */
    TQ("tq", "胎圈", "tireRingCode"),

    /**
     * 钢丝圈计量单位
     */
    GSQ("gsq", "钢丝圈", "beadCode"),

    /**
     * 密炼计量单位
     */
    ML("ml", "密炼", ""),

    /**
     * 成型计量单位
     */
    CX("cx", "成型", ""),

    /**
     * 硫化计量单位
     */
    LH("lh", "硫化", ""),

    ;

    /**
     * 工序代号
     */
    private final String productProcess;

    /**
     * 工序名称
     */
    private final String productName;

    /**
     * 工序字段属性名称
     */
    private final String fieldName;

    HalfComponentCodeEnums(String productProcess, String productName, String fieldName) {
        this.productProcess = productProcess;
        this.productName = productName;
        this.fieldName = fieldName;
    }

    /**
     * 根据半部件编号获取对应的计量单位枚举
     *
     * @param productProcess 半部件编号
     * @return 结果
     */
    public static HalfComponentCodeEnums getEnumByProductProcess(String productProcess) {
        if (productProcess == null) {
            return null;
        }
        for (HalfComponentCodeEnums enums : HalfComponentCodeEnums.values()) {
            if (enums.getProductProcess().equals(productProcess)) {
                return enums;
            }
        }
        return null;
    }
}
