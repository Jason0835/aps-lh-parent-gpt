package com.zlt.aps.lh.engine.strategy.support;

/**
 * 机台反向选择 SKU 的通用匹配层级。
 *
 * <p>数值越小优先级越高。同一层级内不追加全局评分，继续保持日期池内
 * {@code DefaultSkuPriorityStrategy} 已确定的稳定 SKU 顺序。</p>
 *
 * @author APS
 */
public enum MachineSkuMatchLevel {

    /** 当前机台在机胎胚与目标 SKU 相同 */
    SAME_EMBRYO(0, "同胎胚"),
    /** 当前机台实际绑定模具的模壳与预分配目标模具相同 */
    SAME_MOULD_SHELL(1, "同模壳"),
    /** 当前机台前规格与目标 SKU 规格相同 */
    SAME_SPEC(2, "同规格"),
    /** 当前机台胶囊与目标 SKU 可共用 */
    SAME_CAPSULE_GROUP(3, "胶囊共用"),
    /** 当前机台前规格英寸与目标 SKU 相同 */
    SAME_INCH(4, "同英寸"),
    /** 仅满足硬约束，按相近英寸层级参与竞争 */
    NEAR_INCH(5, "相近英寸");

    private final int priority;
    private final String description;

    MachineSkuMatchLevel(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }
}
