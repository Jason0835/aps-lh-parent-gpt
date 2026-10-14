package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.StructureMinMachineRetentionService;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.TypeBlockProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffLoadPreview;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffActionPreviewCapture;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffRequest;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffReleaseResolver;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 时间下机前一次性有界预演；返回负荷及同源动作输入，所有模拟结果和账本均回滚。
 *
 * @author APS
 * @date 2026/10/13
 */
@Slf4j
@Service
public class TimedMachineOffLoadPreviewService {
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private TimedMachineOffShiftService timedMachineOffShiftService;
    @Resource
    private PreviousAlternatePlanReuseService previousAlternatePlanReuseService;
    @Resource
    private TypeBlockProductionStrategy typeBlockProductionStrategy;
    @Resource
    private NewSpecProductionStrategy newSpecProductionStrategy;
    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;

    /**
     * 每批只模拟一次相关释放机台，候选池和正式时间轴均复用原链路。
     *
     * @param context 余量收尾完成、正式续作尚未扣账的上下文
     * @return 独立负荷快照；无可调整请求时不创建资源快照、不扫描候选
     */
    public TimedMachineOffLoadPreview preview(LhScheduleContext context) {
        List<TimedMachineOffRequest> requests = context.getTimedMachineOffRequests().stream()
                .filter(request -> !timedMachineOffShiftService.hasFixedRelease(context, request)).collect(Collectors.toList());
        if (requests.isEmpty()) {
            return null;
        }
        long started = System.nanoTime();
        LocalDate lastDate = requests.stream().map(TimedMachineOffRequest::getStartDate).max(LocalDate::compareTo).get();
        Date lastMorning = TimedMachineOffReleaseResolver.resolveProductionStartShift(
                context.getScheduleWindowShifts(), lastDate).getShiftStartDateTime();
        Set<String> sourceMachineCodes = context.getScheduleResultList().stream()
                .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toCollection(LinkedHashSet::new));
        sourceMachineCodes.addAll(context.getPreviousAlternateReleaseEventMap().keySet());
        Set<String> requestPhysicalCodes = requests.stream().flatMap(request -> request.getProfile().getOriginals().stream())
                .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        Set<LhScheduleResult> baselineResults = Collections.newSetFromMap(new IdentityHashMap<>());
        context.getScheduleResultList().stream()
                .filter(result -> Objects.nonNull(result.getMouldChangeStartTime())
                        && ShiftFieldUtil.resolveScheduledQty(result) > 0).forEach(baselineResults::add);
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        TimedMachineOffActionPreviewCapture previousCapture = context.getTimedMachineOffActionPreviewCapture();
        PreviousAlternatePlanReleaseEvent previousEvent = context.getActivePreviousAlternateEvent();
        TimedMachineOffActionPreviewCapture capture =
                new TimedMachineOffActionPreviewCapture(sourceMachineCodes, sources);
        Map<String, int[]> counts = new LinkedHashMap<>();
        List<TimedMachineOffLoadPreview.ChangeoverEvent> events = new ArrayList<>();
        List<TimedMachineOffActionPreviewCapture.PreparedAction> operations = Collections.emptyList();
        Map<String, Map<LocalDate, String>> structureHandoffs = new LinkedHashMap<>();
        int scopeSize = 0;
        int proposalCount = 0;
        try {
            context.setTimedMachineOffLoadPreview(true);
            context.setTimedMachineOffActionPreviewCapture(capture);
            context.setStructureShiftInMachineIndex(null);
            // 先在模拟账本发布现有释放事实，正式上下文仍保留清零前画像和未消费日计划。
            timedMachineOffShiftService.resolveAndApply(context);
            IProductionStrategy continuous = strategyFactory.getProductionStrategy(ScheduleTypeEnum.CONTINUOUS.getCode());
            continuous.finalizeContinuousProduction(context);
            Set<String> scope = sourceMachineCodes.stream().filter(code -> {
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(code);
                return Objects.nonNull(machine) && Objects.nonNull(machine.getEstimatedEndTime())
                        && !machine.getEstimatedEndTime().after(lastMorning);
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            scopeSize = (int) scope.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode).distinct().count();
            if (!scope.isEmpty()) {
                context.setNewSpecMachineResourceScopeCodeSet(scope);
                previousAlternatePlanReuseService.reuse(context);
                typeBlockProductionStrategy.scheduleTypeBlockChange(context);
                // 普通候选只预测其他释放机台的竞争负荷；本请求无后料时不虚构一次换模。
                Set<String> ordinaryScope = scope.stream().filter(code -> !requestPhysicalCodes.contains(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(code)))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (!ordinaryScope.isEmpty()) {
                    context.setNewSpecMachineResourceScopeCodeSet(ordinaryScope);
                    context.setCurrentStep(ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode());
                    newSpecProductionStrategy.previewTimedMachineOffLoad(context, lastDate,
                            strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                            strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy());
                }
            }
            context.getDailyMouldChangeCountMap().forEach((date, count) -> counts.put(date, count.clone()));
            events = this.collectChangeoverEvents(context, requestPhysicalCodes, baselineResults);
            operations = capture.getEffectiveOperations(context, events);
            proposalCount = events.size();
            structureHandoffs = this.collectStructureHandoffs(context, requests, events);
        } finally {
            snapshot.restore(context);
            context.setActivePreviousAlternateEvent(previousEvent);
            context.setTimedMachineOffActionPreviewCapture(previousCapture);
        }
        String detail = String.format("批次=%s, 请求数=%s, 相关物理机台=%s, 有量交替预演数=%s, "
                        + "负荷=%s, 耗时毫秒=%s, 正式数量和次数账本=已恢复",
                context.getBatchNo(), requests.size(), scopeSize, proposalCount,
                counts.entrySet().stream().map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                        .collect(Collectors.joining(";")), (System.nanoTime() - started) / 1_000_000L);
        log.info("按时间下机负荷预演完成, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机负荷预演完成", detail);
        List<TimedMachineOffActionPreviewCapture.PreparedAction> successfulOperations = new ArrayList<>(operations);
        return new TimedMachineOffLoadPreview(counts, events, structureHandoffs,
                (event, release, simulatedCounts) -> this.revalidateAction(
                        context, successfulOperations, event, release, simulatedCounts));
    }

