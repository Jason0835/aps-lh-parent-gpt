package com.zlt.aps.common.engine.schedule;

/**
 * 自动排程公共流程模板。
 *
 * <p>该类只固定自动排程步骤顺序和上下文校验，具体步骤、领域响应及异常类型由业务模块实现。
 * 数据加载和结果持久化通过业务模块提供的步骤实现，从而避免公共模块依赖 TM/TC 领域模型。</p>
 *
 * @param <C> 排程上下文类型
 * @param <R> 排程响应类型
 */
public abstract class AbstractAutoScheduleTemplate<C, R> {

    /**
     * 执行自动排程公共流程。
     *
     * @param context 排程上下文
     * @return 排程响应
     */
    public R execute(C context) {
        this.validateContext(context);
        this.doBootstrap(context);
        this.doInventoryPredict(context);
        this.doDemandAndPlanCalc(context);
        this.doTaskSort(context);
        this.doMachineAssign(context);
        this.doSnapshotAndPersist(context);
        return this.buildResponse(context);
    }

    /**
     * 校验排程上下文。
     *
     * @param context 排程上下文
     */
    protected abstract void validateContext(C context);

    /**
     * 初始化并加载领域数据。
     *
     * @param context 排程上下文
     */
    protected abstract void doBootstrap(C context);

    /**
     * 计算预计库存。
     *
     * @param context 排程上下文
     */
    protected abstract void doInventoryPredict(C context);

    /**
     * 计算需求量和计划量。
     *
     * @param context 排程上下文
     */
    protected abstract void doDemandAndPlanCalc(C context);

    /**
     * 对待排任务排序。
     *
     * @param context 排程上下文
     */
    protected abstract void doTaskSort(C context);

    /**
     * 执行机台分配。
     *
     * @param context 排程上下文
     */
    protected abstract void doMachineAssign(C context);

    /**
     * 构建快照并持久化排程结果。
     *
     * @param context 排程上下文
     */
    protected abstract void doSnapshotAndPersist(C context);

    /**
     * 构建业务领域排程响应。
     *
     * @param context 排程上下文
     * @return 排程响应
     */
    protected abstract R buildResponse(C context);
}
