/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SKU 选机优先级日志快照。
 *
 * <p>该对象只承载当前选机时点的日志观察数据，不参与机台选择、模具预占、产能扣减或结果写入。
 * {@code orderedCandidates} 可以包含仅因其它 SKU 占用而暂不可选的机台；
 * {@code actualSelectableMachineCodes} 始终只记录正式选机主链已经确认可选的机台。</p>
 *
 * <p>单控整机沿用左侧运行态机台作为代表，{@code displayMachineCodeMap} 和
 * {@code memberMachineCodeMap} 分别保存 {@code K1501L/K1501R} 展示编码及两侧成员，
 * 便于日志汇总两侧实时占用结果，同时不改变正式排产使用的代表机台编码。</p>
 *
 * @author APS
 */
public class MachinePriorityTraceSnapshot {

    /** 日志展示顺序，包含实际可选机台和仅日志展示机台。 */
    private final List<MachineScheduleDTO> orderedCandidates;

    /** 正式选机主链在当前时点确认的实际可选机台编码。 */
    private final Set<String> actualSelectableMachineCodes;

    /** 本轮正式选机主链确定的首选候选机台编码。 */
    private final String actualSelectedMachineCode;

    /** 本轮最终形成有效排程结果的实际命中机台编码。 */
    private final String actualHitMachineCode;

    /**
     * 本轮选机结果：null-尚未完成结果确认；true-实际命中；false-最终未命中。
     *
     * <p>快照在候选选择时创建，因此初始值必须为 null；只有排程结果和机台占用关系
     * 真正提交后才能标记命中，窗口最终收口时才能标记未命中。</p>
     */
    private final Boolean selectionSucceeded;

    /** 最终未命中原因；命中或尚未确认时为空。 */
    private final String noHitReason;

    /** 代表机台编码到日志展示编码的映射。 */
    private final Map<String, String> displayMachineCodeMap;

    /** 代表机台编码到物理成员机台编码的映射。 */
    private final Map<String, List<String>> memberMachineCodeMap;

    /**
     * 代表机台在本次选机时点的占用明细。
     *
     * <p>选机日志采用延迟写入，若写入时再次读取运行态分配结果，会把本轮刚生成的排程结果
     * 误记成“前序占用”。因此占用文本必须随候选快照一起冻结。</p>
     */
    private final Map<String, String> occupationTextMap;

    /**
     * 代表机台在本次选机时点用于日志展示的收尾时间。
     *
     * <p>新增排产会在候选试排成功后先更新机台运行态，再延迟写入选机日志。如果日志写入时
     * 重新读取机台参考时间，会把本轮新增结果的完工时间误当成选机前收尾时间。因此该时间
     * 必须与占用明细一起在候选确定时冻结，且只服务日志展示，不参与正式排序或产能计算。</p>
     */
    private final Map<String, Date> priorityTraceEndingTimeMap;

    /**
     * 创建只包含正式候选的兼容快照。
     *
     * <p>非默认机台匹配策略和测试替身可以直接使用该入口，原有日志行为不受诊断扩展影响。</p>
     *
     * @param orderedCandidates 正式选机有序候选
     * @param actualSelectedMachine 本轮首选候选机台
     * @return 只包含正式候选的日志快照
     */
    public static MachinePriorityTraceSnapshot fromActualCandidates(
            List<MachineScheduleDTO> orderedCandidates,
            MachineScheduleDTO actualSelectedMachine) {
        List<MachineScheduleDTO> safeCandidates = CollectionUtils.isEmpty(orderedCandidates)
                ? Collections.<MachineScheduleDTO>emptyList()
                : orderedCandidates;
        Set<String> actualMachineCodes = new LinkedHashSet<String>(Math.max(4, safeCandidates.size() * 2));
        Map<String, String> displayCodeMap = new LinkedHashMap<String, String>(
                Math.max(4, safeCandidates.size() * 2));
        Map<String, List<String>> memberCodeMap = new LinkedHashMap<String, List<String>>(
                Math.max(4, safeCandidates.size() * 2));
        for (MachineScheduleDTO candidate : safeCandidates) {
            if (Objects.isNull(candidate) || StringUtils.isEmpty(candidate.getMachineCode())) {
                continue;
            }
            String machineCode = candidate.getMachineCode();
            actualMachineCodes.add(machineCode);
            displayCodeMap.put(machineCode, machineCode);
            memberCodeMap.put(machineCode, Collections.singletonList(machineCode));
        }
        return new MachinePriorityTraceSnapshot(
                safeCandidates,
                actualMachineCodes,
                Objects.isNull(actualSelectedMachine) ? null : actualSelectedMachine.getMachineCode(),
                displayCodeMap,
                memberCodeMap,
                Collections.<String, String>emptyMap(),
                Collections.<String, Date>emptyMap());
    }