    /**
     * 收集正量结果中的唯一动作，全部本轮新增成功组合均须保留同源输入及提交顺序。
     *
     * @param context 模拟结果及本轮输入捕获器
     * @param requestedMachines 可调整物理机台
     * @param baselineResults 主预演前已有正量动作的结果身份
     * @return 去重后的成功事件及首次可移动交接
     */
    private List<TimedMachineOffLoadPreview.ChangeoverEvent> collectChangeoverEvents(
            LhScheduleContext context, Set<String> requestedMachines, Set<LhScheduleResult> baselineResults) {
        Set<String> eventKeys = new LinkedHashSet<>();
        Set<String> firstHandoffMachines = new LinkedHashSet<>();
        List<TimedMachineOffLoadPreview.ChangeoverEvent> events = new ArrayList<>();
        List<LhScheduleResult> changedResults = context.getScheduleResultList().stream()
                .filter(result -> Objects.nonNull(result.getMouldChangeStartTime()) && ShiftFieldUtil.resolveScheduledQty(result) > 0)
                .sorted(Comparator.comparing(LhScheduleResult::getMouldChangeStartTime)).collect(Collectors.toList());
        for (LhScheduleResult result : changedResults) {
            Date changeTime = result.getMouldChangeStartTime();
            String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
            if (!eventKeys.add(physical + "|" + changeTime.getTime() + "|" + result.getMaterialCode() + "|" + result.getProductStatus())) {
                continue;
            }
            int slot = LhScheduleTimeUtil.isMorningShift(context, changeTime) ? 0 : 1;
            if (!LhScheduleTimeUtil.isMorningShift(context, changeTime) && !LhScheduleTimeUtil.isAfternoonShift(context, changeTime)) {
                continue;
            }
            LocalDate date = changeTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            boolean baseline = baselineResults.contains(result);
            boolean movable = !baseline && requestedMachines.contains(physical) && firstHandoffMachines.add(physical);
            TimedMachineOffLoadPreview.ChangeoverEvent event = new TimedMachineOffLoadPreview.ChangeoverEvent(
                    physical, result.getMaterialCode(), result.getProductStatus(), changeTime, date, slot, movable);
            if (!baseline) {
                // 只有显式记录的正式基线允许缺少本轮捕获；新增竞争动作同样必须能按固定组合重放。
                TimedMachineOffActionPreviewCapture capture = Objects.requireNonNull(
                        context.getTimedMachineOffActionPreviewCapture(), "时间下机事件收集缺少同源输入捕获器");
                event.withPreparedAction(capture.require(context.getScheduleResultSourceSkuMap().get(result),
                        result.getLhMachineCode(), changeTime));
            }
            events.add(event);
        }
        return events;
    }

