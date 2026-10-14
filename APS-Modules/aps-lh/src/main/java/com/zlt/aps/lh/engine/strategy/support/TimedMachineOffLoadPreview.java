package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 有界预演的事件负荷；联合决策使用独立副本迁移动作，不写正式次数账本。
 *
 * @author APS
 * @date 2026/10/13
 */
public final class TimedMachineOffLoadPreview {
    /**
     * 日期到早中班预计次数。
     */
    private final Map<String, int[]> dailyCounts;
    /**
     * 以完整原动作内核复核固定后料，返回合法开始时间。
     */
    private ActionRevalidator actionRevalidator;

    /**
     * 时间下机机台自己已被预演计入的动作，判断该机台时扣除，防止日满额自阻塞。
     */
    private final Map<String, Map<String, int[]>> ownCounts;
    /**
     * 真实预演成功的换模事件，只有可调整请求的首次交接可以迁移。
     */
    private final List<ChangeoverEvent> events = new ArrayList<>();
    /**
     * 物理下机机台、日期到已验证的同结构接替原因。
     */
    private final Map<String, Map<LocalDate, String>> structureHandoffs = new LinkedHashMap<>();

    /**
     * 创建独立预演负荷。
     *
     * @param dailyCounts 预演次数
     * @param ownCounts 各物理机台自己的动作次数
     */
    public TimedMachineOffLoadPreview(Map<String, int[]> dailyCounts,
            Map<String, Map<String, int[]>> ownCounts) {
        this.dailyCounts = this.copyCounts(dailyCounts);
        this.ownCounts = new LinkedHashMap<>();
        ownCounts.forEach((machine, counts) -> this.ownCounts.put(machine, this.copyCounts(counts)));
    }

    /**
     * 创建独立预演负荷。
     *
     * @param dailyCounts 完整模拟次数
     * @param events 成功事件
     * @param structureHandoffs 同结构接替证据
     */
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

    /**
     * 复制可独立迁移的模拟账本。
     *
     * @return 独立决策账本，同一预演再次使用时不得继承上次动作迁移
     */
    public TimedMachineOffLoadPreview copyForDecision() {
        TimedMachineOffLoadPreview copied = new TimedMachineOffLoadPreview(dailyCounts, ownCounts);
        events.forEach(event -> copied.events.add(new ChangeoverEvent(event)));
        structureHandoffs.forEach((machine, reasons) ->
                copied.structureHandoffs.put(machine, new LinkedHashMap<>(reasons)));
        copied.actionRevalidator = actionRevalidator;
        return copied;
    }

