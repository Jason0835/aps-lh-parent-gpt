package com.zlt.aps.tc.api.enums;

/**
 * 胎侧排程步骤枚举。
 *
 * <p>用于统一自动排程模板、过程日志和解释快照中的步骤编码，不直接控制流程。</p>
 */
public enum TcScheduleStepEnum {

    /** 初始化批次、追踪号、参数和基础资料 */
    BOOTSTRAP("BOOTSTRAP", "初始化"),

    /** 库存预测和供应时长测算 */
    INVENTORY_PREDICT("INVENTORY_PREDICT", "库存预测"),

    /** 需求量和计划量计算 */
    PLAN_CALC("PLAN_CALC", "计划量计算"),

    /** 待排任务排序 */
    TASK_SORT("TASK_SORT", "待排任务排序"),

    /** 候选机台过滤和分配 */
    MACHINE_ASSIGN("MACHINE_ASSIGN", "机台分配"),

    /** 产能均衡和顺序计算 */
    CAPACITY_BALANCE("CAPACITY_BALANCE", "产能均衡"),

    /** 解释快照构建及原子落库，对外统一为第六阶段 */
    SNAPSHOT_BUILD("SNAPSHOT_PERSIST", "快照与落库"),

    /** 结果与解释落库 */
    PERSIST("PERSIST", "结果落库");

    private final String code;

    private final String desc;

    TcScheduleStepEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
