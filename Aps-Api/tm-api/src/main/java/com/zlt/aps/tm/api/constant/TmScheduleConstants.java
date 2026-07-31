package com.zlt.aps.tm.api.constant;

/**
 * 胎面排程共享常量。
 *
 * @author APS
 */
public final class TmScheduleConstants {

    /** 需求量算法参数键。 */
    public static final String PARAM_ALGORITHM_SWITCH = "TM_ALGORITHM_SWITCH";

    /** 最低库存班数参数键。 */
    public static final String PARAM_MIN_STOCK_CLASS = "TM_MIN_STOCK_CLASS";

    /** 最小开机量参数键。 */
    public static final String PARAM_MIN_START_QTY = "TM_MIN_START_QTY";

    /** 默认卷长参数键。 */
    public static final String PARAM_DEFAULT_CURL_LENGTH = "TM_DEFAULT_CURL_LENGTH";

    /** 工装总量参数键。 */
    public static final String PARAM_TOOL_TOTAL_QTY = "TM_TOOL_TOTAL_QTY";

    /** 停产需求重分配开关参数键。 */
    public static final String PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED = "TM_SHUTDOWN_REDISTRIBUTION_ENABLED";

    /** 计划量策略参数键。 */
    public static final String PARAM_PLAN_QTY_STRATEGY = "TM_PLAN_QTY_STRATEGY";

    /** 任务排序策略参数键。 */
    public static final String PARAM_TASK_SORT_STRATEGY = "TM_TASK_SORT_STRATEGY";

    /** 新规格回看天数参数键。 */
    public static final String PARAM_NEW_SPEC_LOOKBACK_DAYS = "TM_NEW_SPEC_LOOKBACK_DAYS";

    /** 新规格提前班次数参数键。 */
    public static final String PARAM_NEW_SPEC_ADVANCE_SHIFT_COUNT = "TM_NEW_SPEC_ADVANCE_SHIFT_COUNT";

    /** 实验规格回看天数参数键。 */
    public static final String PARAM_EXPERIMENT_SPEC_LOOKBACK_DAYS = "TM_EXPERIMENT_SPEC_LOOKBACK_DAYS";

    /** 实验规格计划量参数键。 */
    public static final String PARAM_EXPERIMENT_SPEC_PLAN_QTY = "TM_EXPERIMENT_SPEC_PLAN_QTY";

    /** 成型需求偏移班次数参数键。 */
    public static final String PARAM_FORMING_SHIFT_OFFSET = "TM_FORMING_SHIFT_OFFSET";

    /** 班次表头起始日期相对排程日期的偏移天数参数键。 */
    public static final String PARAM_SHIFT_DATE_START_OFFSET = "TM_SHIFT_DATE_START_OFFSET";

    /** 小胶种编码参数键。 */
    public static final String PARAM_SMALL_GLUE_CODES = "TM_SMALL_GLUE_CODES";

    /** 版本匹配模式参数键。 */
    public static final String PARAM_VERSION_MATCH_MODE = "TM_VERSION_MATCH_MODE";

    /** 机台过滤策略参数键。 */
    public static final String PARAM_MACHINE_FILTER_STRATEGY = "TM_MACHINE_FILTER_STRATEGY";

    /** 机台评分策略参数键。 */
    public static final String PARAM_MACHINE_SCORE_STRATEGY = "TM_MACHINE_SCORE_STRATEGY";

    /** 链任务优先策略参数键。 */
    public static final String PARAM_CHAIN_TASK_PRIORITY_STRATEGY = "TM_CHAIN_TASK_PRIORITY_STRATEGY";

    /** 自动滚动开关参数键。 */
    public static final String PARAM_ROLLING_ENABLED = "TM_ROLLING_ENABLED";

    /** 自动滚动提前触发分钟数参数键。 */
    public static final String PARAM_ROLLING_LEAD_MINUTES = "TM_ROLLING_LEAD_MINUTES";

    /** 自动滚动上修阈值班数参数键。 */
    public static final String PARAM_ROLLING_UP_THRESHOLD = "TM_ROLLING_UP_THRESHOLD";

