package com.zlt.aps.tm.api.enums;

/**
 * 胎面未排原因枚举。
 *
 * <p>用于统一未排任务解释中的原因编码。骨架阶段只定义稳定编码，具体命中逻辑由后续规则实现。</p>
 */
public enum TmUnplannedReasonEnum {

    /** 缺少需求数据 */
    DEMAND_MISSING("DEMAND_MISSING", "缺少需求数据"),

    /** 缺少参数或参数无默认值 */
    PARAM_MISSING("PARAM_MISSING", "缺少参数"),

    /** 无可用机台 */
    NO_AVAILABLE_MACHINE("NO_AVAILABLE_MACHINE", "无可用机台"),

    /** 口型板不匹配 */
    MOUTH_PLATE_NOT_MATCH("MOUTH_PLATE_NOT_MATCH", "口型板不匹配"),

    /** 胶料机台关系不允许 */
    GLUE_MACHINE_NOT_ALLOWED("GLUE_MACHINE_NOT_ALLOWED", "胶料机台关系不允许"),

    /** 产能不足 */
    CAPACITY_NOT_ENOUGH("CAPACITY_NOT_ENOUGH", "产能不足"),

    /** 工装不足 */
    TOOL_NOT_ENOUGH("TOOL_NOT_ENOUGH", "工装不足"),

    /** 规则冲突 */
    RULE_CONFLICT("RULE_CONFLICT", "规则冲突"),

    /** 胎面停产且无可分配班次 */
    TM_SHUTDOWN_NO_AVAILABLE_SHIFT("TM_SHUTDOWN_NO_AVAILABLE_SHIFT", "胎面停产且无可分配班次，成型需求无法重分配"),

    /** 待业务确认 */
    WAIT_CONFIRM("WAIT_CONFIRM", "待业务确认");

    private final String code;

    private final String desc;

    TmUnplannedReasonEnum(String code, String desc) {
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
