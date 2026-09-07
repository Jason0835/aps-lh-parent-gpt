package com.zlt.aps.common.engine.schedule.engine;

/**
 * TM/TC 自动排程计划量算法公共策略接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface ISchedulePlanQtyStrategy<C extends ScheduleContextModel<T, ?, ?, ?, ?>,
        T extends ScheduleTaskDraftModel> {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 计算计划量。
     *
     * @param draft   待排任务草稿
     * @param context 排程上下文
     * @return 计划量计算结果
     */
    SchedulePlanQtyResultModel calculate(T draft, C context);
}