    /**
     * 查询已验证的结构接替原因。
     *
     * @param machineCodes 同一物理机的两侧
     * @param date 判断日
     * @return 预演已验证的接替原因，无需求时为空
     */
    public String structureHandoffReason(Collection<String> machineCodes, LocalDate date) {
        return machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .map(machine -> structureHandoffs.getOrDefault(machine, Collections.emptyMap()).get(date))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * 判断是否存在已验证结构接替。
     *
     * @param machineCodes 同一物理机的两侧
     * @return 窗口内是否存在已验证的接替需求
     */
    public boolean hasStructureHandoff(Collection<String> machineCodes) {
        return machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .anyMatch(machine -> !structureHandoffs.getOrDefault(machine, Collections.emptyMap()).isEmpty());
    }

    /**
     * 创建可定向重评的完整预演负荷。
     *
     * @param dailyCounts 全部动作次数
     * @param events 已成功动作
     * @param structureHandoffs 同结构接替证据
     * @param actionRevalidator 完整原组合重评内核
     */
    public TimedMachineOffLoadPreview(Map<String, int[]> dailyCounts, List<ChangeoverEvent> events,
            Map<String, Map<LocalDate, String>> structureHandoffs, ActionRevalidator actionRevalidator) {
        this(dailyCounts, events, structureHandoffs);
        this.actionRevalidator = Objects.requireNonNull(actionRevalidator, "完整动作重评内核不能为空");
    }

    /**
     * 用精确释放事实重评首次交接，只根据真实合法动作时间更新模拟负荷。
     * 先从独立副本排除当前及前缀预算，前缀按原组合重建真实资源后再校验目标；其他动作不迁移。
     *
     * @param context 当前时间下机上下文
     * @param machineCodes 同一物理机台两侧
     * @param decision 当前已完成实际排量的释放事实
     * @return 动作时间迁移明细
     */
    public String moveOwnActions(LhScheduleContext context, Collection<String> machineCodes,
            OffMachineDecision decision) {
        TimedMachineOffLoadPreview working = this.copyForDecision();
        String movements = working.revalidateOwnActions(context, machineCodes, decision);
        // 全部返回时刻和预算校验通过后才原子替换；异常不能留下部分前缀迁移。
        dailyCounts.clear();
        dailyCounts.putAll(working.dailyCounts);
        ownCounts.clear();
        ownCounts.putAll(working.ownCounts);
        for (int index = 0; index < events.size(); index++) {
            events.get(index).date = working.events.get(index).date;
            events.get(index).slot = working.events.get(index).slot;
        }
        return movements;
    }

    /**
     * 在独立账本中重评请求及其完整固定前缀，禁止遗漏已从预算排除的动作。
     *
     * @param context 当前上下文
     * @param machineCodes 请求运行态编码
     * @param decision 已确认的实际释放
     * @return 独立副本的迁移明细
     */
    private String revalidateOwnActions(LhScheduleContext context, Collection<String> machineCodes,
            OffMachineDecision decision) {
        Set<String> physicalCodes = machineCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .collect(Collectors.toSet());
        List<String> movements = new ArrayList<>();
        for (ChangeoverEvent event : events) {
            if (!event.movable || !physicalCodes.contains(event.physicalMachineCode)) {
                continue;
            }
            Map<TimedMachineOffActionPreviewCapture.PreparedAction, ChangeoverEvent> prefix = new IdentityHashMap<>();
            Map<String, int[]> simulatedCounts = this.copyCounts(dailyCounts);
            for (ChangeoverEvent budget : events) {
                boolean beforeTarget = event.preparedAction != null && budget.preparedAction != null
                        && budget.preparedAction.getSequence() < event.preparedAction.getSequence();
                if (beforeTarget && prefix.put(budget.preparedAction, budget) != null) {
                    throw new IllegalStateException("时间下机前缀重复登记同一完整动作");
                }
                if ((budget == event || beforeTarget) && budget.date != null) {
                    simulatedCounts.get(budget.date.toString())[budget.slot]--;
                }
            }
            this.validateNonnegativeCounts(simulatedCounts);
            ActionRevalidationResult result;
            if (decision.getOffMachineTime() == null) {
                // 未释放时没有执行前缀重评，只撤销本机首次事件，其他预算保持原位。
                prefix.clear();
                result = ActionRevalidationResult.targetOnly(null);
            } else {
                result = Objects.requireNonNull(Objects.requireNonNull(actionRevalidator,
                        "时间下机成功事件缺少完整重评内核").revalidate(
                        event, decision.getOffMachineTime(), simulatedCounts), "完整动作重评必须返回显式结果");
            }
            if (!prefix.keySet().equals(result.prefixActionTimes.keySet())
                    || !prefix.keySet().equals(result.prefixReleaseTimes.keySet())) {
                throw new IllegalStateException("时间下机重评结果必须完整覆盖已排除预算的固定前缀");
            }
            Map<ChangeoverEvent, Date> starts = new IdentityHashMap<>();
            starts.put(event, result.targetActionTime);
            this.validateActionTime(context, result.targetActionTime, decision.getOffMachineTime());
            prefix.forEach((action, prior) -> {
                Date start = result.prefixActionTimes.get(action);
                this.validateActionTime(context, start, result.prefixReleaseTimes.get(action));
                starts.put(prior, start);
            });
            // 先整体撤销旧预算再加入真实结果，避免同日互换时出现中间态重复占用。
            starts.keySet().forEach(changed -> this.changeEventCount(changed, -1));
            starts.forEach((changed, start) -> {
                LocalDate date = start == null ? null
                        : start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                int slot = changed.slot;
                if (start != null) {
                    slot = LhScheduleTimeUtil.isMorningShift(context, start) ? 0 : 1;
                }
                if (!Objects.equals(changed.date, date) || changed.slot != slot) {
                    movements.add(changed.physicalMachineCode + "/" + changed.materialCode + "/" + changed.productStatus
                            + ":" + changed.date + "/" + changed.slot + "->" + date + "/" + slot);
                }
                changed.date = date;
                changed.slot = slot;
                this.changeEventCount(changed, 1);
            });
            this.validateNonnegativeCounts(dailyCounts);
            this.validateQuotaLimits(context, dailyCounts);
            ownCounts.values().forEach(this::validateNonnegativeCounts);
        }
        return String.join(";", movements);
    }

    /**
     * 校验完整内核返回的动作没有提前穿越真实释放，也没有落到夜班。
     *
     * @param context 班次配置
     * @param start 成功动作时刻，准入失败为空
     * @param release 本次重放使用的实际可交接时刻
     */
    private void validateActionTime(LhScheduleContext context, Date start, Date release) {
        if (start == null) {
            return;
        }
        if (release == null || start.before(release)) {
            throw new IllegalStateException("后料动作不得早于本次重放的真实释放");
        }
        if (!LhScheduleTimeUtil.isMorningShift(context, start)
                && !LhScheduleTimeUtil.isAfternoonShift(context, start)) {
            throw new IllegalStateException("完整后料重评返回了非法换模班次");
        }
    }

    /**
     * 校验预算扣除与事件数量一致，重复扣除必须显式失败。
     *
     * @param counts 待验证的独立日期计数
     */
    private void validateNonnegativeCounts(Map<String, int[]> counts) {
        if (counts.values().stream().anyMatch(count -> count[0] < 0 || count[1] < 0)) {
            throw new IllegalStateException("时间下机模拟动作预算出现负数");
        }
    }

    /**
     * 复核原子合并后的每日与班次硬额度，防止前缀和未重评未来预约相互超占。
     *
     * @param context 当天有效限额及历史额度快照
     * @param counts 全部动作合并后的独立计数
     */
    private void validateQuotaLimits(LhScheduleContext context, Map<String, int[]> counts) {
        counts.forEach((date, count) -> {
            if (count[0] > LhScheduleTimeUtil.getMorningMouldChangeLimit(context, date)
                    || count[1] > LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, date)
                    || (long) count[0] + count[1] > LhScheduleTimeUtil.getDailyMouldChangeLimit(context, date)) {
                throw new IllegalStateException("时间下机完整重评结果超过当日有效换模额度：" + date);
            }
        });
    }

