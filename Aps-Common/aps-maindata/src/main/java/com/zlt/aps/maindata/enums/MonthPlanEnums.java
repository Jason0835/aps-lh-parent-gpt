package com.zlt.aps.maindata.enums;

import lombok.Getter;

/**
 * 月计划参数枚举
 *
 * @author Chen
 * @since 2025/12/9
 */
@Getter
public enum MonthPlanEnums {

    /**
     * SYS0209001 新模具预计到货天数
     */
    MODULE_ARRIVAL_DAYS("SYS0209001", "单位天，新模具预计到货天数"),
    /**
     * SYS0201001 月份周期排产起始日
     */
    PRODUCTION_CYCLE_START("SYS0201001", "排产月份周期开始日"),
    /**
     * SYS0202001 初始化时，是否进行模具预占产能计算
     */
    OPEN_PREEMPTION_MOULD("SYS0202001", "初始化时，是否进行模具预占产能计算"),
    /**
     * SYS0202002 日硫化量使用的模式值:M = 使用MES的硫化量 S = 使用标准硫化量 A = 使用APS计算的硫化量；其他则认为采用标准硫化量
     */
    DAY_VULCANIZATION_MODE("SYS0202002", "日硫化量使用的模式值"),
    /**
     * SYS0202003 是否采用损耗率计算损耗
     */
    OPEN_LEVEL_RATIO("SYS0202003", "是否采用损耗率计算损耗"),
    /**
     * SYS0102001 从供应链同步的订单PO号如果包含有配置文字说明是储备订单，如果销售也没有维护优先级，则优先级默认中优先级
     */
    SALESORDER_STOCK_FLAG("SYS0102001", "储备订单标记"),
    /**
     * SYS0102002 从供应链同步的订单高优先级订单量超过设定的比例需要自动将提报日期较晚的高优先级订单调整为中优先级
     */
    HIGHT_PRIORITY_ORDER_RATE("SYS0102002", "高优先级订单占比"),
    /**
     * SYS0102003 周转天数(全局参数) ,用于供应链订单池备库上限的计算
     */
    TURN_OVER_DAYS("SYS0102003", "周转天数(全局参数) ,用于供应链订单池备库上限的计算"),
    /**
     * SYS0102004 从供应链同步的订单物料信息只需要指定前缀的
     */
    SCM_ORDER_MATRAL_CODE_PREFIX("SYS0102004", "从供应链同步的订单物料信息只需要符合指定前缀的数据"),
    /**
     * SYS0102005 查询近12个月的月均销量大于零的月份数
     */
    MONTH_SALE_QTY_MONTH("SYS0102005", "查询近12个月的月均销量大于零的月份数"),
    /**
     * SYS0102006 从供应链同步的订单物料信息只需要管控质量符合条件的数据
     */
    SCM_ORDER_MATRAL_QUALITY_STATE("SYS0102006", "从供应链同步的订单物料信息只需要管控质量符合条件的数据"),
    /**
     * SYS0103001 最小投产量
     */
    MIN_PRODUCTION_QTY("SYS0103001", "最小投产量"),
    /**
     * SYS0209002 前后结构断面宽相差值
     */
    SECTION_WIDTH_DIFF_VALUE("SYS0209002", "前后结构断面宽相差值"),
    /**
     * SYS0203001 每日最大可切换结构的成型机台数
     */
    DAY_CHANGE_GROUP_COUNT("SYS0203001", "每日最大可切换结构的成型机台数"),
    /**
     * SYS0203002 每日最大可换模的硫化机台数
     */
    CHANGE_MOULD_LH_MACHINE_NUMBER("SYS0203002", "每日最大可换模的硫化机台数"),
    /**
     * SYS0203003 换模时，SKU首日排产量
     */
    CHANGE_MOULD_FIRST_QTY("SYS0203003", "换模时，SKU首日排产量"),
    /**
     * SYS0203004 换活字块时，SKU收尾量与日硫化量差值
     */
    CHANGE_TYPE_BLOCK_QTY_DIFF("SYS0203004", "换活字块时，SKU收尾量与日硫化量差值"),
    /**
     * SYS0203005 换活字块时，前SKU差值<=SYS0203004时，后SKU的首日排产量
     */
    CHANGE_TYPE_BLOCK_QTY("SYS0203005", "换活字块时，前SKU差值<=SYS0203004时，后SKU的首日排产量"),
    /**
     * SYS0203006 换活字块时，前SKU差值>SYS0203004时，后SKU的首日排产量
     */
    CHANGE_TYPE_BLOCK_MAX_QTY("SYS0203006", "换活字块时，前SKU差值>SYS0203004时，后SKU的首日排产量"),
    /**
     * SYS0203007 单台成型机每日最大排产胎胚种类数
     */
    SINGLE_CX_EMBRYO_CODE_COUNT("SYS0203007", "单台成型机每日最大排产胎胚种类数"),
    /**
     * SYS0203008 每日最大产能
     */
    DAY_MAX_CAPACITY("SYS0203008", "每日最大产能"),
    /**
     * SYS0203009 每日最小产能
     */
    DAY_MIN_CAPACITY("SYS0203009", "每日最小产能"),
    /**
     * SYS0203011 外销贴牌-品牌配置
     */
    OEM_BRAND_CONFIG("SYS0203011", "外销贴牌-品牌配置"),
    /**
     * SYS0203012 外销贴牌-总产量配置，单位条
     */
    OEM_BRAND_CAPACITY("SYS0203012", "外销贴牌-总产量配置，单位条"),
    /**
     * SYS0203013 周期储备量占实单的比例(%)
     */
    RESERVE_PERCENT("SYS0203013", "周期储备量占实单的比例(%)"),