    /**
     * 从当前前料决策重新形成隔离资源状态，按原成功提交顺序重放目标之前的固定组合。
     * 前缀的真实首检、模具、结构和共享数量持续生效，无换模续排同样按原提交顺序消费。
     * 只有换模前缀反馈次数迁移，无换模操作仅更新隔离资源和数量。
     *
     * @param context 正式上下文，当前释放决策已登记但尚未统一扣账
     * @param successfulOperations 主预演有效操作队列，包含固定换模和无换模续排，排除已回滚输入
     * @param event 本轮需要重评的首次后料事件
     * @param release 本轮物理释放时间
     * @param simulatedCounts 已排除目标及其固定前缀预算的独立模拟次数
     * @return 目标动作时间及全部已复核前缀的成功或失败事实，供模拟账本一次性更新
     * @throws IllegalStateException 固定前缀缺少已知机台可用时间时抛出
     */
    private TimedMachineOffLoadPreview.ActionRevalidationResult revalidateAction(LhScheduleContext context,
            List<TimedMachineOffActionPreviewCapture.PreparedAction> successfulOperations,
            TimedMachineOffLoadPreview.ChangeoverEvent event, Date release, Map<String, int[]> simulatedCounts) {
        TimedMachineOffActionPreviewCapture.PreparedAction action = Objects.requireNonNull(
                event.getPreparedAction(), "时间下机后料事件缺少未消费输入");
        List<TimedMachineOffActionPreviewCapture.PreparedAction> prefix = successfulOperations.stream()
                .filter(previous -> previous.getSequence() < action.getSequence())
                .sorted(Comparator.comparingLong(TimedMachineOffActionPreviewCapture.PreparedAction::getSequence))
                .collect(Collectors.toList());
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        successfulOperations.stream().map(TimedMachineOffActionPreviewCapture.PreparedAction::getSource)
                .forEach(sources::add);
        sources.add(action.getSource());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        TimedMachineOffActionPreviewCapture previousCapture = context.getTimedMachineOffActionPreviewCapture();
        PreviousAlternatePlanReleaseEvent previousEvent = context.getActivePreviousAlternateEvent();
        try {
            // 只重新提交已冻结组合；关闭捕获，防止固定前缀递归扩展成候选扫描。
            context.setTimedMachineOffActionPreviewCapture(null);
            context.setTimedMachineOffLoadPreview(true);
            Map<String, int[]> formalCounts = this.copyCounts(context.getDailyMouldChangeCountMap());
            IProductionStrategy continuous = strategyFactory.getProductionStrategy(ScheduleTypeEnum.CONTINUOUS.getCode());
            continuous.finalizeContinuousProduction(context);
            Map<String, int[]> replayCounts = this.copyCounts(simulatedCounts);
            Set<String> dates = new LinkedHashSet<>(formalCounts.keySet());
            dates.addAll(context.getDailyMouldChangeCountMap().keySet());
            for (String date : dates) {
                int[] before = formalCounts.getOrDefault(date, new int[2]);
                int[] after = context.getDailyMouldChangeCountMap().getOrDefault(date, new int[2]);
                int[] replay = replayCounts.computeIfAbsent(date, ignored -> new int[2]);
                for (int slot = 0; slot < replay.length; slot++) {
                    replay[slot] += after[slot] - before[slot];
                }
            }
            // 未重放的后续预算从首个前缀开始生效；保留当前续作提交可能产生的真实次数增量。
            context.setDailyMouldChangeCountMap(replayCounts);
            Set<SkuScheduleDTO> initialized = Collections.newSetFromMap(new IdentityHashMap<>());
            Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> prefixActionTimes = new IdentityHashMap<>();
            Map<TimedMachineOffActionPreviewCapture.PreparedAction, Date> prefixReleaseTimes = new IdentityHashMap<>();
            for (TimedMachineOffActionPreviewCapture.PreparedAction previous : prefix) {
                Date previousRelease = this.resolveActionRelease(context, previous, null);
                // 失败组合不产生资源，成功组合按原内核消费；全部复核事实必须与外层模拟次数一并更新。
                Date started = this.replayAction(context, previous, previousRelease, initialized, sources);
                if (previous.isChangeover()) {
                    prefixReleaseTimes.put(previous, previousRelease);
                    prefixActionTimes.put(previous, started);
                }
            }
            // 每个前缀已经在保留后续预算的同一账本上成功消费，目标直接继承，不能再次叠加次数。
            Date targetRelease = this.resolveActionRelease(context, action, release);
            Date targetTime = this.replayAction(context, action, targetRelease, initialized, sources);
            return new TimedMachineOffLoadPreview.ActionRevalidationResult(
                    targetTime, prefixActionTimes, prefixReleaseTimes);
        } finally {
            snapshot.restore(context);
            context.setActivePreviousAlternateEvent(previousEvent);
            context.setTimedMachineOffActionPreviewCapture(previousCapture);
        }
    }

