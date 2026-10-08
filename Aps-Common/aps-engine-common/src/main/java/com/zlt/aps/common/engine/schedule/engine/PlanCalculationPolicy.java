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

    /**
     * 清理领域专有的计划量调整分量。
     *
     * @param task 已命中库存覆盖的来源任务
     */
    default void clearAdditionalPlanAdjustments(T task) {
        // 默认无领域专有调整分量。
    }

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

    /**
     * 在当前班次需求准备完成后写入领域计划量入口的运行态判定。
     *
     * @param context       排程上下文
     * @param shiftTaskList 当前班次任务
     */
    default void beforeCalculatePlanQty(C context, List<T> shiftTaskList) {
        // 默认无额外计划量入口判定。
    }

    void sortPlanCalcShiftTasks(C context, List<T> shiftTaskList);

    /**
     * 记录逐班计划计算排序轮次；默认空实现兼容未接入解释表的领域策略。
     *
     * @param context        排程上下文
     * @param beforeOrder    排序前任务集合
     * @param afterOrder     排序后任务集合
     * @param sortStartIndex 本轮首个计划计算序号
     * @param sortEndIndex   本轮最后计划计算序号
     */
    default void recordPlanCalcSortSnapshot(C context, List<T> beforeOrder, List<T> afterOrder,
                                            int sortStartIndex, int sortEndIndex) {
        // 默认不记录排序快照。
    }

    BigDecimal calculatePlanQtyForTask(C context, T task, Map<String, F> stockForecastMap,
                                       Map<String, BigDecimal> remainingStockMap, BigDecimal remainingToolQty,
                                       P planStrategy, String planStrategyCode, String demandAlgorithmCode);
}
