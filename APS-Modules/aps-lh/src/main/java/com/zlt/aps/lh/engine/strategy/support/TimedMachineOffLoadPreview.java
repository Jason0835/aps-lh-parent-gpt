package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 有界预演的事件负荷；联合决策使用独立副本迁移动作，不写正式次数账本。 */
public final class TimedMachineOffLoadPreview {
    /** 日期到早中班预计次数。 */
    private final Map<String, int[]> dailyCounts;
    /** 时间下机机台自己已被预演计入的动作，判断该机台时扣除，防止日满额自阻塞。 */
    private final Map<String, Map<String, int[]>> ownCounts;
    /** 真实预演成功的换模事件，只有可调整请求的首次交接可以迁移。 */
    private final List<ChangeoverEvent> events = new ArrayList<>();
    /** 物理下机机台、日期到已验证的同结构接替原因。 */
    private final Map<String, Map<LocalDate, String>> structureHandoffs = new LinkedHashMap<>();

    /** @param dailyCounts 预演次数 @param ownCounts 各物理机台自己的动作次数 */
    public TimedMachineOffLoadPreview(Map<String, int[]> dailyCounts,
            Map<String, Map<String, int[]>> ownCounts) {
        this.dailyCounts = this.copyCounts(dailyCounts);
        this.ownCounts = new LinkedHashMap<>();
        ownCounts.forEach((machine, counts) -> this.ownCounts.put(machine, this.copyCounts(counts)));
    }

    /** @param dailyCounts 完整模拟次数 @param events 成功事件 @param structureHandoffs 同结构接替证据 */
    public TimedMachineOffLoadPreview(Map<String, int[]> dailyCounts, List<ChangeoverEvent> events,
            Map<String, Map<LocalDate, String>> structureHandoffs) {
        this(dailyCounts, Collections.emptyMap());
        events.forEach(event -> this.events.add(new ChangeoverEvent(event)));
        this.events.stream().filter(ChangeoverEvent::isMovable).forEach(event ->
                ownCounts.computeIfAbsent(event.physicalMachineCode, key -> new LinkedHashMap<>())
                        .computeIfAbsent(event.date.toString(), key -> new int[2])[event.slot]++);
        structureHandoffs.forEach((machine, reasons) ->
                this.structureHandoffs.put(machine, new LinkedHashMap<>(reasons)));
    }

    /** @return 独立决策账本，同一预演再次使用时不得继承上次动作迁移 */
    public TimedMachineOffLoadPreview copyForDecision() {
        TimedMachineOffLoadPreview copied = new TimedMachineOffLoadPreview(dailyCounts, ownCounts);
        events.forEach(event -> copied.events.add(new ChangeoverEvent(event)));
        structureHandoffs.forEach((machine, reasons) ->
                copied.structureHandoffs.put(machine, new LinkedHashMap<>(reasons)));
        return copied;
    }

