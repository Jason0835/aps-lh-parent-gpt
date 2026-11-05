package com.zlt.aps.lh.service.impl;

/**
 * 新增排产单个提案提交结果。
 *
 * <p>明确区分“形成排程结果”“形成终局状态”和“失败后完整回滚”，使上层能够只在
 * 运行态真实变化时失效提案缓存。不得再使用排产数量0同时表达终局提交与失败回滚。</p>
 *
 * @author APS
 */
public final class NewSpecScheduleCommitResult {

    /** 本次新增的有效排程结果数量 */
    private final int scheduledCount;
    /** 是否已经保留正式结果或终局未排等状态变化 */
    private final boolean stateChanged;
    /** 是否因未形成有效结果而完整恢复运行态 */
    private final boolean rolledBack;

    /**
     * 回滚前冻结的拒绝原因，成功及无明确拒绝原因时为空。
     */
    private final String failureReason;

    /**
     * 保存提交状态及独立于资源快照的失败证据。
     *
     * @param scheduledCount 有效结果数
     * @param stateChanged 是否保留正式状态
     * @param rolledBack 是否完整恢复
     * @param failureReason 回滚原因
     */
    private NewSpecScheduleCommitResult(int scheduledCount,
                                        boolean stateChanged,
                                        boolean rolledBack, String failureReason) {
        this.scheduledCount = Math.max(0, scheduledCount);
        this.stateChanged = stateChanged;
        this.rolledBack = rolledBack;
        this.failureReason = failureReason;
    }

    /**
     * 构造形成有效排程结果的提交结果。
     *
     * @param scheduledCount 新增结果数量
     * @return 提交结果
     */
    public static NewSpecScheduleCommitResult resultCommitted(int scheduledCount) {
        return new NewSpecScheduleCommitResult(scheduledCount, true, false, null);
    }

    /**
     * 构造形成终局未排或候选出队的提交结果。
     *
     * @return 提交结果
     */
    public static NewSpecScheduleCommitResult terminalStateCommitted() {
        return new NewSpecScheduleCommitResult(0, true, false, null);
    }

    /**
     * 构造失败后完整回滚的提交结果。
     *
     * @return 提交结果
     */
    public static NewSpecScheduleCommitResult rolledBack() {
        return rolledBack(null);
    }

    /**
     * 构造携带明确拒绝证据的完整回滚结果。
     *
     * @param failureReason 资源恢复前捕获的拒绝原因
     * @return 不携带试排资源状态的回滚结果
     */
    public static NewSpecScheduleCommitResult rolledBack(String failureReason) {
        return new NewSpecScheduleCommitResult(0, false, true, failureReason);
    }

    /**
     * 读取回滚前冻结的失败原因。
     *
     * @return 明确拒绝原因，无明确原因时为空
     */
    public String getFailureReason() {
        return failureReason;
    }

    public int getScheduledCount() {
        return scheduledCount;
    }

    public boolean isStateChanged() {
        return stateChanged;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }
}