    /** 自动滚动下修阈值班数参数键。 */
    public static final String PARAM_ROLLING_DOWN_THRESHOLD = "TM_ROLLING_DOWN_THRESHOLD";

    /** 自动滚动下修目标班数参数键。 */
    public static final String PARAM_ROLLING_DOWN_TARGET = "TM_ROLLING_DOWN_TARGET";

    /** 自动滚动需求窗口班次数参数键。 */
    public static final String PARAM_ROLLING_SHIFT_COUNT = "TM_ROLLING_SHIFT_COUNT";

    /** 工序停放小时数参数键。 */
    public static final String PARAM_PROCESS_STANDING_HOURS = "TM_PROCESS_STANDING_HOURS";

    /** 默认生产速度参数键。 */
    public static final String PARAM_DEFAULT_PRODUCTION_SPEED = "TM_DEFAULT_PRODUCTION_SPEED";

    /** 停产检查窗口参数键。 */
    public static final String PARAM_SHUTDOWN_CHECK_WINDOW = "TM_SHUTDOWN_CHECK_WINDOW";

    /** 开班阈值参数键。 */
    public static final String PARAM_OPEN_SHIFT_THRESHOLD = "TM_OPEN_SHIFT_THRESHOLD";

    /** 换规格时间参数键。 */
    public static final String PARAM_SPEC_CHANGE_MINUTES = "TM_SPEC_CHANGE_MINUTES";

    /** 换胶时间参数键，已废弃，自动排程不再按时间折算换胶产能。 */
    @Deprecated
    public static final String PARAM_GLUE_CHANGE_MINUTES = "TM_GLUE_CHANGE_MINUTES";

    /** 主胶料切换固定产能扣减参数键。 */
    public static final String PARAM_GLUE_CHANGE_CAPACITY_DEDUCT = "TM_GLUE_CHANGE_CAPACITY_DEDUCT";

    /** 整车率参数键，控制工厂级工装实际可用比例。 */
    public static final String PARAM_VEHICLE_RATE = "TM_VEHICLE_RATE";

    /** 缺库存快照处理策略参数键。 */
    public static final String PARAM_STOCK_MISSING_POLICY = "TM_STOCK_MISSING_POLICY";

    /** 机台过滤规则执行顺序参数键。 */
    public static final String PARAM_FILTER_RULE_ORDER = "TM_FILTER_RULE_ORDER";

    /** 单项机台过滤规则启停参数前缀。 */
    public static final String PARAM_FILTER_RULE_ENABLED_PREFIX = "TM_FILTER_RULE_";

    /** 单项机台过滤规则启停参数后缀。 */
    public static final String PARAM_FILTER_RULE_ENABLED_SUFFIX = "_ENABLED";

    /** 剩余产能适配评分权重参数键。 */
    public static final String PARAM_SCORE_WEIGHT_REMAIN_CAP = "TM_SCORE_WEIGHT_REMAIN_CAP";

    /** 主胶料连续评分权重参数键。 */
    public static final String PARAM_SCORE_WEIGHT_GLUE_CONT = "TM_SCORE_WEIGHT_GLUE_CONT";

    /** 基部胶相似评分权重参数键。 */
    public static final String PARAM_SCORE_WEIGHT_BASE_GLUE = "TM_SCORE_WEIGHT_BASE_GLUE";

    /** 口型连续评分权重参数键。 */
    public static final String PARAM_SCORE_WEIGHT_MOUTH_CONT = "TM_SCORE_WEIGHT_MOUTH_CONT";

    /** 切换成本评分权重参数键。 */
    public static final String PARAM_SCORE_WEIGHT_SWITCH_COST = "TM_SCORE_WEIGHT_SWITCH_COST";

    /** 定点生产评分权重参数键。 */
    public static final String PARAM_SCORE_WEIGHT_FIXED_MACHINE = "TM_SCORE_WEIGHT_FIXED_MACHINE";

    /** 需求量算法默认编码。 */
    public static final String DEFAULT_ALGORITHM_SWITCH = "1";

    /** 最低库存班数默认值。 */
    public static final String DEFAULT_MIN_STOCK_CLASS = "1";

