package com.zlt.aps.factory.constant;

/**
 * 排产算法常量定义类
 *
 * @author ZLT
 * @date 20251203
 */
public class ProductionConstant {
    /**
     * 月份起始天数
     * 1
     */
    public final static Integer MONTH_START_DAY = 1;
    /**
     * 月份最大天数
     * 31
     */
    public final static Integer MONTH_MAX_DAY = 31;
    /**
     * 非自然月最大天数值
     */
    public final static int NO_NATURAL_MONTH_MAX_VALUE = 28;
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
     * 1 单模规格即SAP与模具关系中只有一副模具
     */
    public final static Integer SINGLE_MOULD_PRODUCT_CODE = 1;
    /**
     * 1 单模台
     */
    public final static Integer SINGLE_MOULD_QTY = 1;
    /**
     * 分配后，补整台的值
     */
    public final static double REPAIR_WHOLE = 0.9d;
    /**
     * 百分比 100
     */
    public final static Integer PERCENTAGE = 100;
    /**
     * 2 双模台
     */
    public final static Integer DOUBLE_MOULD_QTY = 2;
    /**
     * 物料-库位类别毛利润匹配的key值格式
     */
    public final static String PRODUCT_PROFIT_KEY_FORMAT = "%s|*|%s";
    /**
     * 字符分隔符
     */
    public final static String PRODUCT_SPLIT = "\\|\\*\\|";
    /**
     * 默认-利润优先值 100
     */
    public final static int DEFAULT_PROFIT = 100;
    /**
     * 双模排产数量 2
     */
    public final static int DOUBLE_MOULD_PRODUCTION = 2;
    /**
     * Y 值
     */
    public final static String YES_VALUE = "Y";
    /**
     * 达不到排产条件
     */
    public final static Long SKIP_PRODUCTION = Long.MIN_VALUE;
    /**
     * 偶数 2
     */
    public final static int EVEN_NUMBER = 2;
    /**
     * 偶数增加的损耗量 2
     */
    public final static Long ADD_LOSS_QTY_EVEN_NUMBER = 2L;

    /**
     * 偶数增加的损耗量 3
     */
    public final static Long ADD_LOSS_QTY_ODD_NUMBER = 3L;
}
