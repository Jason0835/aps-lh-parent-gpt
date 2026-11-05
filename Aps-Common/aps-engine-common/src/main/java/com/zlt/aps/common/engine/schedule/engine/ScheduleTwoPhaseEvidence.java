package com.zlt.aps.common.engine.schedule.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 两阶段日志与最终解释共用证据视图，禁止各自重算数量。
 */
public final class ScheduleTwoPhaseEvidence {

    /**
     * 提取当前草稿的生命周期、来源、数量和原因快照。
     *
     * @param task 原任务或实际生产片段
     * @return 稳定字段名的内部解释对象
     */
    public Map<String, Object> snapshot(ScheduleTaskDraftModel task) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("businessKey", task.getBusinessKey());
        evidence.put("processCode", task.getProcessCode());
        evidence.put("machineCode", task.getMachineCode());
        evidence.put("shiftOrder", task.getShiftOrder());
        evidence.put("planQty", task.getPlanQty());
        evidence.put("demandOnly", task.getDemandOnlyEvidence());
        evidence.put("wholeRollExempt", ScheduleDemandOnlyQuantityPolicy.enabled(task));
        evidence.put("demandOnlyMinimumQty", task.getDemandOnlyMinimumQty());
        evidence.put("supplyHours", task.getSupplyHours());
        evidence.put("settlementMachineCode", task.getSettlementMachineCode());
        evidence.put("carryRequestQty", task.getCarryRequestQty());
        evidence.put("integerRoundingBeforeQty", task.getIntegerRoundingBeforeQty());
        evidence.put("integerRoundingAddedQty", task.getIntegerRoundingAddedQty());
        evidence.put("curlRollLength", task.getCurlRollLength());
        if (task.getSettlementMode() == ScheduleSettlementMode.NORMAL
                && task.getIntegerRoundingBeforeQty() != null) {
            evidence.put("roundingRule", ScheduleDemandOnlyQuantityPolicy.enabled(task)
                    ? "DEMAND_ONLY_INTEGER_CEILING" : "ROLL_CEILING_THEN_INTEGER_CEILING");
            // 原行扩增后仍保留基础候选补量，禁止把基础补量误当作每次追加补量。
            evidence.put("integerRoundingScope", Boolean.TRUE.equals(task.getExtraCreateTask())
                    ? "EXTRA_CREATE_TARGET" : "BASE_CANDIDATE");
        }
        evidence.put("inputQuantityStage", task.getInputQuantityStage());
        evidence.put("twoPhaseDuplicateTask", task.getTwoPhaseDuplicateTask());
        evidence.put("resolvedLossRate", task.getResolvedLossRate());
        evidence.put("lossMatchLevel", task.getLossMatchLevel());
        evidence.put("lossMatchSource", task.getLossMatchSource());
        evidence.put("preLossPlanQty", task.getPreLossPlanQty());
        evidence.put("lossAddQty", task.getLossAddQty());
        evidence.put("minStartAdjustQty", task.getMinStartAdjustQty());
        evidence.put("tailRoundAdjustQty", task.getTailRoundAdjustQty());
        evidence.put("tailPlanBaseRequestQty", task.getTailPlanBaseRequestQty());
        evidence.put("tailLimitRequestQty", task.getTailLimitRequestQty());
        evidence.put("tailPlanCandidateQty", task.getTailPlanCandidateQty());
        evidence.put("tailLimitCandidateQty", task.getTailLimitCandidateQty());
        evidence.put("planQtyBeforeToolLimit", task.getPlanQtyBeforeToolLimit());
        evidence.put("availableToolQty", task.getAvailableToolQty());
        evidence.put("remainingToolQty", task.getRemainingToolQty());
        evidence.put("toolUsedQty", task.getToolUsedQty());
        evidence.put("machineRemainCapacity", task.getMachineRemainCapacity());
        evidence.put("formingGuardWindowQtyMap", task.getFormingGuardWindowQtyMap() == null ? null
                : new LinkedHashMap<>(task.getFormingGuardWindowQtyMap()));
        evidence.put("scope", task.getPhysicalPlanQty() == null ? "PHYSICAL_OR_REQUEST" : "SOURCE_ATTRIBUTION");
        evidence.put("physicalPlanQty", task.getPhysicalPlanQty());
        evidence.put("physicalBaseAssignQty", task.getPhysicalBaseAssignQty());
        evidence.put("physicalExtraAssignQty", task.getPhysicalExtraAssignQty());
        evidence.put("requestSourceMode", task.getRequestSourceMode());
        evidence.put("settlementMode", task.getSettlementMode());
        evidence.put("extraPolicy", task.getExtraPolicy());
        evidence.put("extraCreateTask", task.getExtraCreateTask());
        if (task.getExtraCreateWindowProof() != null) {
            evidence.put("twoPhaseEvidenceVersion", 2);
            evidence.put("extraCreateWindowProof", task.getExtraCreateWindowProof().copy());
        }
        evidence.put("quantityStage", task.getQuantityStage());
        evidence.put("copyPurpose", task.getCopyPurpose());
        evidence.put("inputSnapshotKey", task.getInputSnapshotKey());
        evidence.put("preLossValueStage", task.getPreLossValueStage());
        evidence.put("sourceBusinessKey", task.getSourceBusinessKey());
        evidence.put("sourceIdentity", task.getSourceIdentity());
        evidence.put("demandEventKey", task.getDemandEventKey());
        evidence.put("demandShiftKey", task.getDemandShiftKey());
        evidence.put("originDemandShiftKey", task.getOriginDemandShiftKey());
        evidence.put("originalSourceShiftKey", task.getOriginalSourceShiftKey());
        evidence.put("originBaseQuotaKey", task.getOriginBaseQuotaKey());
        evidence.put("phaseBalanceKey", task.getPhaseBalanceKey());
        evidence.put("productionAllocKey", task.getProductionAllocKey());
        evidence.put("originProductionAllocKey", task.getOriginProductionAllocKey());
        evidence.put("baseProductionShiftKey", task.getBaseProductionShiftKey());
        evidence.put("actualProductionShiftKey", task.getActualProductionShiftKey());
        evidence.put("extraPrioritySnapshot", task.getExtraPrioritySnapshot());
        evidence.put("baseSortIndex", task.getBaseSortIndex());
        evidence.put("planCalcOrderIndex", task.getPlanCalcOrderIndex());
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("nextShiftDemandQty", task.getNextShiftDemandQty());
        evidence.put("guardDemandQty", task.getGuardDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("stockCoverageCovered", task.getStockCoverageCovered());
        evidence.put("postTailShiftBlocked", task.getPostTailShiftBlocked());
        evidence.put("postTailBlockedQty", task.getPostTailBlockedQty());
        evidence.put("dispatchBlocked", task.getDispatchBlocked());
        evidence.put("unplannedReasonCode", task.getUnplannedReasonCode());
        evidence.put("baseRequestQty", task.getBaseRequestQty());
        evidence.put("targetRequestQty", task.getTargetRequestQty());
        evidence.put("baseCommittedQty", task.getBaseCommittedQty());
        evidence.put("targetCommittedQty", task.getTargetCommittedQty());
        evidence.put("baseRequired", task.getBaseRequired());
        evidence.put("baseAssignQty", task.getBaseAssignQty());
        evidence.put("extraAssignQty", task.getExtraAssignQty());
        evidence.put("baseRemaining", task.getBaseRemaining());
        evidence.put("extraPotentialQty", task.getExtraPotentialQty());
        evidence.put("extraEligibleTask", task.getExtraEligibleTask());
        evidence.put("extraTargetQty", task.getExtraTargetQty());
        evidence.put("extraAllowedQty", task.getExtraAllowedQty());
        evidence.put("extraActualQty", task.getExtraActualQty());
        evidence.put("extraRemaining", task.getExtraRemaining());
        evidence.put("twoPhaseReasonCodes", task.getTwoPhaseReasonCodes() == null ? null
                : new java.util.LinkedHashSet<>(task.getTwoPhaseReasonCodes()));
        evidence.put("latestStartTime", task.getLatestStartTime());
        evidence.put("machineSpeed", task.getMachineSpeed());
        evidence.put("machineSpeedUnit", "M_PER_HOUR");
        evidence.put("machineSpeedRawValue", task.getMachineSpeedRawValue());
        evidence.put("machineSpeedRawUnit", task.getMachineSpeedRawUnit());
        evidence.put("machineSpeedSource", task.getMachineSpeedSource());
        return evidence;
    }
}
