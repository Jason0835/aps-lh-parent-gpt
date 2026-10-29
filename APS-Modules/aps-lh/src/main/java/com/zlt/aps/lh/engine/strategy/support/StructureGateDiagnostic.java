package com.zlt.aps.lh.engine.strategy.support;

import lombok.Getter;

/**
 * 结构附加门禁的只读原因分类，保持原中文失败原因及其优先级。
 *
 * @author APS
 * @date 2026/10/23
 */
@Getter
public final class StructureGateDiagnostic {

    /**
     * 原失败原因。
     */
    private final String reason;
    /**
     * 明确区分等待与数量拒绝。
     */
    private final StructureLimitRejectionCode code;

    /**
     * 冻结原拒绝及分类。
     *
     * @param reason 原中文拒绝原因
     * @param code 实际门禁分类
     */
    public StructureGateDiagnostic(String reason, StructureLimitRejectionCode code) {
        this.reason = reason;
        this.code = code;
    }
}
