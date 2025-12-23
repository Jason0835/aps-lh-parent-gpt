package com.tlt.aps.constant;

/**
 * 分厂常量定义
 *
 * @author ZLT
 * @date 20250220
 */
public class FactoryConstant {
    /**
     * 月起始天数常量
     * 1
     */
    public final static Integer MONTH_START_DAY = 1;
    /**
     * 月最大天数
     * 31
     */
    public final static Integer MONTH_MAX_DAY = 31;
    /**
     * 小时对应的秒数 3600秒
     */
    public final static Integer HOUR_SECOND = 3600;
    /**
     * 分钟对应的秒数 60秒
     */
    public final static Integer MINUTE_SECOND = 60;
    /**
     * 一天最大的小时数
     */
    public final static Integer MAX_DAY_HOURS = 24;
    /**
     * 2 多层胎体布标记
     */
    public final static Integer MULTILAYER_TIRE_FABRIC = 2;

    /**
     * 日期前缀
     */
    public static final String DAY_FIELD = "DAY_";

    /**
     * Y 值
     */
    public final static String YES_VALUE = "Y";
    /**
     * 非自然月最大天数值
     */
    public final static int NO_NATURAL_MONTH_MAX_VALUE = 28;
    /**
     * -1 无限制值
     */
    public final static Integer NO_LIMIT_VALUE = -1;
    //==================================系统参数===============================================
    /**
     * 参数名：SYS001 计划调整需在操作日延后(天)才可调整
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_ADJUST_DELAY_DAYS = "SYS001";
    /**
     * 参数名：SYS002	夏季切换月份
     * 参数类型：整型
     * 参数值：5
     */
    public static String SYS_PARAM_SUMMER_MONTH = "SYS002";
    /**
     * 参数名：SYS003	月份周期，起始天数 <=0则按自然月
     * 参数类型：整型
     * 参数值：-1
     */
    public static String SYS_PARAM_MONTH_CYCLE_START_DAY = "SYS003";
    /**
     * 参数名：SYS004	冬季切换月份
     * 参数类型：整型
     * 参数值：11
     */
    public static String SYS_PARAM_WINTER_MONTH = "SYS004";
    /**
     * 参数名：SYS005	备货时，近几个月需要提前多少个月开始获取
     * 参数类型：整型
     * 参数值：0
     */
    public static String SYS_PARAM_STOCK_UP_MONTH = "SYS005";
    /**
     * 参数名：SYS006	一次法成型切换规格扣减的产能
     * 参数类型：整型
     * 参数值：40
     */
    public static String SYS_PARAM_ONE_SUBTRACT_QTY = "SYS006";
    /**
     * 参数名：SYS005	备货时，近几个月需要提前多少个月开始获取
     * 参数类型：数型
     * 参数值：2.91
     */
    public static String SYS_PARAM_EXPORT_OEM_BRAND_OEE = "SYS007";
    /**
     * 参数名：SYS008 空成型默认切换的规格次数
     * 参数类型：整型
     * 参数值：3
     */
    public static String SYS_PARAM_MOLDING_MACHINE_CHANGE_COUNT = "SYS008";
    /**
     * 参数名：SYS009	1天的工时上限（小时）
     * 参数类型：数型
     * 参数值：
     */
    public static String SYS_PARAM_DAY_WORK_HOURS = "SYS009";
    /**
     * 参数名：SYS008 月度排产单硫化机预产能均值
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_VULCANIZATION_MACHINE_AVG_QTY = "SYS010";
    /**
     * 参数名：SYS011 寸口产能分配时，额外计算的切换规格次数
     * 参数类型：整型
     * 参数值：1
     */
    public static String SYS_PARAM_ADDITIONAL_COUNT = "SYS011";
    /**
     * 参数名：SYS012 月度排产每日排产最大规格数
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_DAY_MAX_PRODUCT_COUNT = "SYS012";
    /**
     * 参数名：SYS012 月度排产特殊日排产量最大量控制
     * 参数类型：字符
     * 参数值：天,数量;天,数量;天,数量
     */
    public static String SYS_SPECIAL_DAY_LIMIT_COUNT = "SYS031";
    /**
     * 参数名：SYS013 月度排产每日新增最大规格数
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_DAY_ADDED_MAX_PRODUCT_COUNT = "SYS013";
    /**
     * 参数名：SYS014	二次法成型切换规格扣减的产能
     * 参数类型：整型
     * 参数值：20
     */
    public static String SYS_PARAM_TWO_SUBTRACT_QTY = "SYS014";
    /**
     * 参数名：SYS032	单条硫化增加时间
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_INTERVAL_TIME_OF_EMBRYO_EXCHANGE = "SYS032";
    /**
     * 参数名：SYS016 换规格需扣减的时间 单位小时
     * 参数类型：数型
     * 参数值：
     */
    public static String SYS_CHANGE_PRODUCT_SUB_HOURS = "SYS016";

