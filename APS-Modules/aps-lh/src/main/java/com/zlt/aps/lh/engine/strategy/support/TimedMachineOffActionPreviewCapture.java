package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;

import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import java.util.Objects;
import java.util.function.Function;

/**
 * 主负荷预演中按来源身份冻结首次交接的未消费输入，供原动作内核定向复核。
 *
 * @author APS
 * @date 2026/10/13
 */
public final class TimedMachineOffActionPreviewCapture {
    /**
     * 本次允许调整的物理机台范围。
     */
    private final Set<String> physicalCodes;
    /**
     * 主预演之前已存在的来源身份，重放时继续消费当前对象和账本。
     */
    private final Set<SkuScheduleDTO> originalSources = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * 只记录真正成功提交的顺序，和动作实际时间的排序分离。
     */
    private long sequence;
    /**
     * 原创建点登记的真实来源身份，不从物料文本或诊断字段推断。
     */
    private final Map<SkuScheduleDTO, SkuScheduleDTO> derivedSources = new IdentityHashMap<>();
    /**
     * 原创建规则判定是否保留总目标，避免派生对象改来源类型后失去严格需求语义。
     */
    private final Map<SkuScheduleDTO, Boolean> preserveDerivedTargets = new IdentityHashMap<>();
    /**
     * 原提交顺序同时包含换模动作和不新增换模次数的绑定续排。
     */
    private final List<PreparedAction> operations = new ArrayList<>();
    /**
     * 来源身份到物理机及动作时刻的完整输入，禁止以物料文本替代来源身份。
     */
    private final Map<SkuScheduleDTO, Map<String, PreparedAction>> actions = new IdentityHashMap<>();

    /**
     * 创建仅覆盖本轮时间下机请求的输入捕获器。
     *
     * @param physicalCodes 请求物理机台
     */
    public TimedMachineOffActionPreviewCapture(Set<String> physicalCodes) {
        this(physicalCodes, Collections.emptyList());
    }

    /**
     * 创建覆盖本轮相关机台的来源身份捕获器。
     *
     * @param physicalCodes 相关机台运行码或物理码
     * @param sources 主预演前已存在的来源对象
     */
    public TimedMachineOffActionPreviewCapture(Set<String> physicalCodes, Collection<SkuScheduleDTO> sources) {
        this.physicalCodes = physicalCodes.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        this.originalSources.addAll(sources);
    }