    /**
     * SYS0203014 按高优先级SKU个数降序的结构清单-前 X 个结构，单位个
     */
    STRUCTURE_BILL_PRE_COUNT("SYS0203014", "按高优先级SKU个数降序的结构清单-前 X 个结构，单位个"),

    /**
     * SYS0203015	外销贴牌是否参与结构优先级的竞争
     */
    OEM_JOIN_STRUCTURE_PRIORITY("SYS0203015", "外销贴牌是否参与结构优先级的竞争"),

    /**
     * SYS0204001 SKU总净需求量<=该值，SKU一次性排产
     */
    SUM_PRODUCTION_QTY("SYS0204001", "SKU总净需求量<=该值，SKU一次性排产"),
    /**
     * SYS0204002 SKU高优先级与总净需求量的差值<=该值，SKU一次性排产
     */
    HEIGHT_DIFF_QTY("SYS0204002", "SKU高优先级与总净需求量的差值<=该值，SKU一次性排产"),
    /**
     * SYS0204003 SKU二次上机的间隔时间
     */
    SKU_SECOND_PRODUCTION("SYS0204003", "SKU二次上机的间隔时间"),
    /**
     * SYS0204004 单位天，SKU符合SYS0204005时，SKU收尾日离结构收尾日可搭配补量的天数
     */
    MATCHING_BOOST_DAY("SYS0204004", "单位天，SKU符合SYS0204005时，SKU收尾日离结构收尾日可搭配补量的天数"),
    /**
     * SYS0204005 SKU可月底补量或是临近结构收尾可搭配补量的排产分类
     */
    BOOST_PRODUCTION_TYPE_VALUE("SYS0204005", "SKU可月底补量或是临近结构收尾可搭配补量的排产分类"),
    /**
     * SYS0204006 单位天，SKU符合SYS0204005时，SKU收尾日离月底可补量的天数
     */
    MAX_BOOST_DAY("SYS0204006", "单位天，SKU符合SYS0204005时，SKU收尾日离月底可补量的天数"),
    /**
     * SYS0204009 结构需求量最小排产天数，<该值则不进行结构排产
     */
    MIN_PRODUCTION_DAYS("SYS0204009", "结构需求量最小排产天数，<该值则不进行结构排产"),
    /**
     * SYS0204010 结构需求量满足SYS0204009时，结构上机最短天数
     */
    MIN_ALLOCATION_DAYS("SYS0204010", "结构需求量满足SYS0204009时，结构上机最短天数"),
    /**
     * SYS0204012 常规结构实单最低供应硫化机台数，<该值则结构需要强制收尾
     */
    NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER("SYS0204012", "常规结构实单最低供应硫化机台数，<该值则结构需要强制收尾"),
    /**
     * SYS0101001 超期常规储备排产月数
     */
    OVERDUE_REGULAR("SYS0101001", "单位：月,超期常规储备排产月数"),
    /**
     * SYS0101002 超期周期储备排产月数
     */
    OVERDUE_CYCLE("SYS0101002", "单位：月,超期周期储备排产月数"),
    /**
     * SYS0101003 超期胎预警月份数
     */
    OVERDUE_TIRE_WARNING("SYS0101003", "单位：月，超期胎预警月份数"),
    /**
     * SYS0103002 月均销量统计之前月份的历史销售记录
     */
    MONTH_AVG_HIS_SUB_MONTH("SYS0103002", "单位：月，月均销量统计之前月份的历史销售记录"),
    /**
     * SYS0206001 单台成型机的月度生产计划锁定期天数
     */
    SINGLE_CX_MACHINE_LOCK_DAYS("SYS0206001", "单台成型机的月度生产计划锁定期天数"),
    /**
     * SYS0206002 多台成型机的月度生产计划锁定期天数
     */
    MULTI_CX_MACHINE_LOCK_DAYS("SYS0206002", "多台成型机的月度生产计划锁定期天数"),

    /**
     * SYS0206003 试制、量试SKU单日上限的数量
     */
    TRIAL_SKU_SINGLE_DAY_QTY_UP_LIMIT("SYS0206003", "试制、量试SKU单日上限的数量"),

