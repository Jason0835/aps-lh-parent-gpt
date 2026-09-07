package com.zlt.aps.common.engine.enums;

/**
 * 自动排程公共步骤枚举。
 *
 * <p>TM 和 TC 的快照落库边界不同，因此分别保留 SNAPSHOT_BUILD 和 SNAPSHOT_PERSIST。</p>
 */
public enum ScheduleStepEnum {

    /** 初始化批次、追踪号、参数和基础资料。 */
    BOOTSTRAP("BOOTSTRAP", "初始化"),

    /** 库存预测和供应时长测算。 */
    INVENTORY_PREDICT("INVENTORY_PREDICT", "库存预测"),

    /** 需求量和计划量计算。 */
    PLAN_CALC("PLAN_CALC", "计划量计算"),

    /** 待排任务排序。 */
    TASK_SORT("TASK_SORT", "待排任务排序"),

    /** 候选机台过滤和分配。 */
    MACHINE_ASSIGN("MACHINE_ASSIGN", "机台分配"),

    /** 产能均衡和顺序计算。 */
    CAPACITY_BALANCE("CAPACITY_BALANCE", "产能均衡"),

    /** TM 解释快照构建。 */
    SNAPSHOT_BUILD("SNAPSHOT_BUILD", "解释快照构建"),

    /** TC 解释快照构建及原子落库。 */
    SNAPSHOT_PERSIST("SNAPSHOT_PERSIST", "快照与落库"),

    /** 结果与解释落库。 */
    PERSIST("PERSIST", "结果落库");

    private final String code;

    private final String desc;

    ScheduleStepEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取步骤编码。
     *
     * @return 步骤编码
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 获取步骤说明。
     *
     * @return 步骤说明
     */
    public String getDesc() {
        return this.desc;
    }
}
