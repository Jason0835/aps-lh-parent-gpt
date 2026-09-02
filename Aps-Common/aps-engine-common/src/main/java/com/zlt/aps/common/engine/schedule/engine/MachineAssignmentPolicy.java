package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 机台分配公共引擎领域策略端口。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 */
public interface MachineAssignmentPolicy<C, T extends ScheduleTaskDraftModel, P> {

    void validateContext(C context);

    boolean isInventoryClosedLoopEnabled(C context);

    void recalculateShiftPlans(C context, List<T> shiftTaskList, Map<String, BigDecimal> runtimeStockMap);

    /**
     * 在机台分配开始前建立班次级成型需求快照，供各班次计划量计算前的工装释放使用。
     *
     * @param context 排程上下文
     * @return 按实际排程班次分组且按产品去重的不可变需求快照
     */
    default Map<Integer, List<ScheduleShiftToolDemand>> snapshotShiftToolDemand(C context) {
        return Collections.emptyMap();
    }

    /**
     * 在当前班次计划量计算和机台分配开始前，预处理当班成型消耗对应的工装释放。
     *
     * @param context                    排程上下文
     * @param shiftOrder                 当前班次
     * @param availableToolQtyBeforeShift 当前班次开始前的可用工装数量
     * @param currentShiftDemandList     当前班次按产品去重后的成型需求快照
     * @return 班次计划量计算前的可用工装数量；未启用工装约束时保留传入值
     */
    default BigDecimal prepareShiftToolLedger(C context, Integer shiftOrder,
                                               BigDecimal availableToolQtyBeforeShift,
                                               List<ScheduleShiftToolDemand> currentShiftDemandList) {
        return availableToolQtyBeforeShift;
    }

    String getProductCode(T task);

    P resolvePlanQtyStrategy(C context);

    void recalculateTaskPlanQty(C context, T task, P planQtyStrategy);

    boolean isPresetPlanTask(T task);

    void sortRecalculationTasks(List<T> taskList);

    boolean isMachineAssignmentRequired(T task);

    void prepareTaskOrder(List<T> taskList);

    T selectNextTask(List<T> remainingTaskList, C context, int assignmentSequence);

    void assignSingleTask(T task, C context);

    void fillCurrentShiftIdleCapacity(Integer shiftOrder, Map<Integer, List<T>> shiftTaskMap, C context);

    void closeShiftInventory(C context, Integer shiftOrder, List<T> shiftTaskList,
                             Map<String, BigDecimal> runtimeStockMap);

    /**
     * 结算不需要进入机台分配的任务，保持班前工装释放后的任务账本状态。
     *
     * @param context 排程上下文
     * @param task 不进入机台分配的任务
     */
    default void settleNonAssignedTaskToolLedger(C context, T task) {
        // 默认策略不处理未进入机台分配的任务。
    }

    /**
     * 完成一个班次的机台处理后保留兼容扩展点；班前成型释放和任务生产占用已在前置及任务阶段完成。
     *
     * @param context 排程上下文
     * @param shiftOrder 当前班次
     * @param availableToolQtyBeforeShift 当前班次开始前的可用工装
     * @param nextShiftDemandList 兼容保留的下一班成型需求快照，当前自动排程不使用
     */
    default void settleShiftToolLedger(C context, Integer shiftOrder,
                                       BigDecimal availableToolQtyBeforeShift,
                                       List<ScheduleShiftToolDemand> nextShiftDemandList) {
        // 默认策略不启用工装班次级结算。
    }

    void finishAssignment(C context);
}