    /** 最小开机量默认值。 */
    public static final String DEFAULT_MIN_START_QTY = "0";

    /** 默认卷长缺省值，单位米/条。 */
    public static final String DEFAULT_CURL_LENGTH = "0";

    /** 工装总量默认值。 */
    public static final String DEFAULT_TOOL_TOTAL_QTY = "0";

    /** 停产需求重分配开关默认值。 */
    public static final String DEFAULT_SHUTDOWN_REDISTRIBUTION_ENABLED = "1";

    /** 通用排程策略默认编码。 */
    public static final String DEFAULT_SCHEDULE_STRATEGY = "DEFAULT";

    /** 新规格回看天数默认值。 */
    public static final String DEFAULT_NEW_SPEC_LOOKBACK_DAYS = "7";

    /** 新规格提前班次数默认值。 */
    public static final String DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT = "2";

    /** 实验规格回看天数默认值。 */
    public static final String DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS = "5";

    /** 实验规格计划量默认值。 */
    public static final String DEFAULT_EXPERIMENT_SPEC_PLAN_QTY = "30";

    /** 成型需求偏移班次数默认值。 */
    public static final String DEFAULT_FORMING_SHIFT_OFFSET = "2";

    /** 班次表头起始日期相对排程日期的默认偏移天数。 */
    public static final int DEFAULT_SHIFT_DATE_START_OFFSET = -2;

    /** 小胶种编码默认值。 */
    public static final String DEFAULT_SMALL_GLUE_CODES = "";

    /** 版本匹配模式默认编码。 */
    public static final String DEFAULT_VERSION_MATCH_MODE = "RECIPE";

    /** 链任务优先策略默认编码。 */
    public static final String DEFAULT_CHAIN_TASK_PRIORITY_STRATEGY = "CONTINUITY_FIRST";

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

    /** 自动滚动分布式锁前缀。 */
    public static final String ROLLING_LOCK_KEY_PREFIX = "TM_SCHEDULE:ROLLING_LOCK:";

    /** 自动滚动运行键前缀。 */
    public static final String ROLLING_RUN_KEY_PREFIX = "ROLLING:";

    /** 自动滚动调度日志操作类型。 */
    public static final String DISPATCHER_OPER_ROLLING = "4";

    /** 工序停放小时数默认值。 */
    public static final String DEFAULT_PROCESS_STANDING_HOURS = "0";

    /** 默认生产速度缺省值。 */
    public static final String DEFAULT_PRODUCTION_SPEED = "0";

    /** 停产检查窗口默认值。 */
    public static final String DEFAULT_SHUTDOWN_CHECK_WINDOW = "3";

    /** 开班阈值默认值。 */
    public static final String DEFAULT_OPEN_SHIFT_THRESHOLD = "1";

    /** 换规格时间默认值。 */
    public static final String DEFAULT_SPEC_CHANGE_MINUTES = "0";

    /** 换胶时间默认值，已废弃。 */
    @Deprecated
    public static final String DEFAULT_GLUE_CHANGE_MINUTES = "0";

    /** 主胶料切换固定产能扣减默认值，单位米。 */
    public static final String DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT = "200";

    /** 整车率默认值。 */
    public static final String DEFAULT_VEHICLE_RATE = "1";

    /** 缺库存快照默认按零继续排程。 */
    public static final String DEFAULT_STOCK_MISSING_POLICY = "ZERO";

    /** 默认机台过滤规则链顺序。 */
    public static final String DEFAULT_FILTER_RULE_ORDER =
            "MACHINE_STATUS,REMAIN_CAPACITY,MOUTH_PLATE,GLUE_MACHINE,FIXED_MACHINE,EXCLUDE_FIXED";

    /** 默认启用单项机台过滤规则。 */
    public static final String DEFAULT_FILTER_RULE_ENABLED = "1";

    /** 剩余产能适配评分默认权重。 */
    public static final String DEFAULT_SCORE_WEIGHT_REMAIN_CAP = "10";

    /** 主胶料连续评分默认权重。 */
    public static final String DEFAULT_SCORE_WEIGHT_GLUE_CONT = "10";

