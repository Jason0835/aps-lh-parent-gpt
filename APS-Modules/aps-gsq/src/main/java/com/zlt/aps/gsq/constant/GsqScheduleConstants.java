package com.zlt.aps.gsq.constant;

/**
 * 钢丝圈排程共享常量
 *
 * <p>集中管理钢丝圈自动滚动更新、班次配置等场景使用的常量，
 * 避免在多个 Service / Controller / Job 类中重复硬编码。</p>
 *
 * @author APS
 */
public final class GsqScheduleConstants {

    // ==================== 班次相关 ====================

    /** 钢丝圈宽表支持的最大班次序号（6班次制） */
    public static final int GSQ_MAX_SHIFT_ORDER = 6;

    // ==================== 任务编号前缀 ====================

    /** 自动滚动任务编号前缀 */
    public static final String ROLLING_TASK_ID_PREFIX = "GSQ-ROLL-";

    /** 人工操作异步任务编号前缀 */
    public static final String OPERATION_TASK_ID_PREFIX = "GSQ-OPER-";

    // ==================== 任务阶段编码 ====================

    /** 自动滚动计算阶段编码 */
    public static final String ROLLING_STAGE_CALCULATING = "ROLLING_CALCULATING";

    /** 自动滚动持久化阶段编码 */
    public static final String ROLLING_STAGE_PERSISTING = "ROLLING_PERSISTING";

    /** 自动滚动完成阶段编码 */
    public static final String ROLLING_STAGE_COMPLETE = "COMPLETE";

    // ==================== 任务创建者标识 ====================

    /** 自动滚动任务创建者 */
    public static final String AUTO_ROLLING_OPERATOR = "AUTO_ROLLING";

    // ==================== 错误摘要长度 ====================

    /** 错误摘要最大长度（数据库字段长度限制） */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    // ==================== 班次业务日期偏移 ====================

    /**
     * class1至class6相对排程日期（SCHEDULE_DATE，即D+1）的MES业务日偏移。
     *
     * <p>6班次制对应关系：</p>
     * <ul>
     *   <li>1班：D日 中班（offset = -1）</li>
     *   <li>2班：D+1日 夜班（offset = 0）</li>
     *   <li>3班：D+1日 早班（offset = 0）</li>
     *   <li>4班：D+1日 中班（offset = 0）</li>
     *   <li>5班：D+2日 夜班（offset = 1）</li>
     *   <li>6班：D+2日 早班（offset = 1）</li>
     * </ul>
     */
    public static final int[] MES_BUSINESS_DATE_OFFSETS = {-1, 0, 0, 0, 1, 1};

    // ==================== 班次字段访问模板（对齐胎圈 TqScheduleConstants） ====================

    /** 班次计划量字段名模板（配合 String.format 使用，动态访问 class1~6PlanQty）。 */
    public static final String SHIFT_PLAN_QTY_FIELD_TEMPLATE = "class%dPlanQty";

    /** 班次顺序字段名模板。 */
    public static final String SHIFT_SEQUENCE_FIELD_TEMPLATE = "class%dSequence";

    /** 班次完成量字段名模板。 */
    public static final String SHIFT_FINISH_QTY_FIELD_TEMPLATE = "class%dFinishQty";

    /** 班次开始时间字段名模板。 */
    public static final String SHIFT_START_TIME_FIELD_TEMPLATE = "class%dStartTime";

    /** 班次结束时间字段名模板。 */
    public static final String SHIFT_END_TIME_FIELD_TEMPLATE = "class%dEndTime";

    /** 班次任务状态字段名模板。 */
    public static final String SHIFT_TASK_STATUS_FIELD_TEMPLATE = "class%dTaskStatus";

    /** 班次原因分析字段名模板。 */
    public static final String SHIFT_ANALYSIS_FIELD_TEMPLATE = "class%dAnalysis";

    // ==================== 人工操作门面锁键 ====================

    /**
     * 人工操作门面分布式锁前缀。
     *
     * <p>对齐胎圈 TqManualOperationFacade 锁键口径，组合格式：
     * {@code GSQ_SCHEDULE:OPER_LOCK:{factoryCode}:{scheduleDate}:{machineCode}}。</p>
     */
    public static final String MANUAL_OPERATION_LOCK_KEY_PREFIX = "GSQ_SCHEDULE:OPER_LOCK:";

    // ==================== 调度日志操作类型（对齐 ApsConstant 与 GsqDispatcherLog.operType 字典） ====================

    /**
     * 调度日志操作类型：4-自动滚动。
     *
     * <p>对齐胎圈 {@code TqScheduleConstants.DISPATCHER_OPER_ROLLING}；
     * 其他操作类型（0-转机台、1-调量、2-插单、3-删除）复用 {@code ApsConstant.DISPATCHER_OPER_*}
     * 以保持与 {@code GsqDispatcherLog.operType} 字典一致。</p>
     */
    public static final String DISPATCHER_OPER_AUTO_ROLLING = "4";

    /**
     * 工具类不允许实例化。
     */
    private GsqScheduleConstants() {
    }
}
