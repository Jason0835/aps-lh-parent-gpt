package com.zlt.aps.lh.engine.strategy.support;

/** 续作降模选择原因，只解释选机，不代表机台已释放。 */
public enum ReductionReason {
    /** 前日交替计划日期命中本次T或T+1。 */
    PREVIOUS_ALTERNATE_PLAN,
    /** 正常排序中的优先续作前缀保护。 */
    CONTINUATION_PREFIX,
    /** 模具可供其他有计划SKU共用。 */
    MOULD_COMMONALITY,
    /** 窗口后五天无计划时优先释放大尺寸机台。 */
    FUTURE_NO_PLAN_LARGE_MACHINE,
    /** 已加载的有效清洗计划。 */
    CLEANING_PLAN,
    /** 胶囊左右侧最大使用次数。 */
    CAPSULE_USAGE,
    /** 业务因素相同时按编码确定顺序。 */
    MACHINE_CODE_FALLBACK
}
