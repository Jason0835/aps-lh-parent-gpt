package com.zlt.aps.tc.api.enums;

/**
 * 胎侧自动排程规则编码枚举。
 */
public enum TcScheduleRuleCodeEnum {

    /** 当日停产需求重分配。 */
    CURRENT_DAY_SHUTDOWN_REDISTRIBUTION("CURRENT_DAY_SHUTDOWN_REDISTRIBUTION", "当日停产需求重分配"),

    /** 未来停产需求重分配。 */
    FUTURE_SHUTDOWN_REDISTRIBUTION("FUTURE_SHUTDOWN_REDISTRIBUTION", "未来停产需求重分配"),

    /** 施工版本匹配及回退。 */
    VERSION_MATCH("VERSION_MATCH", "施工版本匹配及回退"),

    /** 开机阈值调整。 */
    STARTUP_THRESHOLD_ADJUST("STARTUP_THRESHOLD_ADJUST", "开机阈值调整"),

    /** 最晚开机优先。 */
    LATEST_START_PRIORITY("LATEST_START_PRIORITY", "最晚开机优先"),

    /** 未来班次提前填充。 */
    FUTURE_SHIFT_EARLY_FILL("FUTURE_SHIFT_EARLY_FILL", "未来班次提前填充"),

    /** 计划量结转。 */
    PLAN_QTY_CARRYOVER("PLAN_QTY_CARRYOVER", "计划量结转"),

    /** 静态可行但当前班产能不足的顺延。 */
    CAPACITY_BLOCKED_CARRYOVER("CAPACITY_BLOCKED_CARRYOVER", "当前班产能不足顺延"),

    /** 产能溢出拆分。 */
    CAPACITY_OVERFLOW_SPLIT("CAPACITY_OVERFLOW_SPLIT", "产能溢出拆分"),

    /** 新规格提前排程结果。 */
    NEW_SPEC_ADVANCE_RESULT("NEW_SPEC_ADVANCE_RESULT", "新规格提前排程结果"),

    /** 产能溢出未排。 */
    CAPACITY_OVERFLOW_UNPLANNED("CAPACITY_OVERFLOW_UNPLANNED", "产能溢出未排"),

    /** 工装限制未排。 */
    TOOL_LIMIT_UNPLANNED("TOOL_LIMIT_UNPLANNED", "工装限制未排"),

    /** 小胶种连续生产。 */
    SMALL_GLUE_CONTINUOUS("SMALL_GLUE_CONTINUOUS", "小胶种连续生产"),

    /** 机台过滤。 */
    MACHINE_FILTER("MACHINE_FILTER", "机台过滤"),

    /** 机台评分。 */
    MACHINE_SCORE("MACHINE_SCORE", "机台评分"),

    /** 机台分配。 */
    MACHINE_ASSIGN("MACHINE_ASSIGN", "机台分配"),

    /** 新规格识别。 */
    NEW_SPEC_DETECT("NEW_SPEC_DETECT", "新规格识别"),

    /** 新规格提前窗口。 */
    NEW_SPEC_ADVANCE_WINDOW("NEW_SPEC_ADVANCE_WINDOW", "新规格提前窗口"),

    /** 实验规格识别。 */
    EXPERIMENT_SPEC_DETECT("EXPERIMENT_SPEC_DETECT", "实验规格识别"),

    /** 实验规格计划量。 */
    EXPERIMENT_SPEC_PLAN_QTY("EXPERIMENT_SPEC_PLAN_QTY", "实验规格计划量"),

    /** 需求量计算。 */
    DEMAND_QTY_CALC("DEMAND_QTY_CALC", "需求量计算"),

    /** 计划量计算。 */
    PLAN_QTY_CALC("PLAN_QTY_CALC", "计划量计算"),

    /** 任务排序。 */
    TASK_SORT("TASK_SORT", "任务排序");

    private final String code;

    private final String desc;

    TcScheduleRuleCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取规则编码。
     *
     * @return 规则编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取规则说明。
     *
     * @return 规则说明
     */
    public String getDesc() {
        return desc;
    }
}
