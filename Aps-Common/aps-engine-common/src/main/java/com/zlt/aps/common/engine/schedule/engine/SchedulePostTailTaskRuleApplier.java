package com.zlt.aps.common.engine.schedule.engine;

/**
 * 收尾后起排班次任务运行态适配器。
 *
 * <p>该组件只把公共 C/S/N 判定结果写回公共任务模型，不读取数据库，
 * 也不负责 TM/TC 的国际化或未排结果落库。</p>
 */
public final class SchedulePostTailTaskRuleApplier {

    /**
     * 收尾间隔内统一使用的未排原因编码。
     */
    public static final String POST_TAIL_BLOCKED_REASON_CODE = "POST_TAIL_SHIFT_BLOCKED";

    private SchedulePostTailTaskRuleApplier() {
    }

    /**
     * 将收尾后起排判定写入任务运行态。
     *
     * @param task              当前目标任务
     * @param closeOutState     产品实际收尾状态
     * @param parameter         本批次参数快照
     * @param blockedReasonCode 领域未排原因编码
     * @param blockedReasonDesc 领域未排原因说明
     * @return 当前任务判定结果
     */
    public static SchedulePostTailDecision apply(ScheduleTaskDraftModel task,
                                                 SchedulePostTailCloseOutState closeOutState,
                                                 SchedulePostTailShiftParameter parameter,
                                                 String blockedReasonCode,
                                                 String blockedReasonDesc) {
        if (task == null) {
            return null;
        }
        SchedulePostTailDecision decision = SchedulePostTailRuleEvaluator.evaluate(
                closeOutState, task.getShiftOrder(), parameter);
        task.setPostTailShiftParameter(parameter);
        task.setPostTailCloseOutState(closeOutState);
        task.setPostTailDecision(decision);
        task.setPostTailShiftBlocked(Boolean.TRUE.equals(decision.getBlocked()));
        task.setPostTailSkippedShiftOrders(decision.getSkippedTargetShiftOrders());
        if (Boolean.TRUE.equals(decision.getBlocked())) {
            task.setMachineCode(null);
            task.setUnplannedReasonCode(blockedReasonCode);
            task.setUnplannedReasonDesc(blockedReasonDesc);
        } else if (blockedReasonCode != null
                && blockedReasonCode.equals(task.getUnplannedReasonCode())) {
            task.setUnplannedReasonCode(null);
            task.setUnplannedReasonDesc(null);
            task.setPostTailBlockedQty(java.math.BigDecimal.ZERO);
        } else {
            task.setPostTailBlockedQty(java.math.BigDecimal.ZERO);
        }
        return decision;
    }

    /**
     * 禁排判定成立但库存已经覆盖当班和保证需求时，清除零缺口未排原因。
     *
     * @param task              当前任务
     * @param blockedReasonCode 收尾间隔专用未排原因编码
     */
    public static void clearReasonWhenBlockedGapIsZero(ScheduleTaskDraftModel task,
                                                       String blockedReasonCode) {
        if (task == null || !Boolean.TRUE.equals(task.getPostTailShiftBlocked())
                || task.getPostTailBlockedQty() == null
                || task.getPostTailBlockedQty().compareTo(java.math.BigDecimal.ZERO) > 0
                || blockedReasonCode == null
                || !blockedReasonCode.equals(task.getUnplannedReasonCode())) {
            return;
        }
        task.setUnplannedReasonCode(null);
        task.setUnplannedReasonDesc(null);
    }
}
