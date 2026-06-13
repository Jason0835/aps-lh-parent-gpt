package com.zlt.aps.cd90.engine.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 直裁自动排程参数编码。
 *
 * <p>自动排程统一使用t_cd90_params.PARAM_CODE读取参数，禁止依赖参数名称或备注字段。</p>
 */
public final class Cd90AutoScheduleParamCode {

    public static final String CRIMP_LENGTH = "SYS0701011";
    public static final String DEMAND_CALC_MODE = "SYS0701012";
    public static final String DEMAND_WINDOW = "SYS0701013";
    public static final String SCHEDULE_WINDOW = "SYS0701014";
    public static final String STOCK_GUARANTEE_SHIFTS = "SYS0701015";
    public static final String MAX_ROLL_CHANGE_PER_SHIFT = "SYS0701016";
    public static final String MIN_START_QTY = "SYS0701017";
    public static final String MACHINE_PRIORITY = "SYS0701018";
    public static final String MAX_TIME_4SHIFT = "SYS0701019";
    public static final String STOP_LOOKAHEAD_DAYS = "SYS0701020";
    public static final String RESTART_STOCK_THRESHOLD = "SYS0701021";
    public static final String ROLL_TOTAL_COUNT = "SYS0701022";
    public static final String SPEC_CHANGE_MINUTES = "SYS0701023";
    public static final String TASK_TIMEOUT_MINUTES = "SYS0701024";
    public static final String AUTO_SCHEDULE_CRON = "SYS0701025";

    public static final List<String> ALL_CODES = Collections.unmodifiableList(Arrays.asList(
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
            SPEC_CHANGE_MINUTES,
            TASK_TIMEOUT_MINUTES,
            AUTO_SCHEDULE_CRON
    ));

    private Cd90AutoScheduleParamCode() {
    }
}