    /**
     * SYS0206004 试制、量试SKU在结构起产日是否允许排产
     */
    TRIAL_SKU_STRUCT_START_DAY_IS_PRODUCTION("SYS0206004", "试制、量试SKU在结构起产日是否允许排产"),

    /**
     * SYS0206005 试制、量试SKU在周日是否允许排产
     */
    TRIAL_SKU_SUNDAY_IS_PRODUCTION("SYS0206005", "试制、量试SKU在周日是否允许排产"),
    /**
     * SYS0206007 结构内调整减量，提前收尾可搭配排产的天数
     */
    STRUCTURE_ADJUST_PRE_CLOSE_DAY("SYS0206007", "结构内调整减量，提前收尾可搭配排产的天数"),
    /**
     * SYS0205001 单位：台，续作Sku排产硫化机台数超过该值时，需要考虑降膜排产
     */
    DEDUCT_MOULD_MIN_LH_MACHINE_COUNT("SYS0205001", "单位：台，续作Sku排产硫化机台数超过该值时，需要考虑降膜排产"),
    /**
     * SYS0205002 单位：台，续作Sku排产硫化机台数超过该值时，需要降到SYS0205003的值
     */
    FIRST_NEAR_DEAD_LINE_DAY("SYS0205002", "单位：台，续作Sku排产硫化机台数超过该值时，需要降到SYS0205003的值"),
    /**
     * SYS0205003 单位：台，续作Sku排产硫化机台数超过SYS0205002时，需要降到该值
     */
    FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT("SYS0205003", "单位：台，续作Sku排产硫化机台数超过SYS0205002时，需要降到该值"),
    /**
     * SYS0205004 单位：台，续作Sku排产硫化机台数超过该值时，需要降到SYS0205005的值
     */
    SECOND_NEAR_DEAD_LINE_DAY("SYS0205004", "单位：台，续作Sku排产硫化机台数超过该值时，需要降到SYS0205005的值"),
    /**
     * SYS0205005 单位：台，续作Sku排产硫化机台数超过SYS0205004时，需要降到该值
     */
    SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT("SYS0205005", "单位：台，续作Sku排产硫化机台数超过SYS0205004时，需要降到该值"),
    /**
     * SYS0205006 单位：台，续作Sku排产硫化机台数超过该值时，需要降到SYS0205007的值
     */
    LAST_NEAR_DEAD_LINE_DAY("SYS0205006", "单位：台，续作Sku排产硫化机台数超过该值时，需要降到SYS0205007的值"),
    /**
     * SYS0205007 单位：台，续作Sku排产硫化机台数超过SYS0205006时，需要降到该值
     */
    LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT("SYS0205007", "单位：台，续作Sku排产硫化机台数超过SYS0205006时，需要降到该值"),

    /**
     * SYS0208001 净需求计划日产能，计算区域总产能 = 净需求计划日产能 * 当月天数，作用是重新调整每个区域产能
     */
    NET_REQUIREMENT_DAY_CAPACITY("SYS0208001", "净需求计划日产能，计算区域总产能 = 净需求计划日产能 * 当月天数，作用是重新调整每个区域产能"),
    /**
     * SYS0202006 EUDR开始的年周号
     */
    EUDR_REQUIRE("SYS0202006", "EUDR开始的年周号"),

    /**
     * SYS0206006 周程滚动调整日
     */
    WEEK_ROLL_ADJUST_DATE("SYS0206006", "周程滚动调整日"),
    /**
     * SYS0207001 参与Sku排产竞争的优先级高的Sku列表个数
     */
    HEIGHT_PRIORITY_SKU_LIST_COUNT("SYS0207001", "参与Sku排产竞争的优先级高的Sku列表个数"),
    /**
     * SYS0209003 APS通用班制
     */
    APS_GENERAL_SHIFT("SYS0209003", "SKU双模日硫化量，倒算班产使用"),

    /**
     * SYS0209004 单位：台，成型机在结构切换时，首日应减少硫化机台数
     */
    CHANGE_STRUCT_DEC_LH_MACHINES("SYS0209004", "单位：台，成型机在结构切换时，首日应减少硫化机台数"),

    /**
     * SYS0209005 参与排产的特殊原材料编码，多个以,分隔
     */
    SPECIAL_MATERIAL_CODE("SYS0209005", "参与排产的特殊原材料编码，多个以,分隔"),
    /**
     * SYS0209006 正式排产，结构优先级重新排序日
     */
    FORMAL_RESET_SORT_DAY("SYS0209006", "正式排产，结构优先级重新排序日"),
    /**
     * SYS0209007 单位：天，跨结构模具分配比例在该值内完成调整
     */
    MOLD_ALLOCATION_RATIO_CYCLE("SYS0209007", "单位：天，跨结构模具分配比例在该值内完成调整"),
    /**
     * SYS0209008 结构日分配多台成型机时，需要额外增加的硫化机台数配置
     */
    CX_LH_RATIO_EXTRA("SYS0209008","结构日分配多台成型机时，需要额外增加的硫化机台数配置");

    private final String code;
    private final String name;

    MonthPlanEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
