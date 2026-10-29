package com.zlt.aps.lh.engine.strategy.support;

/**
 * 结构准入稳定诊断编码，不依赖中文原因文本识别数量门禁。
 *
 * @author APS
 * @date 2026/10/23
 */
public enum StructureLimitRejectionCode {
    /**
     * 通过或不适用。
     */
    NONE,
    /**
     * 真实机台数量上限。
     */
    MACHINE_COUNT_LIMIT,
    /**
     * 班次或必要身份缺失。
     */
    MISSING_SHIFT,
    /**
     * 结构统计索引缺失。
     */
    MISSING_INDEX,
    /**
     * 无有效结构计划。
     */
    NO_STRUCTURE_PLAN,
    /**
     * 等待时间门禁。
     */
    WAITING_TIME,
    /**
     * 规则首班无法判定或提前占用。
     */
    FIRST_SHIFT_UNAVAILABLE,
    /**
     * 其他门禁，不能推测为数量不足。
     */
    OTHER_GATE
}
