package com.tlt.aps.enums;

/**
 * 月计划工序未排原因定义
 *
 * @author zlt
 * @since 20251209
 */
public enum MonthPlanNoProductionReasonEnum {
    /**
     * MP1101 可排产量为零，无需排产
     */
    NO_PRODUCTION_QTY("MP1101", "可排产量为零，无需排产", "alg.data.initCheck.noProductionQty"),
    /**
     * MP1102 需求计划没有物料编码
     */
    NO_HAS_PRODUCT_CODE("MP1102", "需求计划没有物料编码", "alg.data.initCheck.noHasMaterialCode"),
    /**
     * MP1103 需求计划不排产
     */
    PLAN_NO_PRODUCTION("MP1103", "需求计划不排产", "alg.data.initCheck.planNoProduction"),
    /**
     * MP1201 物料停产状态
     */
    PRODUCT_STATUS_STOP("MP1201", "物料停产状态", "alg.data.initCheck.productStatusStop"),
    /**
     * MP1202 工厂不排产
     */
    FACTORY_NO_PRODUCTION("MP1202", "工厂不排产", "alg.data.initCheck.factoryNoProduction"),
    /**
     * MP1203 没有结构
     */
    NO_STRUCTURE_NAME("MP1203", "没有结构", "alg.data.initCheck.noStructureName"),
    /**
     * MP1204 没有寸口
     */
    NO_PRO_SIZE("MP1204", "没有寸口", "alg.data.initCheck.noProSize"),
    /**
     * MP1205 没有日硫化产能关系
     */
    NO_DAY_LH_CAPACITY_RELATION("MP1205", "没有日硫化产能关系", "alg.data.initCheck.noDayLhCapacityRelation"),
    /**
     * MP1206 没有日硫化量
     */
    NO_DAY_LH_CAPACITY("MP1206", "没有日硫化量", "alg.data.initCheck.noDayLhCapacity"),
    /**
     * MP1301 没有配置施工关系
     */
    NO_CONSTRUCTION_RELATION("MP1301", "没有配置施工关系", "alg.data.initCheck.noConstructionRelation"),
    /**
     * MP1302 施工阶段为空/施工阶段非正式
     */
    NO_CONSTRUCTION_STAGE_OR_NO_FORMAL("MP1302", "施工阶段为空/施工阶段非正式", "alg.data.initCheck.noConstructionStageOrNoFormal"),
    /**
     * MP1303 没有胎胚号
     */
    NO_EMBRYO_CODE("MP1303", "没有胎胚号", "alg.data.initCheck.noEmbryoCode"),
    /**
     * MP1304 没有规格代号
     */
    NO_SPEC_CODE("MP1304", "没有规格代号", "alg.data.initCheck.noSpecCode"),
    /**
     * MP1305 没有硫化时间
     */
    NO_CURING_TIME("MP1305", "没有硫化时间", "alg.data.initCheck.noCuringTime"),
    /**
     * MP1306 没有配置成型法
     */
    NO_MOULD_METHOD("MP1306", "没有配置成型法", "alg.data.initCheck.noMouldMethod"),
    /**
     * MP1401 没有模具关系
     */
    NO_MOULD_RELATION("MP1401", "没有模具关系", "alg.data.initCheck.noMouldRelation"),
    /**
     * MP1402 模具台账不存在
     */
    NO_MOULD_INFO("MP1402", "模具台账不存在", "alg.data.initCheck.noMouldInfo"),
    /**
     * MP1403 模具状态禁用
     */
    MOULD_STATUS_DISABLE("MP1403", "模具状态禁用", "alg.data.initCheck.mouldStatusDisable"),
    /**
     * MP1501 超出模具产能
     */
    EXCEED_MOULD_CAPACITY("MP1501", "超出模具产能", "alg.data.initCheck.exceedMouldCapacity"),
    /**
     * MP1502 超出共用模具产能
     */
    EXCEED_SHARE_MOULD_CAPACITY("MP1502", "超出共用模具产能", "alg.data.initCheck.exceedShareMouldCapacity"),
    /**
     * MP2101 成型产能不足，整个结构不排
     */
    EXCEED_CX_CAPACITY_WHOLE_STRUCTURE_NAME("MP2101", "成型产能不足，整个结构不排", "alg.data.groupCapacity.exceedCxCapacityWholeStructureName"),
    /**
     * MP2102 没有达到最低起产天数，不排
     */
    NO_MIN_CX_CAPACITY_WHOLE_STRUCTURE_NAME("MP2102", "没有达到最低起产天数，不排", "alg.data.groupCapacity.noMinCxCapacityWholeStructureName"),
    /**
     * MP2201 成型产能不足，整个英寸不排
     */
    EXCEED_CX_CAPACITY_WHOLE_PRO_SIZE("MP2201", "成型产能不足，整个英寸不排", "alg.data.groupCapacity.exceedCxCapacityWholeProSize"),
    /**
     * MP2202 没有达到最低起产天数，不排
     */
    NO_MIN_CX_CAPACITY_WHOLE_PRO_SIZE("MP2202", "没有达到最低起产天数，不排", "alg.data.groupCapacity.noMinCxCapacityWholeProSize"),
    /**
     * MP2301 成型没有配置日产能
     */
    NO_CONFIGURATION_QUOTA("MP2301", "成型没有配置日产能", "alg.data.groupCapacity.noConfigurationQuota"),
    /**
     * MP2302 没有成型硫化配比配置
     */
    NO_CONFIGURATION_LH_RATIO("MP2302", "没有成型硫化配比配置", "alg.data.groupCapacity.noConfigurationLhRatio"),
    /**
     * MP3101 特殊轮胎产能控制
     */
    TIRE_TYPE_CONTROL("MP3101", "特殊轮胎产能控制", "alg.data.mouldProduction.tireTypeControl"),
    /**
     * MP3201 超出寸口产能控制
     */
    EXCEED_PRO_SIZE_CAPACITY("MP3201", "超出寸口产能控制", "alg.data.mouldProduction.exceedProSizeCapacity"),
    /**
     * MP3301 超出结构产能控制
     */
    EXCEED_STRUCTURE_NAME_CAPACITY("MP3301", "超出结构产能控制", "alg.data.mouldProduction.exceedStructureNameCapacity"),
    /**
     * MP3401 达到最高配比
     */
    EXCEED_MAX_CX_MOULD("MP3401", "达到最高配比", "alg.data.mouldProduction.exceedMaxCxMould"),
    /**
     * MP3402 小于最低硫化配比
     */
    BELOW_MIN_CX_MOULD("MP3402", "小于最低硫化配比", "alg.data.mouldProduction.belowMinCxMould"),
    /**
     * MP3501 没有可用模具(共用)
     */
    NO_ENABLE_MOULD("MP3501", "没有可用模具(共用)", "alg.data.mouldProduction.noEnableMould"),
    /**
     * MP3502 模具产能不足
     */
    MOULD_CAPACITY_SHORTAGE("MP3502", "模具产能不足", "alg.data.mouldProduction.mouldCapacityShortage"),
    /**
     * MP3601 因日产能限制
     */
    DAY_MAX_CAPACITY_LIMIT("MP3601", "因日产能限制", "alg.data.mouldProduction.dayMaxCapacityLimit"),
    /**
     * MP3602 因换模能力限制
     */
    DAY_CHANGE_MOULD_LIMIT("MP3602", "因换模能力限制", "alg.data.mouldProduction.dayChangeMouldLimit"),
    /**
     * MP3603 因胎胚种类数限制
     */
    DAY_EMBRYO_CODE_LIMIT("MP3603", "因胎胚种类数限制", "alg.data.mouldProduction.dayEmbryoCodeLimit");

    private String errorCode;

    private String errorName;

    private String i18nKey;

    MonthPlanNoProductionReasonEnum(String errorCode, String errorName, String i18nKey) {
        this.errorCode = errorCode;
        this.errorName = errorName;
        this.i18nKey = i18nKey;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorName() {
        return errorName;
    }

    public String getI18nKey() {
        return i18nKey;
    }
}
