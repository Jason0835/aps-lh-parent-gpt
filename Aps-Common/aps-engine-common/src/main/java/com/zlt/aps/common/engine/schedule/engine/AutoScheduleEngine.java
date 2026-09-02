package com.zlt.aps.common.engine.schedule.engine;

/** 自动排程固定步骤公共引擎。 */
public final class AutoScheduleEngine<C, R> {

    /**
     * 按固定顺序执行自动排程。
     *
     * @param context 排程上下文
     * @param policy 领域步骤端口
     * @return 领域响应
     */
    public R execute(C context, AutoSchedulePolicy<C, R> policy) {
        policy.validateContext(context);
        policy.bootstrap(context);
        policy.predictInventory(context);
        policy.calculatePlan(context);
        policy.sortTasks(context);
        policy.assignMachines(context);
        policy.buildSnapshotAndPersist(context);
        return policy.buildResponse(context);
    }
}
