package com.zlt.aps.lh.engine.strategy.support;

/**
 * 新增排产单个业务日内的资源竞争阶段。
 *
 * <p>阶段顺序是按天驱动改造的核心约束：已上机延续优先于正常资源竞争，
 * 当天普通新增与当天到期的续作加机台在正常资源竞争阶段按 S4.5 全局顺序统一选机，
 * 正常阶段冻结资源后再执行提前生产。遗留枚举暂时保留但没有调用入口，枚举顺序不得用于替代
 * 明确的阶段调用顺序，避免后续增加枚举值时无意改变业务优先级。</p>
 *
 * @author APS
 */
public enum DailySchedulePhase {

    /** 延续前一业务日已经上机的新增 SKU */
    CARRY_OVER,
    /** 当前日普通新增与当前日到期的续作加机台统一竞争资源 */
    NORMAL_RESOURCE_COMPETITION,
    /** 已下线：无未来计划的历史欠产或既有收尾遗留任务，暂留待后续关联代码清理 */
    LEGACY_SHORTAGE_OR_ENDING,
    /** 使用正常阶段完成后的剩余资源执行提前生产 */
    EARLY_PRODUCTION,
    /** 当日状态收口以及窗口末日最终未排处理 */
    FINALIZE
}
