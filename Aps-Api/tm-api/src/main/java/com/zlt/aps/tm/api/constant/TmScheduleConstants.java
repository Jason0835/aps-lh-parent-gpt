package com.zlt.aps.tm.api.constant;

/**
 * 胎面排程共享常量。
 *
 * @author APS
 */
public final class TmScheduleConstants {

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

    /**
     * 工具类不允许实例化。
     */
    private TmScheduleConstants() {
    }
}