    /**
     * 一次完整重评的目标与固定前缀结果，身份映射保留失败动作的空时间。
     */
    public static final class ActionRevalidationResult {
        /**
         * 目标合法开始时间，完整准入失败为空。
         */
        private final Date targetActionTime;
        /**
         * 每个已重评前缀动作的合法开始时间，失败值为空。
         */
        private final Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> prefixActionTimes;
        /**
         * 每个前缀本次重放时使用的真实可交接时间。
         */
        private final Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> prefixReleaseTimes;

        /**
         * 创建不共享可变日期对象的完整重评结果。
         *
         * @param targetActionTime 目标合法开始时间，可为空
         * @param prefixActionTimes 全部已重评前缀结果
         * @param prefixReleaseTimes 同一前缀身份集合的真实释放时间
         */
        public ActionRevalidationResult(Date targetActionTime,
                Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> prefixActionTimes,
                Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> prefixReleaseTimes) {
            this.targetActionTime = targetActionTime == null ? null : new Date(targetActionTime.getTime());
            this.prefixActionTimes = this.copyTimes(prefixActionTimes);
            this.prefixReleaseTimes = this.copyTimes(prefixReleaseTimes);
        }

        /**
         * 创建没有重评前缀的明确目标结果。
         *
         * @param start 目标合法时刻，失败或未释放为空
         * @return 只包含目标的完整结果
         */
        public static ActionRevalidationResult targetOnly(Date start) {
            return new ActionRevalidationResult(start, Collections.emptyMap(), Collections.emptyMap());
        }

        /**
         * 读取目标开始时间副本。
         *
         * @return 目标合法时刻，失败为空
         */
        public Date getTargetActionTime() {
            return targetActionTime == null ? null : new Date(targetActionTime.getTime());
        }

        /**
         * 读取前缀动作结果副本。
         *
         * @return 按原组合身份关联的动作时刻
         */
        public Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> getPrefixActionTimes() {
            return this.copyTimes(prefixActionTimes);
        }

        /**
         * 读取前缀真实释放时刻副本。
         *
         * @return 按原组合身份关联的释放时刻
         */
        public Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> getPrefixReleaseTimes() {
            return this.copyTimes(prefixReleaseTimes);
        }

        /**
         * 保留身份键并复制可变日期，失败动作的空值必须保留。
         *
         * @param source 原结果映射
         * @return 不与原映射共享日期的身份映射
         */
        private Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> copyTimes(
                Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> source) {
            Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> copied = new IdentityHashMap<>();
            source.forEach((action, time) -> copied.put(action, time == null ? null : new Date(time.getTime())));
            return copied;
        }
    }