    /**
     * 参数名：SYS017 寸口切换最低需求天数
     * 参数类型：数型
     * 参数值：7
     */
    public static String SYS_CHANGE_SIZE_ALLOCATION_MIN_DAYS = "SYS017";
    /**
     * 参数名：SYS037 成型产能最小产能分配天数
     * 参数类型：数型
     * 参数值：7
     */
    public static String SYS_MIN_CAPACITY_DAYS = "SYS037";
    /**
     * 参数名：SYS018 生成销售需求计划是否加入超欠产
     * 参数类型：字符串
     * 参数值：
     */
    public static String SYS_PARAM_IS_ADD_SHORT = "SYS018";

    /**
     * 参数名：SYS018 寸口产能分配时，是否开启大寸口挤占在产寸口需求
     * 参数类型：字符串
     * 参数值：
     */
    public static String SYS_OPEN_CROWD_OUT = "SYS019";
    /**
     * 参数名：SYS020 获取同规格计划优先排的最大计划量
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_SAME_PRODUCT_LIMIT = "SYS020";

    /**
     * 参数名：SYS021 获取同寸口计划优先排的最大计划量
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_SAME_PRO_SIZE_LIMIT = "SYS021";

    /**
     * 参数名：SYS022 24小时试制量试的硫化时间计算产能
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_INFORMAL_CONSTRUCTION = "SYS022";
    /**
     * 参数名：SYS024 月度计划排产是否考虑共用生胎排产
     * 参数类型：字符串
     * 参数值：Y
     */
    public static String SYS_PARAM_IS_SAME_CONSTRUCTION = "SYS024";
    /**
     * 参数名：SYS025	连续排产（天）后需洗模
     * 参数类型：整型
     * 参数值：
     */
    public static String SYS_PARAM_CONNECTION_SCHEDULING_DAYS = "SYS025";
    /**
     * 参数名：SYS026 洗模日需要扣减的产能时长
     * 参数类型：数型
     * 参数值：
     */
    public static String SYS_PARAM_CLEANING_DAY_LEFT_OVER_HOURS = "SYS026";

    /**
     * 默认工厂常量
     */
    public static String DEFAULT_FACTORY_CODE = "116";
    /**
     * 系统参数：上调控制水位控制-近12个月销售总量
     * 参数类型：整型
     * 参数值：
     */
    public static String SALE_TOTAL_QTY = "SYS036";
    /**
     * 系统参数：默认上调控制水位，最小批量导入时使用
     * 参数类型：整型
     */
    public static String DEFAULT_UP_WATER_LEVEL = "SYS040";