    /**
     * 读取固定动作当前真实可用时刻，已成功前缀推进的机台占用必须覆盖原前料释放边界。
     * 单控整机按全部侧的较晚时刻交接，禁止后续动作重新回到旧时间节点。
     *
     * @param context 当前隔离资源状态
     * @param action 固定组合
     * @param requestedRelease 本轮目标的已确认释放时刻，前缀可为空
     * @return 原释放、当前整机占用及目标请求时刻中的最晚值
     * @throws IllegalStateException 没有任何已知机台可用时刻时抛出
     */
    private Date resolveActionRelease(LhScheduleContext context,
            TimedMachineOffActionPreviewCapture.PreparedAction action, Date requestedRelease) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(action.getMachineCode());
        Date machineReady = context.getMachineScheduleMap().entrySet().stream()
                .filter(entry -> Objects.equals(physical,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey())))
                .map(Map.Entry::getValue).filter(Objects::nonNull)
                .map(MachineScheduleDTO::getEstimatedEndTime).filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        Date timedReady = context.getTimedMachineOffBoundary(action.getMachineCode());
        return Arrays.asList(machineReady, timedReady, requestedRelease).stream().filter(Objects::nonNull)
                .max(Date::compareTo).orElseThrow(() -> new IllegalStateException(
                        "时间下机固定组合缺少机台可用时刻：" + action.getMachineCode()));
    }

    /**
     * 对单个固定组合建立事务快照；准入失败时撤销该组合的部分资源消费，保留已成功前缀。
     *
     * @param context 当前前缀形成的隔离状态
     * @param action 唯一冻结组合
     * @param release 当前机台可用时间
     * @param initialized 已成功初始化的来源身份集合
     * @param sources 当前复核可能消费的全部来源
     * @return 正量动作开始时间，失败为空且不留下资源或初始化状态
     */
    private Date replayAction(LhScheduleContext context, TimedMachineOffActionPreviewCapture.PreparedAction action,
            Date release, Set<SkuScheduleDTO> initialized, List<SkuScheduleDTO> sources) {
        TimedMachineOffPreviewSnapshot attempt = new TimedMachineOffPreviewSnapshot(context, sources);
        Set<SkuScheduleDTO> beforeInitialized = Collections.newSetFromMap(new IdentityHashMap<>());
        beforeInitialized.addAll(initialized);
        PreviousAlternatePlanReleaseEvent previousEvent = context.getActivePreviousAlternateEvent();
        Date started = action.replay(context, release, initialized);
        if (Objects.isNull(started)) {
            attempt.restore(context);
            context.setActivePreviousAlternateEvent(previousEvent);
            initialized.retainAll(beforeInitialized);
        }
        return started;
    }

    /**
     * 复制次数数组，使固定组合重放不能修改调用方的预算。
     *
     * @param source 独立模拟或真实隔离账本
     * @return 日期和计数数组均独立的副本
     */
    private Map<String, int[]> copyCounts(Map<String, int[]> source) {
        Map<String, int[]> copied = new LinkedHashMap<>();
        source.forEach((date, counts) -> copied.put(date, counts.clone()));
        return copied;
    }

    /**
     * 只从完整内核已经排出正量的后料提取接替证据。早班已到结构上限且释放机台不在其中时，
     * 恢复该机早班生产会阻断已验证后料；此时才允许结构接替优先于中位均衡偏好。
     * @param context 完整预演上下文 @param requests 可调整请求 @param events 成功换模事件
     * @return 请求物理机台到各日结构接替证据
     */
    private Map<String, Map<LocalDate, String>> collectStructureHandoffs(LhScheduleContext context,
            List<TimedMachineOffRequest> requests, List<TimedMachineOffLoadPreview.ChangeoverEvent> events) {
        Map<String, Map<LocalDate, String>> handoffs = new LinkedHashMap<>();
        Set<String> changedMachines = events.stream().map(TimedMachineOffLoadPreview.ChangeoverEvent::getPhysicalMachineCode)
                .collect(Collectors.toSet());
        for (TimedMachineOffRequest request : requests) {
            String structure = request.getSourceSku().getStructureName();
            if (Objects.isNull(structure) || structure.isEmpty()) {
                continue;
            }
            String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    request.getProfile().getOriginals().get(0).getLhMachineCode());
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                LocalDate date = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (!shift.isMorningShift() || date.isBefore(request.getStartDate())) {
                    continue;
                }
                Set<String> successors = context.getScheduleResultList().stream()
                        .filter(result -> structure.equals(result.getStructureName())
                                && Objects.nonNull(result.getMouldChangeStartTime())
                                && Objects.nonNull(ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()))
                                && ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) > 0)
                        .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                        .filter(machine -> !physical.equals(machine) && changedMachines.contains(machine))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (successors.isEmpty()) {
                    continue;
                }
                Set<String> scheduled = structureMinMachineRetentionService
                        .resolveEffectiveStructureMachineStatistics(context, structure, shift).getScheduledPhysicalMachineCodes();
                int limit = context.getStructurePlanMachineCount(date, structure);
                if (limit > 0 && scheduled.size() == limit && !scheduled.contains(physical)) {
                    handoffs.computeIfAbsent(physical, key -> new LinkedHashMap<>()).put(date,
                            "结构=" + structure + ", 班次=" + shift.getShiftIndex() + ", 已排=" + scheduled.size()
                                    + ", 上限=" + limit + ", 接替机台=" + String.join(",", successors));
                }
            }
        }
        return handoffs;
    }
}