    /**
     * 创建完整日志快照。
     *
     * @param orderedCandidates 日志展示顺序
     * @param actualSelectableMachineCodes 实际可选机台编码
     * @param actualSelectedMachineCode 实际首选机台编码
     * @param displayMachineCodeMap 展示编码映射
     * @param memberMachineCodeMap 物理成员编码映射
     */
    public MachinePriorityTraceSnapshot(
            List<MachineScheduleDTO> orderedCandidates,
            Set<String> actualSelectableMachineCodes,
            String actualSelectedMachineCode,
            Map<String, String> displayMachineCodeMap,
            Map<String, List<String>> memberMachineCodeMap) {
        this(orderedCandidates, actualSelectableMachineCodes, actualSelectedMachineCode,
                displayMachineCodeMap, memberMachineCodeMap,
                Collections.<String, String>emptyMap(),
                Collections.<String, Date>emptyMap());
    }

    /**
     * 创建包含选机时点占用明细的完整日志快照。
     *
     * @param orderedCandidates 日志展示顺序
     * @param actualSelectableMachineCodes 实际可选机台编码
     * @param actualSelectedMachineCode 首选候选机台编码
     * @param displayMachineCodeMap 展示编码映射
     * @param memberMachineCodeMap 物理成员编码映射
     * @param occupationTextMap 选机时点占用明细
     */
    public MachinePriorityTraceSnapshot(
            List<MachineScheduleDTO> orderedCandidates,
            Set<String> actualSelectableMachineCodes,
            String actualSelectedMachineCode,
            Map<String, String> displayMachineCodeMap,
            Map<String, List<String>> memberMachineCodeMap,
            Map<String, String> occupationTextMap) {
        this(orderedCandidates, actualSelectableMachineCodes, actualSelectedMachineCode,
                displayMachineCodeMap, memberMachineCodeMap, occupationTextMap,
                Collections.<String, Date>emptyMap(),
                null, null, null);
    }

    /**
     * 创建同时冻结占用明细和日志收尾时间的完整快照。
     *
     * @param orderedCandidates 日志展示顺序
     * @param actualSelectableMachineCodes 实际可选机台编码
     * @param actualSelectedMachineCode 首选候选机台编码
     * @param displayMachineCodeMap 展示编码映射
     * @param memberMachineCodeMap 物理成员编码映射
     * @param occupationTextMap 选机时点占用明细
     * @param priorityTraceEndingTimeMap 选机时点日志收尾时间
     */
    public MachinePriorityTraceSnapshot(
            List<MachineScheduleDTO> orderedCandidates,
            Set<String> actualSelectableMachineCodes,
            String actualSelectedMachineCode,
            Map<String, String> displayMachineCodeMap,
            Map<String, List<String>> memberMachineCodeMap,
            Map<String, String> occupationTextMap,
            Map<String, Date> priorityTraceEndingTimeMap) {
        this(orderedCandidates, actualSelectableMachineCodes, actualSelectedMachineCode,
                displayMachineCodeMap, memberMachineCodeMap, occupationTextMap,
                priorityTraceEndingTimeMap,
                null, null, null);
    }

