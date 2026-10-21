package com.zlt.aps.lh.engine.strategy.support;
/**
 * 单个清洗来源事件的判定状态，容量窗口被过滤不代表事件取消。
 *
 * @author APS
 * @since 2026/10/19
 */
public enum CleaningPlanStatus {
    /**
     * 当前事件可以安排。
     */
    SCHEDULED,
    /**
     * 初始化因条件或额度不满足而跳过。
     */
    SKIPPED,
    /**
     * 已提交结果确认来源物料收尾，取消清洗。
     */
    ENDING_CANCELLED,
    /**
     * 已提交正量正规换模取消清洗。
     */
    MOULD_CHANGE_CANCELLED,
    /**
     * 指定组合尚无专用规则，由原调用方保持既有准入范围。
     */
    UNSUPPORTED
}
