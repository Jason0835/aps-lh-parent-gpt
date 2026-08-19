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

    /** 工装限制分区。 */
    TOOL_LIMIT("工装限制"),

    /** 机台选择和产能扣减分区。 */
    MACHINE_SELECTION_CAPACITY("机台选择 产能扣减"),

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
