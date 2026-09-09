package com.zlt.aps.lh.engine.strategy.support;

/** 多机台硫化余量收尾处理结果；只有不适用允许进入原逐日降模。 */
public enum ContinuationEndingAllocationStatus {
    /** 单机、非余量收尾、胎胚硬目标或同物料状态专用链。 */
    NOT_APPLICABLE,
    /** 整组守恒分摊与错峰优化通过。 */
    OPTIMIZED,
    /** 不存在更优合法错峰或有界搜索未完成，保留已校验的连续基线。 */
    BASELINE_RETAINED,
    /** 缺少真实数据或候选未通过硬校验，不提交候选且不回旧逐日降模。 */
    DATA_INCOMPLETE
}
