package com.zlt.aps.lh.engine.strategy.support;

import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * 结构机台上限只读准入结果。
 *
 * <p>该对象只保存一次准入计算所需的标量和物理机台编码快照，不持有排程上下文、
 * 排程结果或Machine×SKU时间计划，避免候选竞争阶段形成大对象引用链。</p>
 *
 * @author APS
 */
@Getter
public final class StructureMachineLimitDecision {

    /** 是否需要执行结构上限约束 */
    private final boolean applicable;
    /** 是否允许当前机台与SKU在正式目标班次开产 */
    private final boolean allowed;
    /** 当前排程阶段 */
    private final DailySchedulePhase phase;
    /** 正式目标班次所属业务日期 */
    private final LocalDate businessDate;
    /** 正式目标班次序号 */
    private final Integer formalTargetShiftIndex;
    /** 正式目标班次开始时间 */
    private final Date shiftStartTime;
    /** 正式目标班次结束时间 */
    private final Date shiftEndTime;
    /** 候选运行态机台编码 */
    private final String machineCode;
    /** 候选物理机台编码 */
    private final String physicalMachineCode;
    /** 物料编码 */
    private final String materialCode;
    /** 产品结构名称 */
    private final String structureName;
    /** SKU首次进入候选池的原始日期 */
    private final LocalDate originalPoolDate;
    /** 收尾排除前的结构物理机台编码 */
    private final Set<String> rawPhysicalMachineCodes;
    /** 在正式目标班次边界完成下机的物理机台编码 */
    private final Set<String> excludedEndingPhysicalMachineCodes;
    /** 调整后仍有效占用结构名额的物理机台编码 */
    private final Set<String> effectivePhysicalMachineCodes;
    /** 候选物理机台新增结构名额 */
    private final int newMachineDelta;
    /** 正式开产业务日结构机台数上限 */
    private final int structureMachineLimit;
    /** 准入或拒绝原因 */
    private final String reason;

    /**
     * 创建结构机台上限准入结果。
     *
     * @param applicable 是否适用结构上限
     * @param allowed 是否允许开产
     * @param phase 当前排程阶段
     * @param businessDate 正式开产业务日期
     * @param formalTargetShiftIndex 正式目标班次
     * @param shiftStartTime 班次开始时间
     * @param shiftEndTime 班次结束时间
     * @param machineCode 运行态机台编码
     * @param physicalMachineCode 物理机台编码
     * @param materialCode 物料编码
     * @param structureName 结构名称
     * @param originalPoolDate 原始候选池日期
     * @param rawPhysicalMachineCodes 原始结构机台集合
     * @param excludedEndingPhysicalMachineCodes 收尾排除机台集合
     * @param effectivePhysicalMachineCodes 有效结构机台集合
     * @param newMachineDelta 新增结构机台增量
     * @param structureMachineLimit 结构机台数上限
     * @param reason 准入原因
     */
    public StructureMachineLimitDecision(
            boolean applicable,
            boolean allowed,
            DailySchedulePhase phase,
            LocalDate businessDate,
            Integer formalTargetShiftIndex,
            Date shiftStartTime,
            Date shiftEndTime,
            String machineCode,
            String physicalMachineCode,
            String materialCode,
            String structureName,
            LocalDate originalPoolDate,
            Set<String> rawPhysicalMachineCodes,
            Set<String> excludedEndingPhysicalMachineCodes,
            Set<String> effectivePhysicalMachineCodes,
            int newMachineDelta,
            int structureMachineLimit,
            String reason) {
        this.applicable = applicable;
        this.allowed = allowed;
        this.phase = phase;
        this.businessDate = businessDate;
        this.formalTargetShiftIndex = formalTargetShiftIndex;
        this.shiftStartTime = shiftStartTime;
        this.shiftEndTime = shiftEndTime;
        this.machineCode = machineCode;
        this.physicalMachineCode = physicalMachineCode;
        this.materialCode = materialCode;
        this.structureName = structureName;
        this.originalPoolDate = originalPoolDate;
        this.rawPhysicalMachineCodes = immutableView(rawPhysicalMachineCodes);
        this.excludedEndingPhysicalMachineCodes = immutableView(
                excludedEndingPhysicalMachineCodes);
        this.effectivePhysicalMachineCodes = immutableView(
                effectivePhysicalMachineCodes);
        this.newMachineDelta = Math.max(0, newMachineDelta);
        this.structureMachineLimit = Math.max(0, structureMachineLimit);
        this.reason = reason;
    }

    /** @return 收尾排除前结构物理机台数 */
    public int getRawMachineCount() {
        return rawPhysicalMachineCodes.size();
    }

    /** @return 班次边界收尾调整后的结构有效物理机台数 */
    public int getEffectiveMachineCount() {
        return effectivePhysicalMachineCodes.size();
    }

    /**
     * 为运行态版本缓存中的冻结集合创建只读视图，不按Machine×SKU重复复制机台编码。
     */
    private static Set<String> immutableView(Set<String> sourceSet) {
        if (sourceSet == null || sourceSet.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(sourceSet);
    }
}
