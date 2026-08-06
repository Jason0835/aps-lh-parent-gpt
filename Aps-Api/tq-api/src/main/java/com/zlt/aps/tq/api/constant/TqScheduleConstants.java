package com.zlt.aps.tq.api.constant;

/**
 * 胎圈排程共享常量。
 *
 * <p>对齐胎面 TmScheduleConstants，承载自动滚动相关的参数键、默认值、锁前缀等常量。</p>
 *
 * @author APS
 */
public final class TqScheduleConstants {

    // ============================================================
    // 自动滚动参数键（与 T_TQ_PARAMS 表 PARAM_CODE 对应）
    // ============================================================

    /** 自动滚动开关参数键。 */
    public static final String PARAM_ROLLING_ENABLED = "TQ_ROLLING_ENABLED";

    /** 自动滚动提前触发分钟数参数键。 */
    public static final String PARAM_ROLLING_LEAD_MINUTES = "TQ_ROLLING_LEAD_MINUTES";

    /** 自动滚动上修阈值班数参数键。 */
    public static final String PARAM_ROLLING_UP_THRESHOLD = "TQ_ROLLING_UP_THRESHOLD";

    /** 自动滚动下修阈值班数参数键。 */
    public static final String PARAM_ROLLING_DOWN_THRESHOLD = "TQ_ROLLING_DOWN_THRESHOLD";

    /** 自动滚动下修目标班数参数键。 */
    public static final String PARAM_ROLLING_DOWN_TARGET = "TQ_ROLLING_DOWN_TARGET";

    /** 自动滚动需求窗口班次数参数键。 */
    public static final String PARAM_ROLLING_SHIFT_COUNT = "TQ_ROLLING_SHIFT_COUNT";

    // ============================================================
    // 自动滚动默认值（与 T_TQ_PARAMS 表 DEFAULT_VALUE 对应）
    // ============================================================

    /** 自动滚动开关默认关闭。 */
    public static final String DEFAULT_ROLLING_ENABLED = "0";

    /** 自动滚动默认提前触发分钟数。 */
    public static final int DEFAULT_ROLLING_LEAD_MINUTES = 30;

    /** 自动滚动默认上修阈值班数。 */
    public static final String DEFAULT_ROLLING_UP_THRESHOLD = "1.0";

    /** 自动滚动默认下修阈值班数。 */
    public static final String DEFAULT_ROLLING_DOWN_THRESHOLD = "3.5";

    /** 自动滚动默认下修目标班数。 */
    public static final String DEFAULT_ROLLING_DOWN_TARGET = "3.0";

    /** 自动滚动默认需求窗口班次数。 */
    public static final int DEFAULT_ROLLING_SHIFT_COUNT = 3;

    // ============================================================
    // 自动滚动运行时键
    // ============================================================

    /** 自动滚动分布式锁前缀。 */
    public static final String ROLLING_LOCK_KEY_PREFIX = "TQ_SCHEDULE:ROLLING_LOCK:";

    /** 自动滚动运行键前缀（Redisson 幂等键）。 */
    public static final String ROLLING_RUN_KEY_PREFIX = "TQ_ROLLING:";

    /** 自动滚动调度日志操作类型。 */
    public static final String DISPATCHER_OPER_ROLLING = "4";

    /** 自动滚动定时任务操作人标识。 */
    public static final String ROLLING_OPERATOR_AUTO = "TQ_ROLLING_JOB";

    // ============================================================
    // 班次字段访问模板（对齐胎面，用于动态字段访问）
    // ============================================================

    /** 胎圈宽表支持的最大班次序号。 */
    public static final int TQ_MAX_SHIFT_ORDER = 6;

    /** 班次计划量字段名模板。 */
    public static final String SHIFT_PLAN_QTY_FIELD_TEMPLATE = "class%dPlanQty";

    /** 班次顺序字段名模板。 */
    public static final String SHIFT_SEQUENCE_FIELD_TEMPLATE = "class%dSequence";

    /** 班次完成量字段名模板。 */
    public static final String SHIFT_FINISH_QTY_FIELD_TEMPLATE = "class%dFinishQty";

    /** 班次开始时间字段名模板。 */
    public static final String SHIFT_START_TIME_FIELD_TEMPLATE = "class%dStartTime";

    /** 班次结束时间字段名模板。 */
    public static final String SHIFT_END_TIME_FIELD_TEMPLATE = "class%dEndTime";

    /** 班次分析字段名模板。 */
    public static final String SHIFT_ANALYSIS_FIELD_TEMPLATE = "class%dAnalysis";

    /** 班次任务状态字段名模板。 */
    public static final String SHIFT_TASK_STATUS_FIELD_TEMPLATE = "class%dTaskStatus";

    // ============================================================
    // 释放状态码（与 TmScheduleResult 对齐）
    // ============================================================

    /** 释放状态：未发布。 */
    public static final String RELEASE_STATUS_NOT_PUBLISHED = "0";

    /** 释放状态：待发布。 */
    public static final String RELEASE_STATUS_PENDING = "1";

    /** 释放状态：发布中。 */
    public static final String RELEASE_STATUS_ISSUING = "2";

    /** 释放状态：已发布。 */
    public static final String RELEASE_STATUS_ISSUED = "3";

    /** 释放状态：已下推。 */
    public static final String RELEASE_STATUS_PUSHED = "4";

    /** 释放状态：已撤销。 */
    public static final String RELEASE_STATUS_REVOKED = "5";

    // ============================================================
    // 通用计算常量
    // ============================================================

    /** 一小时包含的秒数。 */
    public static final long SECONDS_PER_HOUR = 3600L;

    /** 一小时包含的分钟数。 */
    public static final long MINUTES_PER_HOUR = 60L;

    /** 一小时包含的毫秒数。 */
    public static final long MILLIS_PER_HOUR = 3600000L;

    /** 自动排程中间计算保留的小数位数。 */
    public static final int DECIMAL_CALCULATION_SCALE = 6;

    /** 错误消息最大长度。 */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    // ============================================================
    // 导入相关常量（对齐胎面胎侧）
    // ============================================================

    /** 导入排程数据来源标识。 */
    public static final String IMPORT_SCHEDULE_DATA_SOURCE = "IMPORT";

    /** 导入执行锁前缀（Redisson）。 */
    public static final String IMPORT_LOCK_KEY_PREFIX = "TQ_SCHEDULE:IMPORT_LOCK:";

    /**
     * 工具类不允许实例化。
     */
    private TqScheduleConstants() {
    }
}
