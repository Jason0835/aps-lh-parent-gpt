package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 收尾后起排班次解释证据构造器。
 *
 * <p>返回值只供结果解释 JSON 使用。调用方必须通过解释专用轨迹入口写入，
 * 不得把该证据追加到现有过程日志命中列表。</p>
 */
public final class SchedulePostTailEvidenceBuilder {

    private SchedulePostTailEvidenceBuilder() {
    }

    /**
     * 构造任务级收尾后起排判定证据。
     *
     * @param task 当前任务
     * @return 结构化解释证据
     */
    public static Map<String, Object> build(ScheduleTaskDraftModel task) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (task == null) {
            return evidence;
        }
        SchedulePostTailShiftParameter parameter = task.getPostTailShiftParameter();
        SchedulePostTailCloseOutState closeOutState = task.getPostTailCloseOutState();
        SchedulePostTailDecision decision = task.getPostTailDecision();
        evidence.put("parameterCode", parameter == null ? null : parameter.getParamCode());
        evidence.put("parameterRawValue", parameter == null ? null : parameter.getRawValue());
        evidence.put("parameterEffectiveN", parameter == null ? null : parameter.getEffectiveShiftCount());
        evidence.put("parameterSource", parameter == null ? null : parameter.getSource());
        evidence.put("parameterFallbackReason", parameter == null ? null : parameter.getFallbackReason());
        evidence.put("closeOutConfirmed", closeOutState == null ? null : closeOutState.getConfirmed());
        evidence.put("actualCloseOutShiftOrder", closeOutState == null
                ? null : closeOutState.getActualCloseOutShiftOrder());
        evidence.put("confirmationShiftOrder", closeOutState == null
                ? null : closeOutState.getConfirmationShiftOrder());
        evidence.put("closeOutSourceKeys", closeOutState == null
                ? null : closeOutState.getSourceKeySummary());
        evidence.put("closeOutReason", closeOutState == null
                ? null : closeOutState.getConfirmationReason());
        evidence.put("closedStockQty", closeOutState == null ? null : closeOutState.getClosedStockQty());
        evidence.put("availableCloseOutStockQty", closeOutState == null
                ? null : closeOutState.getAvailableCloseOutStockQty());
        evidence.put("unresolvedShortageQty", closeOutState == null
                ? null : closeOutState.getUnresolvedShortageQty());
        evidence.put("remainingCloseOutDemandQty", closeOutState == null
                ? null : closeOutState.getRemainingCloseOutDemandQty());
        evidence.put("targetShiftOrder", decision == null ? task.getShiftOrder() : decision.getTargetShiftOrder());
        evidence.put("intervalShiftCount", decision == null ? null : decision.getIntervalShiftCount());
        evidence.put("recoveryShiftOrder", decision == null ? null : decision.getRecoveryShiftOrder());
        evidence.put("decisionReason", decision == null ? null : decision.getDecisionReason());
        evidence.put("blocked", decision == null ? null : decision.getBlocked());
        evidence.put("allowNormalPlan", decision == null ? null : decision.getAllowNormalPlan());
        evidence.put("skippedTargetShiftOrders", decision == null
                ? null : decision.getSkippedTargetShiftOrders());
        evidence.put("stockCoverageApplicable", task.getStockCoverageWindow() != null);
        evidence.put("stockCoverageCovered", task.getStockCoverageCovered());
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("guardDemandQty", task.getGuardDemandQty());
        evidence.put("openingStockQty", task.getRollingStockQty());
        evidence.put("blockedQty", task.getPostTailBlockedQty());
        evidence.put("actualAssignedQty", decision != null && Boolean.TRUE.equals(decision.getBlocked())
                ? BigDecimal.ZERO : null);
        evidence.put("planQty", task.getPlanQty());
        evidence.put("unplannedReasonCode", task.getUnplannedReasonCode());
        evidence.put("unplannedReasonDesc", task.getUnplannedReasonDesc());
        return evidence;
    }
}
