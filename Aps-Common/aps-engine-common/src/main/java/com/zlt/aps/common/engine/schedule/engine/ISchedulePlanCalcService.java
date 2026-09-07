package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * TM/TC 自动排程计划量计算步骤公共服务接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface ISchedulePlanCalcService<C extends ScheduleContextModel<T, ?, ?, ?, ?>,
        T extends ScheduleTaskDraftModel> {

    /**
     * 执行计划量计算。
     *
     * @param context 排程上下文
     */
    void calculate(C context);

    /**
     * 准备任务聚合、库存快照和工装快照。
     *
     * @param context 排程上下文
     */
    void prepare(C context);

    /**
     * 使用上一班实际关账库存计算当前班需求、供应时长、排序和计划量。
     *
     * @param context         排程上下文
     * @param shiftTaskList   当前班任务
     * @param runtimeStockMap 上一班实际关账库存
     */
    void calculateShiftWithActualStock(C context, List<T> shiftTaskList,
                                       Map<String, BigDecimal> runtimeStockMap);
}
