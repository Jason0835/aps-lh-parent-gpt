package com.zlt.aps.cx.constant;

/**
 * 成型排程公共常量 - 被多个类引用的常量统一定义于此，消除重复。
 *
 * <p>仅本类使用的私有常量保持在各自类中，不提取到此。
 *
 * @author APS Team
 */
public final class ScheduleConstants {

    private ScheduleConstants() {}

    // ==================== 时间常量 ====================

    /** 一天总秒数 */
    public static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** 每小时秒数 */
    public static final int SECONDS_PER_HOUR = 3600;

    // ==================== 业务默认值 ====================

    /** 默认整车容量（条） */
    public static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认机台最大胎胚种类数 */
    public static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /** 默认机台最大硫化机数（配比缺失时单台最多生产的硫化机数） */
    public static final int DEFAULT_MAX_LH_MACHINE_QTY = 10;

    /** 默认日产能（用于续作/试制机台产能估算） */
    public static final int DEFAULT_DAILY_CAPACITY = 1200;

    /** 默认排程天数 */
    public static final int DEFAULT_SCHEDULE_DAYS = 3;

    // ==================== 切换耗时 ====================

    /** 同英寸切换耗时（小时） */
    public static final int DEFAULT_SAME_INCH_SWITCH_HOURS = 2;

    /** 不同英寸切换耗时（小时） */
    public static final int DEFAULT_DIFF_INCH_SWITCH_HOURS = 8;

    // ==================== 均衡分配 ====================

    /** 胚胎编码阈值：大于此值的胎胚通常候选机台较少（业务启发式） */
    public static final int EMBRYO_CODE_HIGH_THRESHOLD = 215103130;

    // ==================== 参数编码 ====================

    /** 强制保留历史任务参数编码 */
    public static final String PARAM_FORCE_KEEP_HISTORY = "SYS04070003";

    /** 同英寸切换耗时参数编码 */
    public static final String PARAM_SAME_INCH_SWITCH_HOURS = "SYS04020004";

    /** 不同英寸切换耗时参数编码 */
    public static final String PARAM_DIFF_INCH_SWITCH_HOURS = "SYS04020005";
}
