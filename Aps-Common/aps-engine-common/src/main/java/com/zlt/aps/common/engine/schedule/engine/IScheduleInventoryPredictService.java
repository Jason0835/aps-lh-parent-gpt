package com.zlt.aps.common.engine.schedule.engine;

/**
 * TM/TC 自动排程库存预测步骤公共服务接口。
 *
 * @param <C> 排程上下文类型
 */
public interface IScheduleInventoryPredictService<C extends ScheduleContextModel<?, ?, ?, ?, ?>> {

    /**
     * 执行库存预测。
     *
     * @param context 排程上下文
     */
    void predict(C context);
}
