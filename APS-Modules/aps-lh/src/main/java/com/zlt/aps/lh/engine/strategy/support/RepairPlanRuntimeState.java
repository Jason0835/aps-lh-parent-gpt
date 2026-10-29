package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批次级维修事实的唯一持有者，兼容访问器均委托本对象。
 * 只缓存来源归一化结果；候选组合即算即弃，撤销候选不删除固定维修及故障事实。
 *
 * @author APS
 * @date 2026/10/21
 */
@Getter
public final class RepairPlanRuntimeState {

    /**
     * 原始维修来源快照，以来源主键唯一登记。
     */
    private final Map<Long, MdmDevicePlanShut> sourcePlanMap = new LinkedHashMap<>();
    /**
     * 原始故障禁产窗口。
     */
    private final Map<Long, MachineFaultWindowDTO> faultWindowMap = new LinkedHashMap<>();
    /**
     * 在原续作阶段识别并推进的迁移事件。
     */
    private final Map<String, ContinuationTemporaryFaultTransferEvent> transferEventMap = new LinkedHashMap<>();
    /**
     * 当前实际维修列表的物理机台索引，避免每个候选扫描全量停机来源。
     */
    private final Map<String, List<MdmDevicePlanShut>> physicalRepairPlanMap = new LinkedHashMap<>();
    /**
     * 物理机台故障索引，避免每个维修候选再次扫描全量故障。
     */
    private final Map<String, List<MachineFaultWindowDTO>> physicalFaultWindowMap = new LinkedHashMap<>();
    /**
     * 各物理机台的独立容量投影缓存，返回调用方前再复制。
     */
    private final Map<String, List<MachineMaintenanceWindowDTO>> capacityWindowMap = new LinkedHashMap<>();
    /**
     * 当前归一化输入指纹，包含预热参数、排程日期及班次边界。
     */
    private String capacityInputKey;
    /**
     * 当前批次约束版本，来源、列表或容量输入变化时递增。
     */
    private long constraintVersion;

    /**
     * 当前候选持有的维修执行视图，完整收尾排除对应05，退出作用域后恢复外层视图。
     */
    private List<MdmDevicePlanShut> scopedStopPlans;

    /**
     * 已确认执行视图对应的固定实际来源列表，仅用于当前批次视图复用。
     */
    private List<MdmDevicePlanShut> committedStopSource;
    /**
     * 来源时刻、相关整机侧位和收尾数量时间的不可变值快照，回滚或就地改量时自然失效。
     */
    private List<Object> committedStopKey;
    /**
     * 相关事实未变时复用的实际维修投影，保证容量解析器能够复用同一输入对象。
     */
    private List<MdmDevicePlanShut> committedStopView;

    /**
     * 缓存已提交结果决定的维修视图；隔离上下文不复制该派生缓存。
     *
     * @param source 当前固定实际来源
     * @param key 不引用可变日期或结果对象的事实快照
     * @param view 供当前批次重复读取的实际视图
     */
    public void cacheCommittedStopView(List<MdmDevicePlanShut> source, List<Object> key,
            List<MdmDevicePlanShut> view) {
        committedStopSource = source;
        committedStopKey = key;
        committedStopView = view;
    }

    /**
     * 发布候选设备时间视图并失效派生容量，不修改固定维修来源。
     *
     * @param plans 独立候选视图，为空时读取批次固定事实
     */
    public void setScopedStopPlans(List<MdmDevicePlanShut> plans) {
        if (scopedStopPlans != plans) {
            scopedStopPlans = plans;
            this.constraintsChanged();
        }
    }

    /**
     * 失效来源归一化缓存，不改变来源及迁移事件。
     */
    public void constraintsChanged() {
        constraintVersion++;
        capacityInputKey = null;
        physicalRepairPlanMap.clear();
        physicalFaultWindowMap.clear();
        capacityWindowMap.clear();
    }

    /**
     * 发布本次归一化输入指纹，实际窗口由解析器按物理机台惰性生成。
     *
     * @param inputKey 当前配置与班次指纹
     */
    public void useCapacityInput(String inputKey) {
        this.constraintsChanged();
        capacityInputKey = inputKey;
    }