    /**
     * 排产周期 -7~31
     */
    public static Integer[] PRODUCTION_CYCLE = {-1, -2, -3, -4, -5, -6, -7, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    /**
     * 系统参数：库容阀值
     */
    public static String STORAGE_CAPACITY_THRESHOLD = "SYS035";
    /**
     * 参数名：SYS030 生成月度需求计划时，是否自动生成备货计划
     * 参数类型：字符串
     * 参数值：Y
     */
    public static String SYS_PARAM_IS_AUTO_CREATE_STOCK_UP = "SYS030";
    /**
     * 参数名：SYS028 生产月度需求计划时，不进行备货的品牌(主要为外贸贴牌品牌)
     * 参数类型：字符串
     * 参数值：(主要为外贸贴牌品牌)
     */
    public static String SYS_PARAM_EXPORT_OEM_BRAND = "SYS028";
    /**
     * 参数名：SYS029 外销贴牌品牌
     * 参数类型：字符串
     * 参数值：外销贴牌品牌
     */
    public static String SYS_PARAM_FOREIGN_OEM_BRAND = "SYS029";
    /**
     * 参数名：SYS034 生产月度需求计划时，内销备货是否保持一致
     * 参数类型：字符串
     * 参数值：N
     */
    public static String SYS_PARAM_DOMESTIC_STOCK_UP_TYPE = "SYS034";
    /**
     * 参数名：SYS038 排产月度计划时，续作规格是否开启满月排产
     * 参数类型：字符串
     * 参数值：N
     */
    public static String SYS_PARAM_CONTINUE_FULL_MOON_PRODUCTION = "SYS038";
    /**
     * 参数名：SYS041 月度计划续作满月满排规格，后续可超产排的天数
     * 参数类型：数值
     * 参数值：5
     */
    public static String SYS_PARAM_FULL_MONTH_DAY = "SYS041";
    /**
     * 参数名：SYS042 月度计划续作规格的月平均销量达到该值时，才进行满月排产
     * 参数类型：数值
     * 参数值：4500
     */
    public static String SYS_PARAM_MONTH_AVERAGE_VALUE = "SYS042";
    /**
     * 参数名：SYS044 生产月度需求计划时，是否包含无订单备货计划需求
     * 参数类型：字符串
     * 参数值：N
     */
    public static String SYS_PARAM_OPEN_NO_SUBMIT_STOCK_UP = "SYS044";

    /**
     * 参数名：SYS045 拼模排产合模压力差值范围
     * 参数类型：数值
     * 参数值：100
     */
    public static String SYS_PARAM_MOULD_CLAMPING_PRESSURE_DIFF = "SYS045";

    /**
     * 参数名：SYS046 拼模排产时，两个规格硫化时间的差值范围,单位秒
     * 参数类型：数值
     * 参数值：30s
     */
    public static String SYS_PARAM_CURING_TIME_DIFF = "SYS046";

    /**
     * 参数名：SYS047 拼模排产时，两个规格排产量之间的差值，单位条
     * 参数类型：数值
     * 参数值：400
     */
    public static String SYS_PARAM_PLAN_QTY_DIFF = "SYS047";

    /**
     * 参数名：SYS048 量小规格拼模排产时，规格的排产量在该值以下才考虑拼模排产
     * 参数类型：数值
     * 参数值：1000
     */
    public static String SYS_PARAM_ASSEMBLING_MOULD_PRODUCTION_QTY = "SYS048";
    /**
     * 参数名：SYS015 月度计划排产是否开启按寸口由大到小排产模式
     * 参数类型：字符串
     * 参数值：N
     */
    public static String SYS_PARAM_OPEN_PRO_SIZE_PRODUCTION_MODEL = "SYS015";
    /**
     * 参数名：SYS023 寸口产能分配，18寸二次法大鼓限定产能
     * 参数类型：字符串
     * 参数值：
     */
    public static String SYS_BIG_DRUM_CAPACITY_VALUE = "SYS023";
    /**
     * 参数名：SYS027 寸口产能分配，18寸二次法最大成型产能限制数
     * 参数类型：数值
     * 参数值：4
     */
    public static String SYS_MAX_LIMIT_CAPACITY = "SYS027";
    /**
     * 参数名：SYS033 寸口产能分配，20寸1次法最大成型产能限制数
     * 参数类型：数值
     * 参数值：1
     */
    public static String SYS_MAX_20_ONE_LIMIT_CAPACITY = "SYS033";
}
