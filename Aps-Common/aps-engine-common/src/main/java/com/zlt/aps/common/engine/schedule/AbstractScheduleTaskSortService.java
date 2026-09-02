package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 待排任务公共排序默认实现。
 *
 * <p>公共算法统一采用“计划量计算顺序优先；未准备计划量顺序时按班次、库存供应时长和领域策略排序”的口径。
 * TM/TC 通过适配方法提供任务字段、策略比较器、日志和规则轨迹，避免复制排序主流程。</p>
 *
 * @param <C> 排程上下文类型
 * @param <T> 待排任务类型
 */
public abstract class AbstractScheduleTaskSortService<C, T> {

    /**
     * 执行待排任务公共排序流程。
     *
     * @param context 排程上下文
     */
    public final void sort(C context) {
        this.validateContext(context);
        List<T> taskList = this.getTaskList(context);
        if (taskList == null || taskList.isEmpty()) {
            return;
        }

        String strategyCode = this.resolveStrategyCode(context);
        String beforeOrder = this.summarizeTaskOrder(context);
        boolean planCalcOrderReady = taskList.stream()
                .allMatch(task -> task != null && this.getPlanCalcOrderIndex(task) != null);
        String sortSource = planCalcOrderReady ? "PLAN_CALC_ORDER" : "LEGACY_TASK_SORT";
        if (planCalcOrderReady) {
            // 计划量计算已确定顺序时只固化该顺序，避免排序阶段再次改变业务优先级。
            taskList.sort(Comparator
                    .comparing(this::getPlanCalcOrderIndex,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(task -> this.defaultString(this.getBusinessKey(task))));
        } else {
            // 独立调用或兼容旧测试上下文时，复用领域策略并增加公共供应时长优先级。
            Comparator<T> originalComparator = this.buildStrategyComparator(context, strategyCode);
            taskList.sort(this.buildStartupAwareComparator(originalComparator));
        }

        String afterOrder = this.summarizeTaskOrder(context);
        this.logTaskSortSummary(context, strategyCode, sortSource, beforeOrder, afterOrder, taskList.size());
        for (int index = 0; index < taskList.size(); index++) {
            T task = taskList.get(index);
            int sortIndex = planCalcOrderReady && this.getPlanCalcOrderIndex(task) != null
                    ? this.getPlanCalcOrderIndex(task) : index + 1;
            this.setBaseSortIndex(task, sortIndex);
            this.recordTaskSort(context, task, strategyCode, sortSource, sortIndex,
                    this.isStartupShift(context, task));
            this.logTaskSortDetail(context, task, strategyCode, sortSource, sortIndex);
        }
    }

    /**
     * 校验排序上下文。
     *
     * @param context 排程上下文
     */
    protected abstract void validateContext(C context);

    /**
     * 获取待排任务列表。
     *
     * @param context 排程上下文
     * @return 待排任务列表
     */
    protected abstract List<T> getTaskList(C context);

    /**
     * 读取排序策略编码。
     *
     * @param context 排程上下文
     * @return 排序策略编码
     */
    protected abstract String resolveStrategyCode(C context);

    /**
     * 构建领域原始排序比较器。
     *
     * @param context      排程上下文
     * @param strategyCode 排序策略编码
     * @return 领域排序比较器
     */
    protected abstract Comparator<T> buildStrategyComparator(C context, String strategyCode);

    /**
     * 获取计划量计算阶段生成的全局顺序。
     *
     * @param task 待排任务
     * @return 计划量顺序
     */
    protected abstract Integer getPlanCalcOrderIndex(T task);

    /**
     * 获取任务业务键。
     *
     * @param task 待排任务
     * @return 业务键
     */
    protected abstract String getBusinessKey(T task);

    /**
     * 获取任务班次顺序。
     *
     * @param task 待排任务
     * @return 班次顺序
     */
    protected abstract Integer getShiftOrder(T task);

    /**
     * 获取库存供应时长。
     *
     * @param task 待排任务
     * @return 供应时长
     */
    protected abstract BigDecimal getSupplyHours(T task);

    /**
     * 写入任务基础排序序号。
     *
     * @param task      待排任务
     * @param sortIndex 基础排序序号
     */
    protected abstract void setBaseSortIndex(T task, int sortIndex);

    /**
     * 判断任务是否属于开产班次。
     *
     * @param context 排程上下文
     * @param task    待排任务
     * @return 是否为开产班次
     */
    protected abstract boolean isStartupShift(C context, T task);

    /**
     * 记录任务排序规则轨迹。
     *
     * @param context    排程上下文
     * @param task       待排任务
     * @param strategyCode 策略编码
     * @param sortSource 排序来源
     * @param sortIndex  基础排序序号
     * @param startupShift 是否为开产班次
     */
    protected abstract void recordTaskSort(C context, T task, String strategyCode, String sortSource,
                                           int sortIndex, boolean startupShift);

    /**
     * 记录任务排序汇总日志。
     *
     * @param context    排程上下文
     * @param strategyCode 策略编码
     * @param sortSource 排序来源
     * @param beforeOrder 排序前摘要
     * @param afterOrder 排序后摘要
     * @param taskCount 任务数量
     */
    protected abstract void logTaskSortSummary(C context, String strategyCode, String sortSource,
                                               String beforeOrder, String afterOrder, int taskCount);

    /**
     * 记录单任务排序明细日志。
     *
     * @param context    排程上下文
     * @param task       待排任务
     * @param strategyCode 策略编码
     * @param sortSource 排序来源
     * @param sortIndex  基础排序序号
     */
    protected abstract void logTaskSortDetail(C context, T task, String strategyCode,
                                              String sortSource, int sortIndex);

    /**
     * 构建公共供应时长优先比较器。
     *
     * @param originalComparator 领域原始比较器
     * @return 加入班次和供应时长优先级的比较器
     */
    private Comparator<T> buildStartupAwareComparator(Comparator<T> originalComparator) {
        return Comparator
                .comparing(this::getShiftOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(this::getSupplyHours, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(originalComparator);
    }

    /**
     * 汇总排序日志中的空业务键。
     *
     * @param value 原始业务键
     * @return 非空文本
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 汇总当前任务顺序，具体字段和数量上限由领域实现决定。
     *
     * @param context 排程上下文
     * @return 顺序摘要
     */
    protected abstract String summarizeTaskOrder(C context);
}
