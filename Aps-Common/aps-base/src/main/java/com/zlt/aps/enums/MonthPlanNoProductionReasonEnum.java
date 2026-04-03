package com.zlt.aps.enums;

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
     * MP1207 没有主花纹
     */
    NO_MAIN_PATTERN("MP1207", "没有主花纹", "alg.data.initCheck.noMainPattern"),
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
     * MP1303 【SKU与示方书关系】未找到%s
     */
    NO_CONSTRUCTION_ALL_INFO("MP1303", "【SKU与示方书关系】未找到%s", "alg.data.initCheck.noConstructionConfigurationInfo"),
    /**
     * MP1303 没有胎胚号
     */
    NO_EMBRYO_CODE("MP1303", "没有胎胚号", "alg.data.initCheck.noFindEmbryoCode"),
    /**
     * MP1303-1 没有制造示方书号
     */
    NO_EMBRYO_NO("MP1303-1","没有制造示方书号","alg.data.initCheck.noFindEmbryoNo"),
    /**
     * MP1303-2 没有文字示方书号
     */
    NO_TEXT_NO("MP1303-2","没有文字示方书号","alg.data.initCheck.noFindTextNo"),
    /**
     * MP1303-3 没有硫化示方书号
     */
    NO_LH_NO("MP1303-2","没有硫化示方书号","alg.data.initCheck.noFindLhNo"),
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
     * MP2103 因低于实单最低配比提前收尾,导致没有达到最低起产天数，不排
     */
    NO_LOW_MIN_LH_MACHINE_COUNT_WHOLE_STRUCTURE_NAME("MP2103", "因低于实单最低配比提前收尾,导致没有达到最低起产天数，不排", "alg.data.groupCapacity.noLowMinLhMachineCountWholeStructureName"),
    /**
     * MP2104 因没有分配到成型产能，整个结构不排
     */
    NO_PRODUCTION_CX_MACHINE("MP2104", "因没有分配到成型产能，整个结构不排", "alg.data.groupCapacity.NoAllocationCxMachineCapacity"),
    /**
     * MP2105 周期结构不在月周期排产清单，整个结构不排
     */
    NO_PRODUCTION_NO_MONTH_LIST("MP2105","周期结构不在月周期排产清单，整个结构不排", "alg.data.initCheck.noExistMonthCycleGroupList"),
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
     * MP2303 结构没有成型硫化配比配置
     */
    GROUP_NO_CONFIGURATION_LH_RATION("MP2303", "结构没有成型硫化配比配置", "alg.data.groupCapacity.groupNoConfigurationLhRatio"),
    /**
     * MP2304 结构下Sku特殊材料种类配置不一致
     */
    GROUP_SPECIAL_MATERIAL_NO_SAME("MP2304", "结构下Sku特殊材料种类配置不一致", "alg.data.groupCapacity.groupSpecialMaterialNoSame"),
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
    DAY_EMBRYO_CODE_LIMIT("MP3603", "因胎胚种类数限制", "alg.data.mouldProduction.dayEmbryoCodeLimit"),
    /**
     * MP3701 缺少特殊原材料数据
     */
    SPECIAL_RAW_MATERIAL_NOTEMPTY("MP3701", "缺少特殊原材料数据", "alg.data.before.production.rawSpecialMaterialNoEmpty"),
    /**
     * MP3702 缺少生产日历数据
     */
    PRODUCTION_CALENDAR_NOTEMPTY("MP3702", "缺少生产日历数据", "alg.data.before.production.productionCalendarNotEmpty"),
    /**
     * MP3703 缺少成型机基础数据
     */
    MOLD_MACHINE_BASEDATA_NOTEMPTY("MP3703", "缺少成型机基础数据", "alg.data.before.production.moldMachineBaseDataNotEmpty"),
    /**
     * MP3704 缺少工装台账数据
     */
    WORKWEAR_INVENTORY_NOTEMPTY("MP3704", "缺少工装台账数据", "alg.data.before.production.workwearInventoryNotEmpty"),
    /**
     * MP3705 缺少模具分配比例配置
     */
    MOLD_ALLOCATION_RATIO_CONFIG_NOTEMPTY("MP3705", "缺少模具分配比例配置", "alg.data.before.production.moldAllocationRatioConfigNotEmpty"),
    /**
     * MP3706 缺少模壳数据
     */
    MOLD_SHELL_NOTEMPTY("MP3706", "缺少模壳数据", "alg.data.before.production.moldShellNotEmpty"),
    /**
     * MP3707 缺少胶囊卡盘数据
     */
    CAPSULE_CHUCK_NOTEMPTY("MP3707", "缺少胶囊卡盘数据", "alg.data.before.production.capsuleChuckNotEmpty"),
    /**
     * MP3708 缺少结构成型硫化配比数据
     */
    STRUCTURE_FORMING_VULCANIZATION_RATIO_NOTEMPTY("MP3708", "缺少结构成型硫化配比数据", "alg.data.before.production.structureFormingVulcanizationRatioNotEmpty"),
    /**
     * MP3709 获取排产参数设定失败
     */
    PARAMS_CONFIG("MP3709", "获取排产参数设定失败", "alg.data.before.production.paramsConfigException"),
    /**
     * MP3710 超6个成品库存信息处理失败
     */
    OVER_SIX_MONTH_STOCK_ERROR("MP3710", "超6个成品库存信息处理失败", "alg.data.before.production.overSixMonthStockError"),
    /**
     * MP3711 初始化库销比处理失败
     */
    INIT_PRODUCTION_REQUIRE_PLAN_ERROR("MP3711", "初始化库销比处理失败", "alg.data.before.production.initProductionRequirePlanError"),
    /**
     * MP3712 构建全局日排产限制信息失败
     */
    DAY_CAPACITY_LIMIT_ERROR("MP3712", "构建全局日排产限制信息失败", "alg.data.before.production.dayCapacityLimitError"),
    /**
     * MP3713 获取成型鼓信息失败
     */
    WORKWEAR_TYPE_INFO_ERROR("MP3713", "获取成型鼓信息失败", "alg.data.before.production.workearTypeInfoError"),
    /**
     * MP3714 获取机台近3个月的生产历史信息失败
     */
    PRODUCTION_HISTORY_ERROR("MP3714", "获取机台近3个月的生产历史信息失败", "alg.data.before.production.productionHistoryError"),
    /**
     * MP3715 根据计划的物料描述补充模具关系中的物料结构名失败
     */
    MOULD_STRUCTURE_NAME_ERROR("MP3715", "根据计划的物料描述补充模具关系中的物料结构名失败", "alg.data.before.production.mouldStructureNameError"),
    /**
     * MP3716 构建结构、主花纹的模具信息失败
     */
    GROUP_MAIN_PATTERN_ERROR("MP3716", "构建结构、主花纹的模具信息失败", "alg.data.before.production.groupMainPatternError"),
    /**
     * MP3798-1 成型产能受限
     */
    NO_ENOUGH_CX_MACHINE_CAPACITY("MP3798-1","成型产能受限","alg.data.mouldProduction.noEnoughCxMachineCapacity"),
    /**
     * MP3798-2 模具产能受限
     */
    NO_ENOUGH_MOULD_CAPACITY("MP3798-2","模具产能受限","alg.data.mouldProduction.noEnoughMouldCapacity"),
    /**
     * MP3799 因%s不排
     */
    GENERAL_NO_PRODUCTION_REASON("MP3799", "因%s不排", "alg.data.mouldProduction.generalNoProductionReasons"),
    /**
     * MP3797 因%s部分未排
     */
    GENERAL_PART_NO_PRODUCTION_REASON("MP3797", "因%s部分未排", "alg.data.mouldProduction.generalPartNoProductionReasons");

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
