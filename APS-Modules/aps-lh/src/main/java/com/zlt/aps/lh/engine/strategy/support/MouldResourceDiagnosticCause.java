package com.zlt.aps.lh.engine.strategy.support;

/**
 * 模具分配的只读诊断子因，独立于原跳过原因和分配行为。
 *
 * @author APS
 * @date 2026/10/23
 */
public enum MouldResourceDiagnosticCause {
    /**
     * 有效模具数量不足。
     */
    QUANTITY_SHORTAGE,
    /**
     * 有效模具仍被其他机台占用。
     */
    OCCUPIED,
    /**
     * 尚未到已提交的释放时刻。
     */
    RELEASE_NOT_REACHED,
    /**
     * 已有关系明确记录未来到货。
     */
    NOT_ARRIVED,
    /**
     * 台账缺失且无到货可用依据。
     */
    MODEL_INFO_MISSING,
    /**
     * 台账明确禁用。
     */
    DISABLED,
    /**
     * 指定模具不属于该SKU关系。
     */
    RELATION_MISMATCH,
    /**
     * 指定模具组数量或有效组合不符。
     */
    EXACT_COMBINATION_MISMATCH
}