    /**
     * 已确定的唯一组合重评契约，调用方负责隔离恢复上下文。
     */
    @FunctionalInterface
    public interface ActionRevalidator {
        /**
         * 在排除自身及固定组合前缀预算的模拟次数上复核合法动作时间。
         *
         * @param event 原成功组合
         * @param release 新物理释放时间
         * @param simulatedCounts 已去自身和前缀预算，重放前缀真实动作后再合并使用的独立次数
         * @return 目标与全部已重评前缀的显式结果，准入失败以对应空时间表示
         */
        ActionRevalidationResult revalidate(ChangeoverEvent event, Date release, Map<String, int[]> simulatedCounts);
    }

    /**
     * 同步增加或撤销首次动作的模拟配额。
     *
     * @param event 首次交接事件
     * @param delta 撤销旧班次或占用新班次，同步总负荷和自身负荷
     */
    private void changeEventCount(ChangeoverEvent event, int delta) {
        if (event.date == null) {
            return;
        }
        String dateKey = event.date.toString();
        dailyCounts.computeIfAbsent(dateKey, key -> new int[2])[event.slot] += delta;
        if (event.movable) {
            ownCounts.computeIfAbsent(event.physicalMachineCode, key -> new LinkedHashMap<>())
                    .computeIfAbsent(dateKey, key -> new int[2])[event.slot] += delta;
        }
    }

    /**
     * 成功事件保留物理机、物料状态及原动作时刻；日期和槽位只在独立模拟账本内更新。
     */
    @Getter
    public static final class ChangeoverEvent {
        /**
         * 实际占用配额的物理机台，L/R合并一次。
         */
        private final String physicalMachineCode;
        /**
         * 后物料。
         */
        private final String materialCode;
        /**
         * 后物料产品状态。
         */
        private final String productStatus;
        /**
         * 原始动作起点，仅用于溯源；提前释放后的合法时间须重新校验。
         */
        private final long originalStartMillis;
        /**
         * 原始配额日期。
         */
        private final LocalDate originalDate;
        /**
         * 原始早中班槽位。
         */
        private final int originalSlot;
        /**
         * 是否为可调整时间下机机台的首次后料事件。
         */
        private final boolean movable;
        /**
         * 同源未消费输入及原动作内核，模拟副本只读共享。
         */
        private TimedMachineOffActionPreviewCapture.PreparedAction preparedAction;
        /**
         * 当前模拟配额日期，为空表示窗口内未释放。
         */
        private LocalDate date;
        /**
         * 当前模拟早中班槽位。
         */
        private int slot;

        /**
         * 创建保留原动作证据的模拟事件。
         *
         * @param machine 物理机
         * @param material 后料
         * @param status 状态
         * @param start 动作起点
         * @param date 动作日期
         * @param slot 早中班槽位
         * @param movable 首次可移动交接
         */
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

        /**
         * 绑定本动作消费前的精确同源输入。
         *
         * @param action 原动作输入描述
         * @return 当前事件
         */
        public ChangeoverEvent withPreparedAction(TimedMachineOffActionPreviewCapture.PreparedAction action) {
            this.preparedAction = Objects.requireNonNull(action, "成功动作输入不能为空");
            return this;
        }

        /**
         * 复制事件的独立日期和槽位。
         *
         * @param source 原事件，复制可变日期和槽位以隔离每次联合决策
         */
        private ChangeoverEvent(ChangeoverEvent source) {
            this(source.physicalMachineCode, source.materialCode, source.productStatus,
                    new Date(source.originalStartMillis), source.originalDate, source.originalSlot, source.movable);
            this.date = source.date;
            this.slot = source.slot;
            this.preparedAction = source.preparedAction;
        }
    }

    /**
     * 取得排除当前物理机首次动作后的模拟负荷。
     *
     * @param date 判断日期
     * @param machineCodes 当前物理机台的运行态编码
     * @return 排除自己动作后的预计次数
     */
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

    /**
     * 深复制每日计数数组。
     *
     * @param source 次数来源
     * @return 不与模拟账本共享数组的快照
     */
    private Map<String, int[]> copyCounts(Map<String, int[]> source) {
        Map<String, int[]> copied = new LinkedHashMap<>();
        source.forEach((date, counts) -> copied.put(date, counts.clone()));
        return copied;
    }
}