    /**
     * 深复制隔离上下文需要的事实和事件，不共享可变日期、列表或派生缓存。
     *
     * @return 与原排程独立的维修运行态
     */
    public RepairPlanRuntimeState copy() {
        RepairPlanRuntimeState target = new RepairPlanRuntimeState();
        sourcePlanMap.forEach((planId, source) -> target.sourcePlanMap.put(planId, copySource(source)));
        faultWindowMap.forEach((planId, fault) -> target.faultWindowMap.put(planId, copyFault(fault)));
        transferEventMap.forEach((machineCode, event) -> target.transferEventMap.put(machineCode, copyEvent(event)));
        target.constraintVersion = constraintVersion;
        target.scopedStopPlans = scopedStopPlans == null ? null : scopedStopPlans.stream()
                .map(RepairPlanRuntimeState::copySource).collect(java.util.stream.Collectors.toList());
        return target;
    }

    /**
     * 原位恢复候选改变的迁移进度，保留历史承接持有的事件引用及稳定迭代器。
     * 固定来源和故障禁产事实不参与候选回滚。
     *
     * @param savedEvents 候选尝试前的独立事件快照
     */
    public void restoreTransferProgress(Map<String, ContinuationTemporaryFaultTransferEvent> savedEvents) {
        transferEventMap.keySet().removeIf(machineCode -> !savedEvents.containsKey(machineCode));
        savedEvents.forEach((machineCode, saved) -> {
            ContinuationTemporaryFaultTransferEvent target = transferEventMap.get(machineCode);
            ContinuationTemporaryFaultTransferEvent copy = copyEvent(saved);
            if (target == null) {
                transferEventMap.put(machineCode, copy);
            } else {
                BeanUtil.copyProperties(copy, target);
            }
        });
    }

    /**
     * 复制来源对象及可变日期，保护原始来源起止时间。
     *
     * @param source 原始设备计划
     * @return 独立来源副本
     */
    public static MdmDevicePlanShut copySource(MdmDevicePlanShut source) {
        MdmDevicePlanShut target = new MdmDevicePlanShut();
        BeanUtil.copyProperties(source, target);
        target.setBeginDate(copyDate(source.getBeginDate()));
        target.setEndDate(copyDate(source.getEndDate()));
        target.setScheduleDate(copyDate(source.getScheduleDate()));
        target.setCreateTime(copyDate(source.getCreateTime()));
        target.setUpdateTime(copyDate(source.getUpdateTime()));
        return target;
    }

    /**
     * 复制原始故障窗口，候选不能更改禁产事实。
     *
     * @param source 原始故障
     * @return 独立故障副本
     */
    public static MachineFaultWindowDTO copyFault(MachineFaultWindowDTO source) {
        MachineFaultWindowDTO target = new MachineFaultWindowDTO();
        target.setPlanId(source.getPlanId());
        target.setMachineCode(source.getMachineCode());
        target.setStartTime(copyDate(source.getStartTime()));
        target.setEndTime(copyDate(source.getEndTime()));
        return target;
    }

    /**
     * 复制迁移事件，隔离候选及补充计划中的迁移目标和失败原因。
     *
     * @param source 原迁移事件
     * @return 日期和集合均独立的事件
     */
    public static ContinuationTemporaryFaultTransferEvent copyEvent(ContinuationTemporaryFaultTransferEvent source) {
        ContinuationTemporaryFaultTransferEvent target = new ContinuationTemporaryFaultTransferEvent();
        BeanUtil.copyProperties(source, target);
        target.setFaultStartTime(copyDate(source.getFaultStartTime()));
        target.setFaultEndTime(copyDate(source.getFaultEndTime()));
        target.setSourcePlanIdList(new ArrayList<>(source.getSourcePlanIdList()));
        target.setAffectedShiftIndexList(new ArrayList<>(source.getAffectedShiftIndexList()));
        return target;
    }

    /**
     * 复制可空业务日期。
     *
     * @param source 原日期
     * @return 独立日期或空值
     */
    public static Date copyDate(Date source) {
        return source == null ? null : new Date(source.getTime());
    }
}
