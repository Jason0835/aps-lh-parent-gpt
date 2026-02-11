package com.zlt.aps.factory.daylimit;

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
    NO_LIMIT("00", "无限制", ""),
    /**
     * 01 胎胚种类数限制
     */
    EMBRYO_CODE_COUNT_LIMIT("01", "胎胚种类数限制", "alg.data.mouldProduction.embryoCodeCountLimit"),
    /**
     * 02 模具排产日限制
     */
    MOULD_LOCAL_LIMIT("02", "模具排产日限制", "alg.data.mouldProduction.mouldLocalLimit"),
    /**
     * 03 模壳总数限制
     */
    MOULD_SHELL_LIMIT("03", "模壳总数限制", "alg.data.mouldProduction.mouldShellLimit"),
    /**
     * 04 模具分配比例限制
     */
    MOULD_ALLOCATION_LIMIT("04", "模具分配比例限制", "alg.data.mouldProduction.mouldAllocationLimit"),
    /**
     * 05 胶囊卡盘总数限制
     */
    CAPSULE_CHUCK_LIMIT("05", "胶囊卡盘总数限制", "alg.data.mouldProduction.capsuleChuckLimit"),
    /**
     * 06 胎胚种类数、模具排产日双重限制
     */
    EMBRYO_COUNT_AND_MOULD_LOCAL_LIMIT("06", "胎胚种类数、模具排产日双重限制", "alg.data.mouldProduction.embryoCountAndMouldLocalLimit"),
    /**
     * 07 胎胚种类数、模具排产日、模壳总数多重限制
     */
    MOULD_SHELL_DOUBLE_LIMIT("07", "胎胚种类数、模具排产日、模壳总数多重限制", "alg.data.mouldProduction.mouldShellDoubleLimit"),
    /**
     * 08 胎胚种类数、模具排产日、模壳总数、模具分配比例多重限制
     */
    MOULD_ALLOCATION_DOUBLE_LIMIT("08", "胎胚种类数、模具排产日、模壳总数、模具分配比例多重限制", "alg.data.mouldProduction.mouldAllocationDoubleLimit"),
    /**
     * 09 胎胚种类数、模具排产日、模壳总数、模具分配比例、胶囊卡盘总数多重限制
     */
    CAPSULE_CHUCK_DOUBLE_LIMIT("09", "胎胚种类数、模具排产日、模壳总数、模具分配比例、胶囊卡盘总数多重限制", "alg.data.mouldProduction.capsuleChuckDoubleLimit"),
    /**
     * 10 换模次数限制
     */
    CHANGE_MOULD_LIMIT("09", "换模次数限制", "alg.data.mouldProduction.changeMouldLimit"),
    /**
     * 11 日产能上限限制
     */
    DAY_CAPACITY_LIMIT("11", "日产能上限限制", "alg.data.mouldProduction.dayCapacityLimit"),
    /**
     * 12 没有模具限制
     */
    FIND_MOULD_LIMIT("12", "没有模具限制", "alg.data.mouldProduction.enableMouldLimit"),
    /**
     * 16 胎胚种类数、模具排产日、模壳总数、模具分配比例、胶囊卡盘总数、日产能上限多重限制
     */
    DAY_CAPACITY_DOUBLE_LIMIT("16", "胎胚种类数、模具排产日、模壳总数、模具分配比例、胶囊卡盘总数、日产能上限多重限制", "alg.data.mouldProduction.dayCapacityDoubleLimit"),
    /**
     * 17 特殊原材料库存限制
     */
    SPECIAL_MATERIAL_STOCK_LIMIT("17", "特殊原材料库存限制", "alg.data.mouldProduction.specialMaterialStockLimit");
    /**
     * 限制类型
     */
    private String limitType;
    /**
     * 限制描述
     */
    private String limitDesc;
    /**
     * 国际化配置key
     */
    private String i18nKey;

    MouldProductionLimitTypeEnum(String limitType, String limitDesc, String i18nKey) {
        this.limitType = limitType;
        this.limitDesc = limitDesc;
        this.i18nKey = i18nKey;
    }
}
