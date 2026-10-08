package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.PrecisionMaintenanceJudgeDTO;
import lombok.Getter;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 当前批次的设备计划编排状态；原始判定、候选时间计划与有效安排分别存放。 */
@Getter
public final class EquipmentPlanRuntimeState {

    /** 各运行侧最近一次判断输入；任一侧变化时整台物理机台重新评估。 */
    private final Map<String, String> inputVersionMap = new LinkedHashMap<>();
    /** 最近一次原始判定，0仅对该次输入有效。 */
    private final Map<String, PrecisionMaintenanceJudgeDTO> decisionMap = new LinkedHashMap<>();
    /** 已适配、尚未必正式接受的候选时间计划。 */
    private final Map<String, EquipmentPlanTimeline> candidateTimelineMap = new LinkedHashMap<>();
    /** 独立的安排状态；回填只能读取最终有效安排。 */
    private final Map<Long, EquipmentPlanArrangement> arrangementMap = new LinkedHashMap<>();
    /** 无确认收尾或输入尚不完整的机台，输入变化后受控复评。 */
    private final Set<String> pendingPhysicalMachines = new LinkedHashSet<>();
    /** 未覆盖组合原因去重，避免每次预演重复输出。 */
    private final Set<String> diagnosticKeys = new LinkedHashSet<>();
    /** 仅约束发布、撤销时递增，用于失效候选、容量和时间缓存。 */
    private long constraintVersion;

    /** 登记约束变化，由公共编排调用。 */
    public void constraintsChanged() {
        constraintVersion++;
    }

    /** @return 独立运行态副本；只读时间计划及安排记录可共享。 */
    public EquipmentPlanRuntimeState copy() {
        EquipmentPlanRuntimeState target = new EquipmentPlanRuntimeState();
        target.inputVersionMap.putAll(inputVersionMap);
        decisionMap.forEach((machine, decision) -> target.decisionMap.put(machine, copyDecision(decision)));
        target.candidateTimelineMap.putAll(candidateTimelineMap);
        target.arrangementMap.putAll(arrangementMap);
        target.pendingPhysicalMachines.addAll(pendingPhysicalMachines);
        target.diagnosticKeys.addAll(diagnosticKeys);
        target.constraintVersion = constraintVersion;
        return target;
    }

    /** @param source 原始判定 @return 独立判定副本 */
    public static PrecisionMaintenanceJudgeDTO copyDecision(PrecisionMaintenanceJudgeDTO source) {
        PrecisionMaintenanceJudgeDTO target = new PrecisionMaintenanceJudgeDTO();
        target.setMachineCode(source.getMachineCode());
        target.setPrecisionPlanId(source.getPrecisionPlanId());
        target.setDurationHours(source.getDurationHours());
        target.setForceDown(source.isForceDown());
        target.setPrecisionStartTime(source.getPrecisionStartTime() == null
                ? null : new Date(source.getPrecisionStartTime().getTime()));
        target.setPrecisionEndTime(source.getPrecisionEndTime() == null
                ? null : new Date(source.getPrecisionEndTime().getTime()));
        return target;
    }
}
