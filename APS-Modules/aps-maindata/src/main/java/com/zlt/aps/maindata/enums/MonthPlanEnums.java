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
     * SYS1215 周转天数(全局参数) ,用于供应链订单池备库上限的计算
     */
    TURN_OVER_DAYS("SYS1215", "周转天数(全局参数) ,用于供应链订单池备库上限的计算"),
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
     * SYS0204001 SKU总净需求量<=该值，SKU一次性排产
     */
    SUM_PRODUCTION_QTY("SYS0204001", "SKU总净需求量<=该值，SKU一次性排产"),
    /**
     * SYS0204002 SKU高优先级与总净需求量的差值<=该值，SKU一次性排产
     */
    HEIGHT_DIFF_QTY("SYS0204002", "SKU高优先级与总净需求量的差值<=该值，SKU一次性排产"),
    /**
     * SYS0204003 模具二次上机的间隔时间
     */
    MOULD_SECOND_PRODUCTION("SYS0204003", "模具二次上机的间隔时间"),
    /**
     * SYS0204004 SKU排产分类为主销、常规，月均销量>=该值时，月底1-2天可直接补量
     */
    BOOST_AVERAGE_VALUE("SYS0204004", "SKU排产分类为主销、常规，月均销量>=该值时，月底1-2天可直接补量"),
    /**
     * SYS0204005 SKU符合SYS0204004时，收尾日离月底可补量的天数
     */
    MAX_BOOST_DAY("SYS0204005", "SKU符合SYS0204004时，收尾日离月底可补量的天数"),
    /**
     * SYS0204006 结构需求量最小排产天数，<该值则不进行结构排产
     */
    MIN_PRODUCTION_DAYS("SYS0204006", "结构需求量最小排产天数，<该值则不进行结构排产"),
    /**
     * SYS0204007 结构需求量满足SYS0204006时，结构上机最短天数
     */
    MIN_ALLOCATION_DAYS("SYS0204007", "结构需求量满足SYS0204006时，结构上机最短天数"),
    /**
     * SYS0204008 常规结构实单最低供应硫化机台数，<该值则结构需要强制收尾
     */
    NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER("SYS0204008", "常规结构实单最低供应硫化机台数，<该值则结构需要强制收尾");

    private final String code;
    private final String name;

    MonthPlanEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

}
