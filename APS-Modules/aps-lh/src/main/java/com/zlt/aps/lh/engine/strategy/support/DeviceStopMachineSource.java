package com.zlt.aps.lh.engine.strategy.support;

import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 设备停机来源与排程机台的不可变对应关系，保留来源侧别及指定模具。
 * 本对象只描述身份，不决定清洗、置换、产能或换模时段。
 *
 * @author APS
 * @since 2026/11/13
 */
@Getter
@ToString
public final class DeviceStopMachineSource {
    /**
     * 原始停机计划主键，供清洗事件及执行日期回填关联。
     */
    private final Long sourcePlanId;
    /**
     * 原始配置的机台编码，不以运行机台覆盖。
     */
    private final String sourceMachineCode;
    /**
     * 来源所属物理机台编码。
     */
    private final String physicalMachineCode;
    /**
     * 清洗窗口和排程事件使用的实际运行机台编码。
     */
    private final String runtimeMachineCode;
    /**
     * 来源配置的左右侧标识；整机来源为空。
     */
    private final String sourceSide;
    /**
     * 来源明确维护的模具集合，不按整机在机模具扩写。
     */
    private final Set<String> sourceMouldCodes;
    /**
     * 是否为已确认的硫化喷砂单侧来源归属双模整机。
     */
    private final boolean sideMappedToPhysical;

    /**
     * 保存解析后的来源身份并隔离可变模具集合。
     *
     * @param sourcePlanId 原停机计划主键
     * @param sourceMachineCode 原配置机台编码
     * @param physicalMachineCode 物理机台编码
     * @param runtimeMachineCode 实际运行机台编码
     * @param sourceSide 来源左右侧
     * @param sourceMouldCodes 已解析的非空模具集合容器
     * @param sideMappedToPhysical 是否按硫化喷砂规则将来源侧归属整机
     */
    public DeviceStopMachineSource(Long sourcePlanId, String sourceMachineCode, String physicalMachineCode,
            String runtimeMachineCode, String sourceSide, Set<String> sourceMouldCodes,
            boolean sideMappedToPhysical) {
        this.sourcePlanId = sourcePlanId;
        this.sourceMachineCode = sourceMachineCode;
        this.physicalMachineCode = physicalMachineCode;
        this.runtimeMachineCode = runtimeMachineCode;
        this.sourceSide = sourceSide;
        this.sourceMouldCodes = Collections.unmodifiableSet(new LinkedHashSet<>(sourceMouldCodes));
        this.sideMappedToPhysical = sideMappedToPhysical;
    }

    /**
     * 校验明确指定的来源模具与实际装机身份，单侧来源只允许一副属于该整机的模具。
     * 整机及真实单控侧继续严格集合相等；空来源不构成已确认原模身份。
     *
     * @param actualMouldCodes 调用方确认的实际装机模具集合
     * @return 来源模具身份是否匹配
     */
    public boolean matchesMoulds(Set<String> actualMouldCodes) {
        if (sourceMouldCodes.isEmpty() || actualMouldCodes == null || actualMouldCodes.isEmpty()) {
            return false;
        }
        if (sideMappedToPhysical) {
            return sourceMouldCodes.size() == 1 && actualMouldCodes.containsAll(sourceMouldCodes);
        }
        return sourceMouldCodes.equals(actualMouldCodes);
    }
}