    /** @param machineCodes 同一物理机的两侧 @param date 判断日 @return 预演已验证的接替原因，无需求时为空 */
    public String structureHandoffReason(Collection<String> machineCodes, LocalDate date) {
        return machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .map(machine -> structureHandoffs.getOrDefault(machine, Collections.emptyMap()).get(date))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /** @param machineCodes 同一物理机的两侧 @return 窗口内是否存在已验证的接替需求 */
    public boolean hasStructureHandoff(Collection<String> machineCodes) {
        return machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .anyMatch(machine -> !structureHandoffs.getOrDefault(machine, Collections.emptyMap()).isEmpty());
    }

    /**
     * 用新的交接边界更新模拟负荷。保留原动作最早开始时间；后续同机动作不随首次下机盲目搬移。
     * 无后料成功事件时不虚构换模，窗口内不释放时撤销该首次事件的模拟占用。
     * @param machineCodes 请求机台及配对侧 @param decision 已校验硬额度和物理交接的下机决策
     * @return 本次迁移的事件明细，供正式过程日志对账
     */
    public String moveOwnActions(Collection<String> machineCodes, OffMachineDecision decision) {
        Set<String> physicalCodes = machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .collect(Collectors.toSet());
        List<String> movements = new ArrayList<>();
        for (ChangeoverEvent event : events) {
            if (!event.movable || !physicalCodes.contains(event.physicalMachineCode)) {
                continue;
            }
            LocalDate targetDate = null;
            int targetSlot = event.originalSlot;
            if (decision.getOffMachineTime() != null) {
                boolean keepOriginal = decision.getOffMachineTime().getTime() <= event.originalStartMillis;
                targetDate = keepOriginal ? event.originalDate : decision.getPlanDate();
                if (!keepOriginal) {
                    targetSlot = ShiftEnum.MORNING_SHIFT.getCode().equals(decision.getShift()) ? 0 : 1;
                }
            }
            if (Objects.equals(event.date, targetDate) && event.slot == targetSlot) {
                continue;
            }
            movements.add(event.physicalMachineCode + "/" + event.materialCode + "/" + event.productStatus
                    + ":" + event.date + "/" + event.slot + "->" + targetDate + "/" + targetSlot);
            this.changeEventCount(event, -1);
            event.date = targetDate;
            event.slot = targetSlot;
            this.changeEventCount(event, 1);
        }
        return String.join(";", movements);
    }

    /** @param event 首次交接事件 @param delta 撤销旧班次或占用新班次，同步总负荷和自身负荷 */
    private void changeEventCount(ChangeoverEvent event, int delta) {
        if (event.date == null) {
            return;
        }
        String dateKey = event.date.toString();
        dailyCounts.computeIfAbsent(dateKey, key -> new int[2])[event.slot] += delta;
        ownCounts.computeIfAbsent(event.physicalMachineCode, key -> new LinkedHashMap<>())
                .computeIfAbsent(dateKey, key -> new int[2])[event.slot] += delta;
    }

    /** 成功事件保留物理机、物料状态及原动作时刻；日期和槽位只在独立模拟账本内更新。 */
    @Getter
    public static final class ChangeoverEvent {
        /** 实际占用配额的物理机台，L/R合并一次。 */
        private final String physicalMachineCode;
        /** 后物料。 */
        private final String materialCode;
        /** 后物料产品状态。 */
        private final String productStatus;
        /** 原始动作起点，迁移不得提前到原可行时间之前。 */
        private final long originalStartMillis;
        /** 原始配额日期。 */
        private final LocalDate originalDate;
        /** 原始早中班槽位。 */
        private final int originalSlot;
        /** 是否为可调整时间下机机台的首次后料事件。 */
        private final boolean movable;
        /** 当前模拟配额日期，为空表示窗口内未释放。 */
        private LocalDate date;
        /** 当前模拟早中班槽位。 */
        private int slot;

        /** @param machine 物理机 @param material 后料 @param status 状态 @param start 动作起点
         * @param date 动作日期 @param slot 早中班槽位 @param movable 首次可移动交接 */
        public ChangeoverEvent(String machine, String material, String status, Date start,
                LocalDate date, int slot, boolean movable) {
            this.physicalMachineCode = machine;
            this.materialCode = material;
            this.productStatus = status;
            this.originalStartMillis = start.getTime();
            this.originalDate = date;
            this.originalSlot = slot;
            this.movable = movable;
            this.date = date;
            this.slot = slot;
        }

        /** @param source 原事件，复制可变日期和槽位以隔离每次联合决策 */
        private ChangeoverEvent(ChangeoverEvent source) {
            this(source.physicalMachineCode, source.materialCode, source.productStatus,
                    new Date(source.originalStartMillis), source.originalDate, source.originalSlot, source.movable);
            this.date = source.date;
            this.slot = source.slot;
        }
    }

    /** @param date 判断日期 @param machineCodes 当前物理机台的运行态编码 @return 排除自己动作后的预计次数 */
    public int[] countsFor(LocalDate date, Collection<String> machineCodes) {
        int[] counts = dailyCounts.getOrDefault(date.toString(), new int[2]).clone();
        machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode).distinct()
                .forEach(machine -> {
                    int[] own = ownCounts.getOrDefault(machine, Collections.emptyMap()).get(date.toString());
                    if (own != null) {
                        counts[0] = Math.max(0, counts[0] - own[0]);
                        counts[1] = Math.max(0, counts[1] - own[1]);
                    }
                });
        return counts;
    }

    /** @param source 次数来源 @return 不与模拟账本共享数组的快照 */
    private Map<String, int[]> copyCounts(Map<String, int[]> source) {
        Map<String, int[]> copied = new LinkedHashMap<>();
        source.forEach((date, counts) -> copied.put(date, counts.clone()));
        return copied;
    }
}
