package com.zlt.aps.common.core.utils;

/**
 * TM/TC 排程结果查询和导出的统一排序 SQL。
 *
 * <p>TM 与 TC 的六个班次业务排序键一致，列表和专用导出的稳定辅助字段因展示字段不同而保留各自口径；
 * 所有排序表达式集中在本类维护，调用方只负责选择对应场景的常量。</p>
 */
public final class TmTcScheduleResultOrderSql {

    /**
     * 机台及六个班次的共同业务排序，序号为空时排在后面。
     */
    private static final String BUSINESS_ORDER_PREFIX =
            "MACHINE_CODE ASC, CLASS1_SEQUENCE IS NULL ASC, CLASS1_SEQUENCE ASC, "
                    + "CLASS2_SEQUENCE IS NULL ASC, CLASS2_SEQUENCE ASC, "
                    + "CLASS3_SEQUENCE IS NULL ASC, CLASS3_SEQUENCE ASC, "
                    + "CLASS4_SEQUENCE IS NULL ASC, CLASS4_SEQUENCE ASC, "
                    + "CLASS5_SEQUENCE IS NULL ASC, CLASS5_SEQUENCE ASC, "
                    + "CLASS6_SEQUENCE IS NULL ASC, CLASS6_SEQUENCE ASC";

    /**
     * 胎面结果列表和通用导出的默认排序表达式，不包含 ORDER BY 关键字。
     */
    public static final String TM_DEFAULT_ORDER_BY = BUSINESS_ORDER_PREFIX
            + ", TREAD_CODE ASC, ID ASC";

    /**
     * 胎面专用导出的排序表达式，包含 ORDER BY 关键字。
     */
    public static final String TM_EXPORT_ORDER_BY = "ORDER BY " + BUSINESS_ORDER_PREFIX
            + ", TREAD_CODE ASC, ID ASC";

    /**
     * 胎侧看板和传统列表的默认排序表达式，不包含 ORDER BY 关键字。
     */
    public static final String TC_DEFAULT_ORDER_BY = BUSINESS_ORDER_PREFIX
            + ", SIDEWALL_CODE ASC, ID ASC";

    /**
     * 胎侧看板查询使用的排序表达式，包含 ORDER BY 关键字。
     */
    public static final String TC_BOARD_ORDER_BY = "ORDER BY " + TC_DEFAULT_ORDER_BY;

    /**
     * 胎侧专用导出的排序表达式，包含 ORDER BY 关键字。
     */
    public static final String TC_EXPORT_ORDER_BY = "ORDER BY " + BUSINESS_ORDER_PREFIX
            + ", SIDEWALL_CODE ASC, ID ASC";

    /**
     * 工具类不允许实例化。
     */
    private TmTcScheduleResultOrderSql() {
    }
}