    /** 基部胶相似评分默认权重。 */
    public static final String DEFAULT_SCORE_WEIGHT_BASE_GLUE = "8";

    /** 口型连续评分默认权重。 */
    public static final String DEFAULT_SCORE_WEIGHT_MOUTH_CONT = "10";

    /** 切换成本评分默认权重。 */
    public static final String DEFAULT_SCORE_WEIGHT_SWITCH_COST = "10";

    /** 定点生产评分默认权重。 */
    public static final String DEFAULT_SCORE_WEIGHT_FIXED_MACHINE = "10";

    /** 自动排程基础资料缓存键前缀。 */
    public static final String BASE_DATA_CACHE_KEY_PREFIX = "aps:tm:autoSchedule:baseData:";

    /** 自动排程基础资料缓存有效分钟数。 */
    public static final long BASE_DATA_CACHE_TTL_MINUTES = 5L;

    /** 自动排程日志前缀。 */
    public static final String AUTO_PLAN_LOG_PREFIX = "[TM_AUTO_PLAN]";

    /** 自动排程批次号前缀。 */
    public static final String AUTO_PLAN_BATCH_NO_PREFIX = "TM";

    /** 自动排程异步任务编号前缀。 */
    public static final String AUTO_SCHEDULE_TASK_ID_PREFIX = "TM-";

    /** 自动排程请求校验完成阶段编码。 */
    public static final String AUTO_SCHEDULE_STAGE_REQUEST_VALIDATED = "REQUEST_VALIDATED";

    /** 自动排程执行完成阶段编码。 */
    public static final String AUTO_SCHEDULE_STAGE_COMPLETE = "COMPLETE";

    /** 自动排程错误摘要最大长度。 */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    /** 任务排序日志最多保留的业务键数量。 */
    public static final int TASK_ORDER_SUMMARY_LIMIT = 20;

    /** 实验规格固定取首班。 */
    public static final int EXPERIMENT_SPEC_SHIFT_ORDER = 1;

    /** 最低库存班数数值默认值。 */
    public static final int DEFAULT_MIN_STOCK_CLASS_VALUE = 1;

    /** 新规格回看天数数值默认值。 */
    public static final int DEFAULT_NEW_SPEC_LOOKBACK_DAYS_VALUE = 7;

    /** 新规格提前班次数值默认值。 */
    public static final int DEFAULT_NEW_SPEC_ADVANCE_SHIFT_COUNT_VALUE = 2;

    /** 成型需求偏移班次数值默认值。 */
    public static final int DEFAULT_FORMING_SHIFT_OFFSET_VALUE = 2;

    /** 实验规格回看天数数值默认值。 */
    public static final int DEFAULT_EXPERIMENT_SPEC_LOOKBACK_DAYS_VALUE = 5;

    /** 实验规格计划量数值默认值。 */
    public static final int DEFAULT_EXPERIMENT_SPEC_PLAN_QTY_VALUE = 30;

    /** 停产检查窗口数值默认值。 */
    public static final int DEFAULT_SHUTDOWN_CHECK_WINDOW_VALUE = 3;

    /** 停产检查窗口最小值。 */
    public static final int MIN_SHUTDOWN_CHECK_WINDOW = 1;

    /** 停产检查窗口最大值。 */
    public static final int MAX_SHUTDOWN_CHECK_WINDOW = 30;

    /** 需求保护默认班次数。 */
    public static final int DEFAULT_GUARD_SHIFT_COUNT = 2;

    /** 人工插单允许的最小顺序号。 */
    public static final int MIN_INSERT_SEQUENCE = 2;

    /** 一小时包含的秒数。 */
    public static final long SECONDS_PER_HOUR = 3600L;

    /** 一小时包含的分钟数。 */
    public static final long MINUTES_PER_HOUR = 60L;

    /** 一小时包含的毫秒数。 */
    public static final long MILLIS_PER_HOUR = 3600000L;

    /** 自动排程中间计算保留的小数位数。 */
    public static final int DECIMAL_CALCULATION_SCALE = 6;

    /** 无法识别的内部占位编码。 */
    public static final String UNKNOWN_CODE = "UNKNOWN";

