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
    NO_PRODUCTION_QTY("MP1101", "可排产量为零，无需排产", ""),
    /**
     * MP1102 可排产量为零，无需排产
     */
    NO_HAS_PRODUCT_CODE("MP1102", "需求计划没有物料编码", ""),
    /**
     * MP1201 物料停产状态
     */
    PRODUCT_STATUS_STOP("MP1201", "物料停产状态", ""),
    /**
     * MP1202 工厂不排产
     */
    FACTORY_NO_PRODUCTION("MP1202", "工厂不排产", ""),
    /**
     * MP1203 没有结构
     */
    NO_STRUCTURE_NAME("MP1203", "没有结构", ""),
    /**
     * MP1204 没有寸口
     */
    NO_PRO_SIZE("MP1204", "没有寸口", ""),
    /**
     * MP1205 没有日硫化产能
     */
    NO_DAY_LH_CAPACITY("MP1205", "没有日硫化产能", ""),
    /**
     * MP1301 没有配置施工关系
     */
    NO_CONSTRUCTION_RELATION("MP1301", "没有配置施工关系", ""),
    /**
     * MP1302 施工阶段为空/施工阶段非正式
     */
    NO_CONSTRUCTION_STAGE_OR_NO_FORMAL("MP1302", "施工阶段为空/施工阶段非正式", ""),
    /**
     * MP1303 没有胎胚号
     */
    NO_EMBRYO_CODE("MP1303", "没有胎胚号", ""),
    /**
     * MP1304 没有规格代号
     */
    NO_SPEC_CODE("MP1304", "没有规格代号", ""),
    /**
     * MP1305 没有硫化时间
     */
    NO_CURING_TIME("MP1305", "没有硫化时间", ""),
    /**
     * MP1306 没有配置成型法
     */
    NO_MOULD_METHOD("MP1306", "没有配置成型法", ""),
    /**
     * MP1401 没有模具关系
     */
    NO_MOULD_RELATION("MP1401", "没有模具关系", ""),
    /**
     * MP1402 模具台账不存在
     */
    NO_MOULD_INFO("MP1402", "模具台账不存在", ""),
    /**
     * MP1403 模具状态禁用
     */
    MOULD_STATUS_DISABLE("MP1403", "模具状态禁用", ""),
    /**
     * MP1501 超出模具产能
     */
    EXCEED_MOULD_CAPACITY("MP1501", "超出模具产能", ""),
    /**
     * MP1502 超出共用模具产能
     */
    EXCEED_SHARE_MOULD_CAPACITY("MP1502", "超出共用模具产能", ""),
    /**
     * MP2101 成型产能不足，整个结构不排
     */
    EXCEED_CX_CAPACITY_WHOLE_STRUCTURE_NAME("MP2101", "成型产能不足，整个结构不排", ""),
    /**
     * MP2102 没有达到最低起产天数，不排
     */
    NO_MIN_CX_CAPACITY_WHOLE_STRUCTURE_NAME("MP2102", "没有达到最低起产天数，不排", ""),
    /**
     * MP2201 成型产能不足，整个英寸不排
     */
    EXCEED_CX_CAPACITY_WHOLE_PRO_SIZE("MP2201", "成型产能不足，整个英寸不排", ""),
    /**
     * MP2202 没有达到最低起产天数，不排
     */
    NO_MIN_CX_CAPACITY_WHOLE_PRO_SIZE("MP2202", "没有达到最低起产天数，不排", ""),
    /**
     * MP2301 成型没有配置日产能
     */
    NO_CONFIGURATION_QUOTA("MP2301", "成型没有配置日产能", ""),
    /**
     * MP2302 没有成型硫化配比配置
     */
    NO_CONFIGURATION_LH_RATIO("MP2302", "没有成型硫化配比配置", ""),
    /**
     * MP3101 特殊轮胎产能控制
     */
    TIRE_TYPE_CONTROL("MP3101", "特殊轮胎产能控制", ""),
    /**
     * MP3201 超出寸口产能控制
     */
    EXCEED_PRO_SIZE_CAPACITY("MP3201", "超出寸口产能控制", ""),
    /**
     * MP3301 超出结构产能控制
     */
    EXCEED_STRUCTURE_NAME_CAPACITY("MP3301", "超出结构产能控制", ""),
    /**
     * MP3401 达到最高配比
     */
    EXCEED_MAX_CX_MOULD("MP3401", "达到最高配比", ""),
    /**
     * MP3402 小于最低硫化配比
     */
    BELOW_MIN_CX_MOULD("MP3402", "小于最低硫化配比", ""),
    /**
     * MP3501 没有可用模具(共用)
     */
    NO_ENABLE_MOULD("MP3501", "没有可用模具(共用)", ""),
    /**
     * MP3502 模具产能不足
     */
    MOULD_CAPACITY_SHORTAGE("MP3502", "模具产能不足", ""),
    /**
     * MP3601 因日产能限制
     */
    DAY_MAX_CAPACITY_LIMIT("MP3601", "因日产能限制", ""),
    /**
     * MP3602 因换模能力限制
     */
    DAY_CHANGE_MOULD_LIMIT("MP3602", "因换模能力限制", ""),
    /**
     * MP3603 因胎胚种类数限制
     */
    DAY_EMBRYO_CODE_LIMIT("MP3603", "因胎胚种类数限制", "");

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