    /**
     * 判断是否应捕获指定机台，普通竞争机台不创建额外输入快照。
     *
     * @param machineCode 实际运行态机台编码
     * @return 是否属于本轮可调整物理机台
     */
    public boolean includes(String machineCode) {
        return physicalCodes.contains(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
    }

    /**
     * 在原内核消费数量之前冻结来源及历史动作，回调必须仍调用原动作内核。
     *
     * @param context 当前预演上下文
     * @param source 原组合的来源对象
     * @param resolver 从新释放时刻复核该唯一组合的原内核
     * @return 尚未登记为成功事件的冻结输入
     */
    public PreparedAction prepare(LhScheduleContext context, SkuScheduleDTO source,
            Function<Date, Date> resolver) {
        return new PreparedAction(context, source, resolver, context.getActivePreviousAlternateEvent(),
                originalSources.contains(source), originalSources,
                derivedSources.get(source), preserveDerivedTargets.getOrDefault(source, true));
    }

    /**
     * 在真实派生创建点登记来源及原数量目标规则，后续只按身份复用。
     *
     * @param source 原需求身份
     * @param derived 已按原业务规则创建的执行视图
     * @param preserveTotalTarget 是否按原规则保留独立总目标
     */
    public void registerDerivedSource(SkuScheduleDTO source, SkuScheduleDTO derived, boolean preserveTotalTarget) {
        derivedSources.put(derived, derivedSources.getOrDefault(source, source));
        preserveDerivedTargets.put(derived, preserveTotalTarget);
    }

    /**
     * 仅登记已产生完整有效提案或正量结果的动作。
     *
     * @param machineCode 实际机台编码
     * @param start 原动作真实开始时间
     * @param action 消费前冻结输入
     */
    public void record(String machineCode, Date start, PreparedAction action) {
        action.machineCode = machineCode;
        action.sequence = ++sequence;
        operations.add(action);
        actions.computeIfAbsent(action.source, ignored -> new LinkedHashMap<>())
                .put(this.key(machineCode, start), action);
    }

    /**
     * 登记原绑定产生正量的无换模续排步骤，和换模动作共用真实提交顺序。
     *
     * @param machineCode 原绑定的实际机台
     * @param bindingResult 原绑定结果身份
     * @param action 消费前的同源输入
     */
    public void recordContinuation(String machineCode, LhScheduleResult bindingResult, PreparedAction action) {
        action.machineCode = machineCode;
        action.sequence = ++sequence;
        action.changeover = false;
        action.bindingResult = bindingResult;
        operations.add(action);
    }

    /**
     * 只输出主预演最终有效的固定操作，剔除已回滚动作，保留成功绑定续排。
     *
     * @param context 主预演恢复前的最终上下文
     * @param effectiveEvents 最终正量换模事件
     * @return 按原成功提交顺序排列的完整操作前缀
     */
    public List<PreparedAction> getEffectiveOperations(LhScheduleContext context,
            List<TimedMachineOffLoadPreview.ChangeoverEvent> effectiveEvents) {
        Set<PreparedAction> changes = Collections.newSetFromMap(new IdentityHashMap<>());
        effectiveEvents.stream().map(TimedMachineOffLoadPreview.ChangeoverEvent::getPreparedAction)
                .filter(Objects::nonNull).forEach(changes::add);
        Set<LhScheduleResult> results = Collections.newSetFromMap(new IdentityHashMap<>());
        results.addAll(context.getScheduleResultList());
        return operations.stream().filter(operation -> operation.changeover ? changes.contains(operation)
                        : results.contains(operation.bindingResult)
                        && ShiftFieldUtil.resolveScheduledQty(operation.bindingResult) > 0)
                .sorted(java.util.Comparator.comparingLong(PreparedAction::getSequence))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据正式预演结果保留的来源身份取回精确输入，缺失表示内部链路不完整。
     *
     * @param source 结果登记的来源身份
     * @param machineCode 结果机台
     * @param start 结果动作开始时间
     * @return 对应未消费输入
     */
    public PreparedAction require(SkuScheduleDTO source, String machineCode, Date start) {
        return Objects.requireNonNull(this.find(source, machineCode, start),
                "时间下机成功交接缺少同源未消费输入：" + machineCode);
    }

    /**
     * 查询已捕获的成功组合，正式基线中既有动作可能没有本轮描述。
     *
     * @param source 来源身份
     * @param machineCode 实际机台
     * @param start 原动作开始时间
     * @return 本轮成功组合描述，基线既有动作返回空
     */
    public PreparedAction find(SkuScheduleDTO source, String machineCode, Date start) {
        Map<String, PreparedAction> byMachine = actions.get(source);
        return byMachine == null ? null : byMachine.get(this.key(machineCode, start));
    }

    /**
     * 生成物理机和原动作时刻键，左右侧共同指向同一次准备。
     *
     * @param machineCode 机台编码
     * @param start 原动作开始时间
     * @return 稳定事件键
     */
    private String key(String machineCode, Date start) {
        return LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode) + "|" + start.getTime();
    }

    /**
     * 复制数量输入及可变集合，保持来源身份由调用方另行保留。
     *
     * @param source 原数量输入
     * @return 与原输入不共享日额度对象的副本
     */
    private static SkuScheduleDTO copySku(SkuScheduleDTO source) {
        SkuScheduleDTO copied = BeanUtil.copyProperties(source, SkuScheduleDTO.class);
        if (source.getDailyPlanQuotaMap() != null) {
            Map<java.time.LocalDate, SkuDailyPlanQuotaDTO> quotas = new LinkedHashMap<>();
            source.getDailyPlanQuotaMap().forEach((date, quota) -> quotas.put(date,
                    quota == null ? null : BeanUtil.copyProperties(quota, SkuDailyPlanQuotaDTO.class)));
            copied.setDailyPlanQuotaMap(quotas);
        }
        if (source.getMouldCodeList() != null) {
            copied.setMouldCodeList(new ArrayList<>(source.getMouldCodeList()));
        }
        if (source.getTemporaryFaultSourceMachineCodeSet() != null) {
            copied.setTemporaryFaultSourceMachineCodeSet(
                    new LinkedHashSet<>(source.getTemporaryFaultSourceMachineCodeSet()));
        }
        return copied;
    }

    /**
     * 冻结提前生产的可消费日计划，重评不共享正式额度对象。
     *
     * @param source 原中心视图，可为空
     * @return 独立运行视图，未激活来源为空
     */
    private static EarlyProductionRuntimePlan copyRuntimePlan(EarlyProductionRuntimePlan source) {
        if (source == null) {
            return null;
        }
        EarlyProductionRuntimePlan copied = BeanUtil.copyProperties(source, EarlyProductionRuntimePlan.class);
        Map<java.time.LocalDate, SkuDailyPlanQuotaDTO> quotas = new LinkedHashMap<>();
        source.getShiftedDailyPlanQuotaMap().forEach((date, quota) -> quotas.put(date,
                BeanUtil.copyProperties(quota, SkuDailyPlanQuotaDTO.class)));
        copied.setShiftedDailyPlanQuotaMap(quotas);
        if (source.getDecision() != null) {
            EarlyProductionDecision decision = BeanUtil.copyProperties(
                    source.getDecision(), EarlyProductionDecision.class);
            decision.setStructurePlanMachineCounts(
                    new ArrayList<>(source.getDecision().getStructurePlanMachineCounts()));
            copied.setDecision(decision);
        }
        if (source.getDecisionLogEntry() != null) {
            copied.setDecisionLogEntry(BeanUtil.copyProperties(
                    source.getDecisionLogEntry(), EarlyProductionDecisionLogEntry.class));
        }
        return copied;
    }

    /**
     * 同一来源的未消费状态与原动作回调；仅在外层资源快照保护下执行。
     */
    public static final class PreparedAction {
        /**
         * 必须保持的来源对象身份。
         */
        private final SkuScheduleDTO source;
        /**
         * 主预演消费前数量状态。
         */
        private final SkuScheduleDTO sourceState;
        /**
         * 派生视图共用的真实原日计划对象引用，不能重置为主预演旧值。
         */
        private final Map<java.time.LocalDate, SkuDailyPlanQuotaDTO> sharedQuota;
        /**
         * 是否在主预演前已有正式来源对象。
         */
        private final boolean originalSource;
        /**
         * 原创建点确定的根来源身份，普通未派生来源为空。
         */
        private final SkuScheduleDTO rootSource;
        /**
         * 原创建规则要求保留独立总目标时，不改成当前中心余量。
         */
        private final boolean preserveTotalTarget;
        /**
         * 原成功提交的实际机台编码。
         */
        private String machineCode;
        /**
         * 原成功提交顺序。
         */
        private long sequence;
        /**
         * 是否为占用换模配额的动作，无换模绑定续排为假。
         */
        private boolean changeover = true;
        /**
         * 无换模续排对应的原结果身份，用于确认主预演最终仍保留该操作。
         */
        private LhScheduleResult bindingResult;
        /**
         * 完整原动作重评入口。
         */
        private final Function<Date, Date> resolver;
        /**
         * 原动作所属排程步骤，重放仍走同一业务入口语义。
         */
        private final String currentStep;
        /**
         * 原历史动作类型、模具与有效准备截止；普通换活字块为空。
         */
        private final PreviousAlternatePlanReleaseEvent historicalEvent;
        /**
         * 原组合已验证的一次性历史增机需求；不可变值只在隔离重放中重新挂接。
         */
        private final PreviousAlternateAdditionalMachineDemand additionalMachineDemand;
        /**
         * 原关联预演为本物理机冻结的完整模具组，不重新改选模具。
         */
        private final Map<String, List<String>> plannedMoulds = new LinkedHashMap<>();
        /**
         * 原组合已激活的提前生产中心视图，保存身份所对应的数量语义。
         */
        private final EarlyProductionRuntimePlan earlyProductionPlan;
        /**
         * 主预演时共享提前生产运行视图的原来源身份，与普通日计划账本分离。
         */
        private final SkuScheduleDTO earlyProductionOwner;

        /**
         * 冻结一次动作的原输入。
         *
         * @param context 主预演上下文
         * @param source 来源身份
         * @param resolver 原动作内核
         * @param historicalEvent 当前历史事件，可为空
         * @param originalSource 是否为主预演前已有来源
         * @param originalSources 主预演前原来源身份集合
         * @param rootSource 原创建点登记的根来源，可为空
         * @param preserveTotalTarget 是否保留原独立总目标
         */
        private PreparedAction(LhScheduleContext context, SkuScheduleDTO source, Function<Date, Date> resolver,
                PreviousAlternatePlanReleaseEvent historicalEvent, boolean originalSource,
                Set<SkuScheduleDTO> originalSources,
                SkuScheduleDTO rootSource, boolean preserveTotalTarget) {
            this.source = source;
            this.sharedQuota = source.getDailyPlanQuotaMap();
            this.originalSource = originalSource;
            this.rootSource = rootSource;
            this.preserveTotalTarget = preserveTotalTarget;
            EarlyProductionRuntimePlan runtime = context.getEarlyProductionRuntimePlanMap().get(source);
            this.earlyProductionPlan = copyRuntimePlan(runtime);
            this.earlyProductionOwner = runtime == null ? null
                    : context.getEarlyProductionRuntimePlanMap().entrySet().stream()
                    .filter(entry -> originalSources.contains(entry.getKey()) && entry.getValue() != null
                            && entry.getValue().getShiftedDailyPlanQuotaMap() == runtime.getShiftedDailyPlanQuotaMap())
                    .map(Map.Entry::getKey).findFirst().orElse(source);
            this.sourceState = copySku(source);
            this.resolver = resolver;
            this.currentStep = context.getCurrentStep();
            this.historicalEvent = historicalEvent == null ? null
                    : BeanUtil.copyProperties(historicalEvent, PreviousAlternatePlanReleaseEvent.class);
            this.additionalMachineDemand = context.resolvePreviousAlternateAdditionalMachineDemand(source);
            if (historicalEvent != null) {
                String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        historicalEvent.getMachineCode());
                context.getPreviousAlternatePlannedMouldMap().forEach((machine, moulds) -> {
                    if (physical.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine))) {
                        plannedMoulds.put(machine, new ArrayList<>(moulds));
                    }
                });
            }
        }

        /**
         * 返回外层资源快照需要包含的原来源对象。
         *
         * @return 原来源身份
         */
        public SkuScheduleDTO getSource() {
            return source;
        }

        /**
         * 在已经隔离的上下文恢复未消费输入并执行原组合，禁止用于正式提交。
         *
         * @param context 外层已建立恢复快照的上下文
         * @param release 当前物理释放时间
         * @return 通过完整原内核的动作开始时间，失败为空
         */
        public Date revalidate(LhScheduleContext context, Date release) {
            return this.replay(context, release, Collections.newSetFromMap(new IdentityHashMap<>()));
        }

        /**
         * 返回原成功提交顺序。
         *
         * @return 顺序编号
         */
        public long getSequence() {
            return sequence;
        }

        /**
         * 判断本操作是否产生换模次数。
         *
         * @return 换模或换活字块动作返回真，原绑定续排返回假
         */
        public boolean isChangeover() {
            return changeover;
        }

        /**
         * 返回原实际机台编码。
         *
         * @return 运行态机台编码
         */
        public String getMachineCode() {
            return machineCode;
        }

        /**
         * 在当前续作已扣账的隔离上下文重放固定组合，同一来源仅初始化一次。
         *
         * @param context 已隔离且当前续作已正式扣账的上下文
         * @param release 本组合可用机台时刻
         * @param initialized 当前前缀已经初始化的来源身份
         * @return 正量提交的动作开始时间，无合法结果为空
         */
        public Date replay(LhScheduleContext context, Date release, Set<SkuScheduleDTO> initialized) {
            if (initialized.add(source) && !originalSource) {
                // 派生对象只恢复执行身份，日额度继续绑定隔离续作已扣减的真实共享引用。
                BeanUtil.copyProperties(copySku(sourceState), source);
                source.setDailyPlanQuotaMap(rootSource == null ? sharedQuota : rootSource.getDailyPlanQuotaMap());
                Integer remaining = context.getSkuProductionRemainingQtyMap().get(
                        MonthPlanDateResolver.buildMaterialStatusKey(
                                source.getMaterialCode(), source.getProductStatus()));
                if (remaining != null) {
                    source.setRemainingScheduleQty(remaining);
                    source.setPendingQty(remaining);
                    // 原创建点明确普通派生使用实时余量，严格需求继续保留原总目标。
                    if (!preserveTotalTarget) {
                        source.setTargetScheduleQty(remaining);
                    }
                }
                if (earlyProductionPlan != null && !context.getEarlyProductionRuntimePlanMap().containsKey(source)) {
                    // 提前生产移位额度是独立账本；优先共享当前原来源的运行视图，不能挂普通日计划。
                    EarlyProductionRuntimePlan ownerPlan =
                            context.getEarlyProductionRuntimePlanMap().get(earlyProductionOwner);
                    context.getEarlyProductionRuntimePlanMap().put(source,
                            ownerPlan != null ? ownerPlan : copyRuntimePlan(earlyProductionPlan));
                }
            }
            PreviousAlternatePlanReleaseEvent event = historicalEvent == null ? null
                    : BeanUtil.copyProperties(historicalEvent, PreviousAlternatePlanReleaseEvent.class);
            if (event != null) {
                event.setOfflineTime(release);
                if (additionalMachineDemand != null) {
                    context.getPreviousAlternateAdditionalMachineDemandMap()
                            .put(event.getMachineCode(), additionalMachineDemand);
                }
                plannedMoulds.forEach((machine, moulds) -> context.getPreviousAlternatePlannedMouldMap()
                        .put(machine, new ArrayList<>(moulds)));
            }
            context.setCurrentStep(currentStep);
            context.setActivePreviousAlternateEvent(event);
            context.getPreviousAlternateCandidateAvailableTimeMap().put(source, release);
            return resolver.apply(release);
        }
    }
}