    /** 自动排程结果数据来源编码。 */
    public static final String AUTO_SCHEDULE_DATA_SOURCE = "AUTO";

    /** 模板导入排程结果数据来源编码。 */
    public static final String IMPORT_SCHEDULE_DATA_SOURCE = "IMPORT";

    /** 预置机台绑定来源。 */
    public static final String PRESET_MACHINE_BIND_SOURCE = "PRESET_MACHINE";

    /** 溢出任务候选探测后缀。 */
    public static final String OVERFLOW_PROBE_SUFFIX = "PROBE";

    /** 产能限制结转来源。 */
    public static final String CARRYOVER_SOURCE_CAPACITY_LIMIT = "CAPACITY_LIMIT";

    /** 工装限制结转来源。 */
    public static final String CARRYOVER_SOURCE_TOOL_LIMIT = "TOOL_LIMIT";

    /** 同班匹配机台拆分类型。 */
    public static final String SPLIT_TYPE_SAME_SHIFT_MATCHED_MACHINE = "SAME_SHIFT_MATCHED_MACHINE";

    /** 后续班匹配机台拆分类型。 */
    public static final String SPLIT_TYPE_NEXT_SHIFT_MATCHED_MACHINE = "NEXT_SHIFT_MATCHED_MACHINE";

    /** 小胶种绑定机台不存在原因。 */
    public static final String SMALL_GLUE_BOUND_MACHINE_NOT_FOUND = "BOUND_MACHINE_NOT_FOUND";

    /** 默认生产速度非正数跳过原因。 */
    public static final String SKIP_REASON_DEFAULT_PRODUCTION_SPEED_NON_POSITIVE =
            "DEFAULT_PRODUCTION_SPEED_NON_POSITIVE";

    /** 班次开始时间无效跳过原因。 */
    public static final String SKIP_REASON_SHIFT_START_TIME_INVALID = "SHIFT_START_TIME_INVALID";

    /** 无可用停产需求承接班次后缀。 */
    public static final String FUTURE_SHUTDOWN_NO_TARGET_SUFFIX = "NO_TARGET";

    /** 自动追加任务链操作编码。 */
    public static final String CHAIN_OPERATION_AUTO_APPEND = "AUTO_APPEND";

    /** 自动前置任务链操作编码。 */
    public static final String CHAIN_OPERATION_AUTO_PREPEND = "AUTO_PREPEND";

    /** 人工插单任务链操作编码。 */
    public static final String CHAIN_OPERATION_MANUAL_INSERT = "MANUAL_INSERT";

    /** 人工删除任务链操作编码。 */
    public static final String CHAIN_OPERATION_MANUAL_DELETE = "MANUAL_DELETE";

    /** 人工转机任务链操作编码。 */
    public static final String CHAIN_OPERATION_MANUAL_TRANSFER = "MANUAL_TRANSFER";

    /** 人工调量任务链操作编码。 */
    public static final String CHAIN_OPERATION_CHANGE_QTY = "CHANGE_QTY";

    /** 胎面宽表支持的最大班次序号。 */
    public static final int TM_MAX_SHIFT_ORDER = 6;

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

    /** 发布异步任务编号前缀。 */
    public static final String RELEASE_TASK_ID_PREFIX = "TM-REL-";

    /** 发布超时分钟数参数键。 */
    public static final String PARAM_RELEASE_TIMEOUT_MINUTES = "TM_RELEASE_TIMEOUT_MINUTES";

    /** 默认发布超时分钟数。 */
    public static final String DEFAULT_RELEASE_TIMEOUT_MINUTES = "10";

    /** 发布阶段：装配。 */
    public static final String RELEASE_STAGE_ASSEMBLE = "RELEASE_ASSEMBLE";

    /** 发布阶段：下发。 */
    public static final String RELEASE_STAGE_ISSUE = "RELEASE_ISSUE";

    /** 发布阶段：等待反馈。 */
    public static final String RELEASE_STAGE_WAIT_FEEDBACK = "RELEASE_WAIT_FEEDBACK";

    /**
     * 工具类不允许实例化。
     */
    private TmScheduleConstants() {
    }
}