    /**
     * 创建带选机结果的完整日志快照。
     *
     * @param orderedCandidates 日志展示顺序
     * @param actualSelectableMachineCodes 实际可选机台编码
     * @param actualSelectedMachineCode 首选候选机台编码
     * @param displayMachineCodeMap 展示编码映射
     * @param memberMachineCodeMap 物理成员编码映射
     * @param occupationTextMap 选机时点占用明细
     * @param priorityTraceEndingTimeMap 选机时点日志收尾时间
     * @param actualHitMachineCode 实际命中机台编码
     * @param selectionSucceeded 是否实际命中
     * @param noHitReason 最终未命中原因
     */
    private MachinePriorityTraceSnapshot(
            List<MachineScheduleDTO> orderedCandidates,
            Set<String> actualSelectableMachineCodes,
            String actualSelectedMachineCode,
            Map<String, String> displayMachineCodeMap,
            Map<String, List<String>> memberMachineCodeMap,
            Map<String, String> occupationTextMap,
            Map<String, Date> priorityTraceEndingTimeMap,
            String actualHitMachineCode,
            Boolean selectionSucceeded,
            String noHitReason) {
        /*
         * 新增排产在候选命中后会原地推进 MachineScheduleDTO 的前物料、前规格、英寸和收尾时间。
         * 选机日志属于延迟写入，如果这里只复制 List 容器，列表中的可变 DTO 仍会被本轮结果污染，
         * 最终把“选机前画像”错误记录为“选机后画像”。因此候选 DTO 必须在选机时点逐字段冻结。
         */
        this.orderedCandidates = Collections.unmodifiableList(
                copyTraceCandidates(orderedCandidates));
        this.actualSelectableMachineCodes = Collections.unmodifiableSet(
                new LinkedHashSet<String>(
                        CollectionUtils.isEmpty(actualSelectableMachineCodes)
                                ? Collections.<String>emptySet()
                                : actualSelectableMachineCodes));
        this.actualSelectedMachineCode = actualSelectedMachineCode;
        this.actualHitMachineCode = actualHitMachineCode;
        this.selectionSucceeded = selectionSucceeded;
        this.noHitReason = noHitReason;
        this.displayMachineCodeMap = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(
                        CollectionUtils.isEmpty(displayMachineCodeMap)
                                ? Collections.<String, String>emptyMap()
                                : displayMachineCodeMap));
        this.memberMachineCodeMap = Collections.unmodifiableMap(
                copyMemberMachineCodeMap(memberMachineCodeMap));
        this.occupationTextMap = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(
                        CollectionUtils.isEmpty(occupationTextMap)
                                ? Collections.<String, String>emptyMap()
                                : occupationTextMap));
        this.priorityTraceEndingTimeMap = Collections.unmodifiableMap(
                copyPriorityTraceEndingTimeMap(priorityTraceEndingTimeMap));
    }

    /**
     * 深复制物理成员编码映射，防止日志构建后被调用方集合修改。
     *
     * @param sourceMap 原始成员编码映射
     * @return 不共享可变列表的映射
     */
    private static Map<String, List<String>> copyMemberMachineCodeMap(
            Map<String, List<String>> sourceMap) {
        if (CollectionUtils.isEmpty(sourceMap)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> copiedMap = new LinkedHashMap<String, List<String>>(
                Math.max(4, sourceMap.size() * 2));
        for (Map.Entry<String, List<String>> entry : sourceMap.entrySet()) {
            List<String> memberCodes = CollectionUtils.isEmpty(entry.getValue())
                    ? Collections.<String>emptyList()
                    : entry.getValue();
            copiedMap.put(entry.getKey(), Collections.unmodifiableList(
                    new ArrayList<String>(memberCodes)));
        }
        return copiedMap;
    }

    /**
     * 深复制选机日志候选列表。
     *
     * <p>该复制只服务日志观察，不参与正式选机。数组、日期和列表均切断与运行态机台对象的
     * 可变引用，保证延迟日志稳定还原选机发生时的真实机台画像。</p>
     *
     * @param sourceCandidates 选机时点候选列表
     * @return 不与运行态机台对象共享可变字段的候选列表
     */
    private static List<MachineScheduleDTO> copyTraceCandidates(
            List<MachineScheduleDTO> sourceCandidates) {
        if (CollectionUtils.isEmpty(sourceCandidates)) {
            return new ArrayList<MachineScheduleDTO>(0);
        }
        List<MachineScheduleDTO> copiedCandidates =
                new ArrayList<MachineScheduleDTO>(sourceCandidates.size());
        for (MachineScheduleDTO sourceCandidate : sourceCandidates) {
            if (Objects.nonNull(sourceCandidate)) {
                copiedCandidates.add(copyTraceCandidate(sourceCandidate));
            }
        }
        return copiedCandidates;
    }

    /**
     * 复制单台候选机台的日志观察字段。
     *
     * @param sourceCandidate 运行态候选机台
     * @return 选机时点独立快照
     */
    private static MachineScheduleDTO copyTraceCandidate(MachineScheduleDTO sourceCandidate) {
        MachineScheduleDTO copiedCandidate = new MachineScheduleDTO();
        copiedCandidate.setMachineCode(sourceCandidate.getMachineCode());
        copiedCandidate.setMachineName(sourceCandidate.getMachineName());
        copiedCandidate.setMaxMoldNum(sourceCandidate.getMaxMoldNum());
        copiedCandidate.setStatus(sourceCandidate.getStatus());
        copiedCandidate.setDimensionMinimum(sourceCandidate.getDimensionMinimum());
        copiedCandidate.setDimensionMaximum(sourceCandidate.getDimensionMaximum());
        copiedCandidate.setShellStandard(sourceCandidate.getShellStandard());
        copiedCandidate.setSupport195WideBase(sourceCandidate.getSupport195WideBase());
        copiedCandidate.setSupport225WideBase(sourceCandidate.getSupport225WideBase());
        copiedCandidate.setSupportChipTire(sourceCandidate.getSupportChipTire());
        copiedCandidate.setMachineOrder(sourceCandidate.getMachineOrder());
        copiedCandidate.setCurrentMaterialCode(sourceCandidate.getCurrentMaterialCode());
        copiedCandidate.setCurrentMaterialDesc(sourceCandidate.getCurrentMaterialDesc());
        copiedCandidate.setPreviousMaterialCode(sourceCandidate.getPreviousMaterialCode());
        copiedCandidate.setPreviousMaterialDesc(sourceCandidate.getPreviousMaterialDesc());
        copiedCandidate.setPreviousSpecCode(sourceCandidate.getPreviousSpecCode());
        copiedCandidate.setPreviousProSize(sourceCandidate.getPreviousProSize());
        copiedCandidate.setEnding(sourceCandidate.isEnding());
        copiedCandidate.setStructureEndingAligned(sourceCandidate.isStructureEndingAligned());
        copiedCandidate.setEstimatedEndTime(copyDate(sourceCandidate.getEstimatedEndTime()));
        copiedCandidate.setNextMaterialCode(sourceCandidate.getNextMaterialCode());
        copiedCandidate.setShiftRemainingCapacity(
                Objects.isNull(sourceCandidate.getShiftRemainingCapacity())
                        ? null : sourceCandidate.getShiftRemainingCapacity().clone());
        copiedCandidate.setShiftAvailable(
                Objects.isNull(sourceCandidate.getShiftAvailable())
                        ? null : sourceCandidate.getShiftAvailable().clone());
        copiedCandidate.setPlanStopStartTime(copyDate(sourceCandidate.getPlanStopStartTime()));
        copiedCandidate.setPlanStopEndTime(copyDate(sourceCandidate.getPlanStopEndTime()));
        copiedCandidate.setStopType(sourceCandidate.getStopType());
        copiedCandidate.setHasDryIceCleaning(sourceCandidate.isHasDryIceCleaning());
        copiedCandidate.setHasSandBlastCleaning(sourceCandidate.isHasSandBlastCleaning());
        copiedCandidate.setCleaningPlanTime(copyDate(sourceCandidate.getCleaningPlanTime()));
        copiedCandidate.setCleaningWindowList(
                CollectionUtils.isEmpty(sourceCandidate.getCleaningWindowList())
                        ? new ArrayList<>(0)
                        : new ArrayList<>(sourceCandidate.getCleaningWindowList()));
        copiedCandidate.setHasMaintenancePlan(sourceCandidate.isHasMaintenancePlan());
        copiedCandidate.setMaintenancePlanTime(copyDate(sourceCandidate.getMaintenancePlanTime()));
        copiedCandidate.setMaintenanceWindowList(
                CollectionUtils.isEmpty(sourceCandidate.getMaintenanceWindowList())
                        ? new ArrayList<>(0)
                        : new ArrayList<>(sourceCandidate.getMaintenanceWindowList()));
        copiedCandidate.setHasRepairPlan(sourceCandidate.isHasRepairPlan());
        copiedCandidate.setRepairPlanTime(copyDate(sourceCandidate.getRepairPlanTime()));
        copiedCandidate.setCapsuleUsageCount(sourceCandidate.getCapsuleUsageCount());
        copiedCandidate.setCapsuleUsageCount2(sourceCandidate.getCapsuleUsageCount2());
        copiedCandidate.setMouldChangeTasks(
                CollectionUtils.isEmpty(sourceCandidate.getMouldChangeTasks())
                        ? new ArrayList<>(0)
                        : new ArrayList<>(sourceCandidate.getMouldChangeTasks()));
        return copiedCandidate;
    }

    /**
     * 防御性复制日期。
     *
     * @param sourceDate 原日期
     * @return 独立日期实例；原日期为空时返回 null
     */
    private static Date copyDate(Date sourceDate) {
        return Objects.isNull(sourceDate) ? null : new Date(sourceDate.getTime());
    }

    /**
     * 深复制日志收尾时间映射，防止调用方修改可变 {@link Date} 对象污染已冻结快照。
     *
     * @param sourceMap 原始日志收尾时间映射
     * @return 与调用方不共享 Date 实例的映射
     */
    private static Map<String, Date> copyPriorityTraceEndingTimeMap(
            Map<String, Date> sourceMap) {
        if (CollectionUtils.isEmpty(sourceMap)) {
            return Collections.emptyMap();
        }
        Map<String, Date> copiedMap = new LinkedHashMap<String, Date>(
                Math.max(4, sourceMap.size() * 2));
        for (Map.Entry<String, Date> entry : sourceMap.entrySet()) {
            Date endingTime = entry.getValue();
            copiedMap.put(
                    entry.getKey(),
                    Objects.isNull(endingTime)
                            ? null : new Date(endingTime.getTime()));
        }
        return copiedMap;
    }

    /**
     * 获取日志展示顺序。
     *
     * @return 只读候选列表
     */
    public List<MachineScheduleDTO> getOrderedCandidates() {
        return Collections.unmodifiableList(copyTraceCandidates(orderedCandidates));
    }

    /**
     * 获取指定机台在选机时点冻结的候选画像。
     *
     * <p>TOP5 日志和详细优先级日志必须统一读取该快照，禁止在排产提交后再次读取已经推进的
     * 运行态 {@link MachineScheduleDTO}，否则会把本轮物料误写成前物料并产生同规格、同英寸假象。</p>
     *
     * @param machineCode 代表机台编码
     * @return 防御性复制后的选机时点候选；快照中不存在时返回 null
     */
    public MachineScheduleDTO resolveCandidateSnapshot(String machineCode) {
        if (StringUtils.isEmpty(machineCode)) {
            return null;
        }
        for (MachineScheduleDTO candidate : orderedCandidates) {
            if (Objects.nonNull(candidate)
                    && StringUtils.equals(machineCode, candidate.getMachineCode())) {
                return copyTraceCandidate(candidate);
            }
        }
        return null;
    }

    /**
     * 判断代表机台是否属于正式可选集合。
     *
     * @param machineCode 代表机台编码
     * @return true-正式可选；false-仅日志展示
     */
    public boolean isActualSelectable(String machineCode) {
        return StringUtils.isNotEmpty(machineCode)
                && actualSelectableMachineCodes.contains(machineCode);
    }

    /**
     * 判断代表机台是否为本轮实际首选。
     *
     * @param machineCode 代表机台编码
     * @return true-本轮实际首选
     */
    public boolean isActualSelected(String machineCode) {
        return StringUtils.isNotEmpty(machineCode)
                && StringUtils.equals(actualSelectedMachineCode, machineCode);
    }

    /**
     * 判断代表机台是否为本轮最终实际命中机台。
     *
     * @param machineCode 代表机台编码
     * @return true-本轮最终形成有效排程结果
     */
    public boolean isActualHit(String machineCode) {
        return StringUtils.isNotEmpty(machineCode)
                && StringUtils.equals(actualHitMachineCode, machineCode);
    }

    /**
     * 获取本轮首选候选机台编码。
     *
     * @return 实际首选机台编码
     */
    public String getActualSelectedMachineCode() {
        return actualSelectedMachineCode;
    }

    /**
     * 获取本轮实际命中机台编码。
     *
     * @return 实际命中代表机台编码；未命中或尚未确认时为空
     */
    public String getActualHitMachineCode() {
        return actualHitMachineCode;
    }

    /**
     * 判断本轮选机结果是否已经确认。
     *
     * @return true-已经确认命中或未命中；false-仍是选机时点的待确认快照
     */
    public boolean isSelectionOutcomeResolved() {
        return Objects.nonNull(selectionSucceeded);
    }

    /**
     * 判断本轮是否实际命中。
     *
     * @return true-实际命中；false-未命中或尚未确认
     */
    public boolean isSelectionSucceeded() {
        return Boolean.TRUE.equals(selectionSucceeded);
    }

    /**
     * 获取最终未命中原因。
     *
     * @return 未命中原因；命中或尚未确认时为空
     */
    public String getNoHitReason() {
        return noHitReason;
    }

    /**
     * 基于当前选机时点快照生成实际命中快照。
     *
     * <p>调用方只能在排程结果、机台占用和跨日在机绑定全部提交成功后调用，
     * 防止把仅进入换模、首检或 dayN 试算的候选误记成实际命中机台。</p>
     *
     * @param actualHitMachineCode 实际命中代表机台编码
     * @return 保留原候选观察范围并标记命中的新快照
     */
    public MachinePriorityTraceSnapshot withActualHit(String actualHitMachineCode) {
        return new MachinePriorityTraceSnapshot(
                orderedCandidates, actualSelectableMachineCodes, actualSelectedMachineCode,
                displayMachineCodeMap, memberMachineCodeMap, occupationTextMap,
                priorityTraceEndingTimeMap,
                actualHitMachineCode, Boolean.TRUE, null);
    }

    /**
     * 基于当前选机时点快照生成最终未命中快照。
     *
     * @param noHitReason 三天排程窗口最终未命中原因
     * @return 保留最后一次有效观察范围并标记未命中的新快照
     */
    public MachinePriorityTraceSnapshot withNoHit(String noHitReason) {
        return new MachinePriorityTraceSnapshot(
                orderedCandidates, actualSelectableMachineCodes, actualSelectedMachineCode,
                displayMachineCodeMap, memberMachineCodeMap, occupationTextMap,
                priorityTraceEndingTimeMap,
                null, Boolean.FALSE, noHitReason);
    }

    /**
     * 获取日志展示编码。
     *
     * @param representativeMachineCode 代表机台编码
     * @return 单控整机组合编码或原机台编码
     */
    public String resolveDisplayMachineCode(String representativeMachineCode) {
        return displayMachineCodeMap.getOrDefault(
                representativeMachineCode, representativeMachineCode);
    }

    /**
     * 获取日志占用汇总需要读取的物理成员机台编码。
     *
     * @param representativeMachineCode 代表机台编码
     * @return 单控整机返回 L/R 两侧，其余返回自身
     */
    public List<String> resolveMemberMachineCodes(String representativeMachineCode) {
        List<String> memberMachineCodes = memberMachineCodeMap.get(representativeMachineCode);
        if (CollectionUtils.isEmpty(memberMachineCodes)) {
            return StringUtils.isEmpty(representativeMachineCode)
                    ? Collections.<String>emptyList()
                    : Collections.singletonList(representativeMachineCode);
        }
        return memberMachineCodes;
    }

    /**
     * 获取本次选机时点冻结的机台占用明细。
     *
     * @param representativeMachineCode 代表机台编码
     * @return 已聚合并稳定排序的占用 SKU；选机时点没有占用时返回“无”
     */
    public String resolveOccupationText(String representativeMachineCode) {
        String occupationText = occupationTextMap.get(representativeMachineCode);
        return StringUtils.isEmpty(occupationText) ? "无" : occupationText;
    }

    /**
     * 判断快照是否已冻结指定机台的日志收尾时间。
     *
     * <p>兼容旧日志入口创建的快照不含该映射，调用方可据此回落到原有实时解析逻辑；
     * 新增排产正式主链创建的完整快照必须命中该映射。</p>
     *
     * @param representativeMachineCode 代表机台编码
     * @return true-已经在选机时点冻结日志收尾时间
     */
    public boolean hasPriorityTraceEndingTime(String representativeMachineCode) {
        return StringUtils.isNotEmpty(representativeMachineCode)
                && priorityTraceEndingTimeMap.containsKey(representativeMachineCode);
    }

    /**
     * 获取选机时点冻结的日志收尾时间。
     *
     * @param representativeMachineCode 代表机台编码
     * @return 防御性复制后的冻结时间；选机时点无有效时间时返回 null
     */
    public Date resolvePriorityTraceEndingTime(String representativeMachineCode) {
        Date endingTime = priorityTraceEndingTimeMap.get(representativeMachineCode);
        return Objects.isNull(endingTime)
                ? null : new Date(endingTime.getTime());
    }
}
