package com.zlt.aps.cd90.engine.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 直裁自动排程参数编码。
 *
 * <p>自动排程统一使用t_cd90_params.PARAM_CODE读取参数。</p>
 */
public final class Cd90AutoScheduleParamCode {

    /** 各班计划量均分阈值：非收尾规格净需求量超过该值时，先按净需求量除以2再计算计划量。 */
    public static final String EQUAL_SHARE_THRESHOLD = "SYS0701007";
    /** 工装卷曲米数（大卷卷曲长度）：单个工装卷一个完整大卷的长度（米），用于米数需求转大卷个数。 */
    public static final String CRIMP_LENGTH = "SYS0701011";
    /** 需求计算方式：AVERAGE（按班次平均值）或 SUM（各班需求相加）。 */
    public static final String DEMAND_CALC_MODE = "SYS0701012";
    /** 成型需求窗口班数：计算需排产量时向前取多少个班的成型计划。 */
    public static final String DEMAND_WINDOW = "SYS0701013";
    /** 直裁排程输出窗口班数：一次排程输出多少班的排产结果，取值 1~6。 */
    public static final String SCHEDULE_WINDOW = "SYS0701014";
    /** 库存保证班数：库存能保证供应的班数大于此值时，当前直裁班次无需排该规格。 */
    public static final String STOCK_GUARANTEE_SHIFTS = "SYS0701015";
    /** 每班大卷切换提醒次数：切换超过此数时仅记录告警日志，不限制排产。 */
    public static final String MAX_ROLL_CHANGE_PER_SHIFT = "SYS0701016";
    /** 最小起排量（米）：非收尾规格排产量低于此值时自动补足。 */
    public static final String MIN_START_QTY = "SYS0701017";
    /** 机台优先顺序：逗号分隔的机台编码列表，按此顺序优先分配排产。 */
    public static final String MACHINE_PRIORITY = "SYS0701018";
    /** 连续4班同规格上机次数上限：限制同一规格在连续4个班次内的出现次数，防止频繁切换。 */
    public static final String MAX_TIME_4SHIFT = "SYS0701019";
    /** 停产前瞻天数：扫描生产日历未来 N 天，提前识别停产/恢复生产事件。 */
    public static final String STOP_LOOKAHEAD_DAYS = "SYS0701020";
    /** 实际复产后的备库上限（米）：复产场景下限制最大备库量。 */
    public static final String RESTART_STOCK_THRESHOLD = "SYS0701021";
    /** 工装总数：工厂拥有的直裁工装（大卷轴）总数，必填且 > 0。 */
    public static final String ROLL_TOTAL_COUNT = "SYS0701022";
    /** 同大卷不同直裁规格切换耗时（分钟）：从班产能中扣减。 */
    public static final String SAME_ROLL_DIFF_SPEC_CHANGE_MINUTES = "SYS0701023";
    /** 旧字段兼容别名，新代码使用SAME_ROLL_DIFF_SPEC_CHANGE_MINUTES。 */
    public static final String SPEC_CHANGE_MINUTES = SAME_ROLL_DIFF_SPEC_CHANGE_MINUTES;
    /** 自动排程任务超时分钟数：超时后主动中止排程，同时作为异常任务心跳判定阈值。 */
    public static final String TASK_TIMEOUT_MINUTES = "SYS0701024";
    /** 自动排程定时表达式：按服务器时区触发，为各启用工厂生成下一天排程任务。 */
    public static final String AUTO_SCHEDULE_CRON = "SYS0701025";
    /** 不同大卷同直裁规格切换耗时（分钟）：保持规格但切换大卷时的班产能扣减。 */
    public static final String DIFF_ROLL_SAME_SPEC_CHANGE_MINUTES = "SYS0701026";
    /** 不同大卷不同直裁规格切换耗时（分钟）：同时切换大卷和规格时的班产能扣减。 */
    public static final String DIFF_ROLL_DIFF_SPEC_CHANGE_MINUTES = "SYS0701027";
    /** 上机即耗尽特殊大卷代码列表：逗号分隔的大卷代码，命中后该大卷上机即完全耗尽。 */
    public static final String SPECIAL_ROLL_USE_UP_CODES = "SYS0701028";
    /** 特殊大卷额外前瞻班数：预留给特殊大卷额外扩展的成型班数。 */
    public static final String SPECIAL_ROLL_LOOKAHEAD_SHIFTS = "SYS0701029";
    /** 特殊大卷额外备库上限：预留给特殊大卷额外扩展的备库数量上限。 */
    public static final String SPECIAL_ROLL_EXTRA_STOCK_LIMIT = "SYS0701030";
    /** 非收尾部分排最小车数：库排不足时，实际分配车数达到该值才允许提交部分排。 */
    public static final String PARTIAL_MIN_VEHICLE_COUNT = "SYS0701031";
    /** 通用损耗率兜底（百分比）：t_cd90_loss_setting 四层优先级均未命中时使用，对应参数 LOSS_RATE。 */
    public static final String LOSS_RATE = "SYS0701003";

    public static final List<String> ALL_CODES = Collections.unmodifiableList(Arrays.asList(
            EQUAL_SHARE_THRESHOLD,
            CRIMP_LENGTH,
            DEMAND_CALC_MODE,
            DEMAND_WINDOW,
            SCHEDULE_WINDOW,
            STOCK_GUARANTEE_SHIFTS,
            MAX_ROLL_CHANGE_PER_SHIFT,
            MIN_START_QTY,
            MACHINE_PRIORITY,
            MAX_TIME_4SHIFT,
            STOP_LOOKAHEAD_DAYS,
            RESTART_STOCK_THRESHOLD,
            ROLL_TOTAL_COUNT,
            SAME_ROLL_DIFF_SPEC_CHANGE_MINUTES,
            TASK_TIMEOUT_MINUTES,
            AUTO_SCHEDULE_CRON,
            DIFF_ROLL_SAME_SPEC_CHANGE_MINUTES,
            DIFF_ROLL_DIFF_SPEC_CHANGE_MINUTES,
            SPECIAL_ROLL_USE_UP_CODES,
            SPECIAL_ROLL_LOOKAHEAD_SHIFTS,
            SPECIAL_ROLL_EXTRA_STOCK_LIMIT,
            PARTIAL_MIN_VEHICLE_COUNT,
            LOSS_RATE
    ));

    private Cd90AutoScheduleParamCode() {
    }
}
