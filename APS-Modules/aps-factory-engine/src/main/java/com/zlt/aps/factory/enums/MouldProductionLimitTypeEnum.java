package com.zlt.aps.factory.enums;

import lombok.Getter;

/**
 * 模具排产日限制控制枚举定义
 * 00 无限制
 * 01 胎胚种类数限制
 * 02 模具排产日限制
 * 03 模壳总数限制
 * 04 模具分配比例限制
 * 05 胶囊卡盘总数限制
 *
 * @author ZLT
 * 20260121
 */
@Getter
public enum MouldProductionLimitTypeEnum {
    /**
     * 00 无限制
     */
    NO_LIMIT("00", "无限制"),
    /**
     * 01 胎胚种类数限制
     */
    EMBRYO_CODE_COUNT_LIMIT("01", "胎胚种类数限制"),
    /**
     * 02 模具排产日限制
     */
    MOULD_LOCAL_LIMIT("02", "模具排产日限制"),
    /**
     * 03 模壳总数限制
     */
    MOULD_SHELL_LIMIT("03", "模壳总数限制"),
    /**
     * 04 模具分配比例限制
     */
    MOULD_ALLOCATION_LIMIT("04", "模具分配比例限制"),
    /**
     * 05 胶囊卡盘总数限制
     */
    CAPSULE_CHUCK_LIMIT("05", "胶囊卡盘总数限制"),
    /**
     * 06 胎胚种类数、模具排产日双重限制
     */
    EMBRYO_COUNT_AND_MOULD_LOCAL_LIMIT("06", "胎胚种类数、模具排产日双重限制"),
    /**
     * 07 胎胚种类数、模具排产日、模壳总数多重限制
     */
    MOULD_SHELL_DOUBLE_LIMIT("07", "胎胚种类数、模具排产日、模壳总数多重限制"),
    /**
     * 08 胎胚种类数、模具排产日、模壳总数、模具分配比例多重限制
     */
    MOULD_ALLOCATION_DOUBLE_LIMIT("08", "胎胚种类数、模具排产日、模壳总数、模具分配比例多重限制"),
    /**
     * 09 胎胚种类数、模具排产日、模壳总数、模具分配比例、胶囊卡盘总数多重限制
     */
    CAPSULE_CHUCK_DOUBLE_LIMIT("09", "胎胚种类数、模具排产日、模壳总数、模具分配比例、胶囊卡盘总数多重限制"),
    /**
     * 10 换模次数限制
     */
    CHANGE_MOULD_LIMIT("09", "换模次数限制");
    /**
     * 限制类型
     */
    private String limitType;
    /**
     * 限制描述
     */
    private String limitDesc;

    MouldProductionLimitTypeEnum(String limitType, String limitDesc) {
        this.limitType = limitType;
        this.limitDesc = limitDesc;
    }
}
