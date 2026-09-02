package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 计划量公共引擎领域策略端口。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 * @param <F> 库存预测类型
 * @param <P> 计划量策略类型
 * @param <D> 需求量策略类型
 */
public interface PlanCalculationPolicy<C, T extends ScheduleTaskDraftModel,
        F extends ScheduleInventoryForecast, G extends SchedulePlanTaskGroup<T>, P, D> {

    void validateContext(C context);

    T copyDerivedTask(T sourceTask);

    void applyTailDecision(T aggregateTask, List<T> sourceTaskList);

    String validatePlanGroup(C context, String planGroupKey, List<T> sourceTaskList);

    RuntimeException planGroupConflictException(List<String> conflictMessageList);

    void enrichAggregateTask(T aggregateTask, List<T> sourceTaskList);

    G createPlanTaskGroup();

    String resolvePlanStrategyCode(C context);

    P resolvePlanStrategy(String strategyCode);

    String resolveDemandAlgorithmCode(C context);

    D resolveDemandStrategy(String algorithmCode);

    BigDecimal initializeGlobalAvailableToolQty(C context, Map<String, F> stockForecastMap);

    void prepareShiftDemandAndSupply(C context, List<T> shiftTaskList, Map<String, F> stockForecastMap,
                                     Map<String, BigDecimal> remainingStockMap, D demandStrategy,
                                     String demandAlgorithmCode);

    void sortPlanCalcShiftTasks(C context, List<T> shiftTaskList);

    BigDecimal calculatePlanQtyForTask(C context, T task, Map<String, F> stockForecastMap,
                                       Map<String, BigDecimal> remainingStockMap, BigDecimal remainingToolQty,
                                       P planStrategy, String planStrategyCode, String demandAlgorithmCode);
}
