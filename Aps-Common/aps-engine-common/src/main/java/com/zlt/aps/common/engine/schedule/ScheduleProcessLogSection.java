package com.zlt.aps.common.engine.schedule;

/**
 * 自动排程班次过程日志分区。
 *
 * <p>枚举顺序即为日志正文中同一班次的固定展示顺序。</p>
 */
public enum ScheduleProcessLogSection {

    /** 库存滚动分区。 */
    INVENTORY_ROLLING("库存滚动"),

    /** 计划量计算分区。 */
    PLAN_QTY_CALCULATION("计划量计算"),

    /** 机台过滤与选择分区。 */
    MACHINE_SELECTION("机台过滤与选择"),

    /** 选机后的工装预校验和计划定稿分区。 */
    TOOL_LIMIT("工装预校验及计划定稿"),

    /** 选机后的机台产能扣减分区。 */
    CAPACITY_DEDUCTION("产能扣减"),

    /** 实际承接任务后的工装账本结算分区。 */
    TOOL_LEDGER_SETTLEMENT("工装账本结算"),

    /** 未排任务分区。 */
    UNPLANNED_TASK("未排任务");

    /** 分区中文名称。 */
    private final String displayName;

    /**
     * 创建过程日志分区。
     *
     * @param displayName 分区中文名称
     */
    ScheduleProcessLogSection(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取分区中文名称。
     *
     * @return 分区中文名称
     */
    public String getDisplayName() {
        return this.displayName;
    }
}
