package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.component.NewProductionDateAdmissionPolicy;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.enums.UnscheduledReasonEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.UnscheduledResultCollector;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.*;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Supplier;

/**
 * 专供机台分结构四班转产，复用候选原有准入、结构切换约束、单组合预演和完整回滚。
 * 原料截断与后料跨日生产属于同一次提交，失败不会留下空机或资源扣账。
 */
@Slf4j
@Service
public class DedicatedSupplyTransferService {
    /**
     * 成功转产的新料首个正量班次原因。
     */
    public static final String TRANSFER_ANALYSIS = "硫化机专供成型机转产下机";
    /**
     * 单机时间轴与生产内核。
     */
    @Resource
    private NewSpecProductionStrategy productionStrategy;
    /**
     * 本批公共策略。
     */
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    /**
     * 普通物料资格解析器。
     */
    @Resource
    private NewSpecMaterialEligibilityService eligibilityService;
    /**
     * 普通机台需求门禁。
     */
    @Resource
    private NewSpecCandidatePoolBuilder candidatePoolBuilder;
    /**
     * 复用现有机台选料比较规则。
     */
    @Resource
    private NewSpecMachineSkuCompetitionService competitionService;
    /**
     * 原料截断及共享账本恢复。
     */
    @Resource
    private ContinuationCutoverService cutoverService;
    /**
     * 实时剩余需求。
     */
    @Resource
    private TargetScheduleQtyResolver quantityResolver;
    /**
     * 结构在机统计刷新。
     */
    @Resource
    private StructureEndingAlignmentService alignmentService;
    /**
     * 最终未排投影。
     */
    @Resource
    private UnscheduledResultCollector unscheduledCollector;
    /**
     * 截断后重新投影胶囊资源占用。
     */
    @Resource
    private CapsuleReplacementRuleService capsuleReplacementRuleService;

    /**
     * 按收尾、胎胚机台数及编码顺序处理全部已排产专供机台。
     *
     * @param context 结构切换及回补完成后的实时上下文
     */
    public void process(LhScheduleContext context) {
        if (context.getSupplyFormingMachinesByLhMachineMap().isEmpty()) {
            return;
        }
        Map<String, List<LhScheduleResult>> tails = this.collectTails(context);
        List<SkuScheduleDTO> originalPending = new ArrayList<>(context.getNewSpecSkuList());
        List<SkuScheduleDTO> pool = tails.isEmpty() ? Collections.emptyList() : this.remainingCandidates(context);
        log.info("专供转产入口, factoryCode={}, batchNo={}, eligibleMachineGroups={}, frozenCandidates={}, remainingCandidates={}, originalWorkingCandidates={}",
                context.getFactoryCode(), context.getBatchNo(), tails.size(),
                context.getNextShiftNewPlanCandidateList().size(), pool.size(), originalPending.size());
        if (tails.isEmpty() || pool.isEmpty()) {
            return;
        }
        long started = System.nanoTime();
        Date originalDate = context.getCurrentScheduleDate();
        List<DayScheduleContext> days = new ArrayList<>();
        Map<LocalDate, List<LhShiftConfigVO>> grouped = LhScheduleTimeUtil.groupByWorkDate(
                StructureSwitchSchedulingPolicy.orderedShifts(context));
        grouped.forEach((date, shifts) -> days.add(new DayScheduleContext(date, shifts,
                days.isEmpty(), days.size() == grouped.size() - 1)));
        Map<String, Set<String>> embryoMachines = this.embryoMachineIndex(context);
        int succeeded = 0;
        try {
            context.setNewSpecSkuList(new ArrayList<>(pool));
            // 入口末段只收集一次，成功后的新料不能再次入选；四班分别只扫描该固定集合。
            Set<String> transferred = new HashSet<>();
            Map<String, Integer> actualByStructure = new HashMap<>();
            LocalDate firstDate = days.get(0).getScheduleDate();
            for (int dayIndex = 0; dayIndex < Math.min(2, days.size()); dayIndex++) {
                DayScheduleContext day = days.get(dayIndex);
                // 保留原四班和T日统计基数，首日不截断原料、不扣资源，也不计成功数。
                if (!NewProductionDateAdmissionPolicy.isAllowed(context, day.getScheduleDate())) {
                    log.info("专供转产按日期准入跳过, factoryCode={}, batchNo={}, date={}",
                            context.getFactoryCode(), context.getBatchNo(), day.getScheduleDate());
                    continue;
                }
                for (LhShiftConfigVO shift : day.getDayShifts()) {
                    if (shift.resolveShiftTypeEnum() == ShiftEnum.NIGHT_SHIFT) {
                        continue;
                    }
                    Map<String, Integer> targets = new HashMap<>();
                    Map<String, Integer> shiftActual = new HashMap<>();
                    Map<String, List<LhScheduleResult>> candidates = new LinkedHashMap<>();
                    tails.forEach((code, rows) -> {
                        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(code);
                        if (!transferred.contains(physical) && this.endsInShift(context, rows, shift)) {
                            candidates.put(code, rows);
                        }
                    });
                    while (!candidates.isEmpty()) {
                        String code = this.selectNextMachine(context, candidates, embryoMachines);
                        List<LhScheduleResult> rows = candidates.remove(code);
                        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(code);
                        String structure = this.source(context, rows).getStructureName();
                        int currentDay = dayIndex;
                        int target = targets.computeIfAbsent(structure, key -> this.shiftTarget(
                                context.getStructurePlanMachineCount(firstDate, key), currentDay,
                                shift.resolveShiftTypeEnum(), actualByStructure.getOrDefault(key, 0)));
                        if (transferred.contains(physical) || shiftActual.getOrDefault(structure, 0) >= target) {
                            continue;
                        }
                        boolean success = this.tryMachine(context, rows, pool, days, dayIndex, shift);
                        if (!success && rows.size() > 1) {
                            for (LhScheduleResult row : rows) {
                                if (this.tryMachine(context, Collections.singletonList(row), pool, days, dayIndex, shift)) {
                                    success = true;
                                    break;
                                }
                            }
                        }
                        if (success) {
                            transferred.add(physical);
                            actualByStructure.merge(structure, 1, Integer::sum);
                            shiftActual.merge(structure, 1, Integer::sum);
                            succeeded++;
                            this.refreshEmbryoMachines(context, embryoMachines, code);
                        }
                    }
                    log.info("专供转产分班统计, batchNo={}, date={}, shift={}, target={}, actual={}",
                            context.getBatchNo(), day.getScheduleDate(), shift.getShiftType(), targets, shiftActual);
                }
            }
        } finally {
            // 前置成功组合的消费和提前生产计划继续交给新增，保留本次新增的补偿身份。
            Set<SkuScheduleDTO> knownCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
            knownCandidates.addAll(originalPending);
            knownCandidates.addAll(pool);
            context.getNewSpecSkuList().stream().filter(knownCandidates::add).forEach(originalPending::add);
            context.setNewSpecSkuList(originalPending);
            context.rebuildStructureSkuMapFromPending(originalPending);
            context.setCurrentScheduleDate(originalDate);
            log.info("专供转产结束, factoryCode={}, batchNo={}, success={}, elapsedMs={}",
                    context.getFactoryCode(), context.getBatchNo(), succeeded,
                    (System.nanoTime() - started) / 1_000_000L);
        }
    }

    /**
     * 计算当前结构的单班目标，首日中班采用固定目标，次日使用此前实际数量。
     *
     * @param machines 首日月计划最大硫化机台数
     * @param dayIndex 从零开始的业务日偏移
     * @param shiftType 当前班别
     * @param actual 此前已成功下机数量
     * @return 本班最多允许下机的物理机台数
     */
    int shiftTarget(int machines, int dayIndex, ShiftEnum shiftType, int actual) {
        if (shiftType == ShiftEnum.NIGHT_SHIFT || machines <= 0 || dayIndex > 1) {
            return 0;
        }
        int firstMorning = (int) ((machines + 3L) / 4);
        if (dayIndex == 0) {
            return shiftType == ShiftEnum.MORNING_SHIFT ? firstMorning
                    : (int) ((machines + 1L) / 2) - firstMorning;
        }
        int remaining = Math.max(0, machines - actual);
        return shiftType == ShiftEnum.MORNING_SHIFT ? (int) ((remaining + 1L) / 2) : remaining;
    }

    /**
     * 按入口末段最后正量班次和实际释放时间核对当班收尾，班末结束归属生产班。
     *
     * @param context 完整窗口
     * @param rows 同物理机当前末段
     * @param shift 当前早班或中班
     * @return 全部运行侧均在本班结束时为真
     */
    boolean endsInShift(LhScheduleContext context, List<LhScheduleResult> rows, LhShiftConfigVO shift) {
        return rows.stream().allMatch(row -> {
            LhShiftConfigVO last = context.getScheduleWindowShifts().stream()
                    .filter(item -> ShiftFieldUtil.getShiftPlanQty(row, item.getShiftIndex()) != null
                            && ShiftFieldUtil.getShiftPlanQty(row, item.getShiftIndex()) > 0)
                    .max(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime)).orElse(null);
            return last == shift && row.getSpecEndTime() != null
                    && row.getSpecEndTime().after(shift.getShiftStartDateTime())
                    && !row.getSpecEndTime().after(shift.getShiftEndDateTime());
        });
    }

    /**
     * 从冻结身份恢复独立后处理候选，普通新增的最终未排收口不能丢失仍有真实余量的需求。
     * 当前工作对象优先于冻结对象，数量始终读取共享账本，不从未排数量重建SKU。
     *
     * @param context 原候选身份与实时数量账本
     * @return 按物料状态去重且仍有未完成量的原SKU对象
     */
    List<SkuScheduleDTO> remainingCandidates(LhScheduleContext context) {
        Map<String, SkuScheduleDTO> candidates = new LinkedHashMap<>();
        java.util.stream.Stream.concat(context.getNextShiftNewPlanCandidateList().stream(),
                        context.getNewSpecSkuList().stream())
                .filter(Objects::nonNull)
                .forEach(sku -> candidates.put(StructureSwitchCandidateScope.skuKey(sku), sku));
        return candidates.values().stream()
                .filter(sku -> quantityResolver.previewUnfulfilledProductionRemainingQty(context, sku) > 0)
                .collect(Collectors.toList());
    }

    /**
     * 按收尾、同胎胚物理机台数、运行编码选择下一组，比较过程只读取索引。
     *
     * @param context 本批收尾快照
     * @param tails 尚未处理的末段集合
     * @param embryoMachines 实时胎胚物理机索引
     * @return 最高优先级机台编码
     */
    String selectNextMachine(LhScheduleContext context, Map<String, List<LhScheduleResult>> tails,
            Map<String, Set<String>> embryoMachines) {
        return tails.keySet().stream().min(Comparator
                .comparingInt((String code) -> this.endingRank(context, tails.get(code)))
                .thenComparingInt(code -> embryoMachines.getOrDefault(this.source(context, tails.get(code))
                        .getEmbryoCode(), Collections.emptySet()).size())
                .thenComparing(code -> code)).orElse(null);
    }

    /**
     * 从每个运行侧提取最后有效生产段，历史指定物理机整机豁免本次下机，结构切换结果仍保留。
     * 同物理机两侧同SKU组成整组，不同SKU分别由普通单侧匹配校验决定能否处理。
     *
     * @param context 实时结果及来源
     * @return 按运行机台编码组织的合法末段
     */
    Map<String, List<LhScheduleResult>> collectTails(LhScheduleContext context) {
        Set<String> historicalMachines = this.historicalProtectedMachines(context);
        Map<String, LhScheduleResult> last = new LinkedHashMap<>();
        context.getScheduleResultList().stream().filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0)
                .filter(result -> result.getSpecEndTime() != null)
                .forEach(result -> last.merge(result.getLhMachineCode(), result,
                        (left, right) -> left.getSpecEndTime().after(right.getSpecEndTime()) ? left : right));
        Map<String, List<LhScheduleResult>> groups = new LinkedHashMap<>();
        for (LhScheduleResult result : last.values().stream()
                .sorted(Comparator.comparing(LhScheduleResult::getLhMachineCode)).collect(Collectors.toList())) {
            SkuScheduleDTO sku = context.getScheduleResultSourceSkuMap().get(result);
            String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
            if (sku == null || !context.getSupplyFormingMachinesByLhMachineMap().containsKey(physical)
                    || !StructureTransferExemptionPolicy.shouldBypassStructureSwitch(context, sku)
                    || NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, sku) == null
                    || historicalMachines.contains(physical)
                    || context.getStructureSwitchResultPlanMap().containsKey(result)) {
                continue;
            }
            List<LhScheduleResult> existing = groups.values().stream()
                    .filter(rows -> StringUtils.equals(physical, LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                            rows.get(0).getLhMachineCode())))
                    .filter(rows -> StringUtils.equals(StructureSwitchCandidateScope.skuKey(sku),
                            StructureSwitchCandidateScope.skuKey(this.source(context, rows))))
                    .findFirst().orElse(null);
            if (existing == null) {
                groups.put(result.getLhMachineCode(), new ArrayList<>(Collections.singletonList(result)));
            } else {
                existing.add(result);
            }
        }
        return groups;
    }

    /**
     * 按既有日内置换口径冻结历史指定保护范围，不要求指定后料已经排产成功。
     * 有效计划与历史释放记录保护整台物理机，历史结果映射仅在结果仍有效时提供保护。
     *
     * @param context 本批有效历史计划、释放事件及结果来源
     * @return 需要豁免专供转产下机的物理机台编码集合
     */
    private Set<String> historicalProtectedMachines(LhScheduleContext context) {
        Set<String> machines = new HashSet<>();
        context.getEligiblePreviousAlternatePlans().stream().filter(Objects::nonNull)
                .map(plan -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()))
                .filter(StringUtils::isNotBlank).forEach(machines::add);
        context.getPreviousAlternateReleaseEventMap().keySet().stream()
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotBlank).forEach(machines::add);
        Set<LhScheduleResult> currentResults = Collections.newSetFromMap(new IdentityHashMap<>());
        currentResults.addAll(context.getScheduleResultList());
        context.getPreviousAlternateResultPlanMap().keySet().stream().filter(Objects::nonNull)
                .filter(currentResults::contains)
                .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                .filter(StringUtils::isNotBlank).forEach(machines::add);
        return machines;
    }

    /**
     * 统计全部正量结果的同胎胚物理机台数，保留截断后仍有正量的原胎胚登记。
     *
     * @param context 实时结果
     * @return 胎胚对应去重物理机台数
     */
    Map<String, Long> embryoMachineCounts(LhScheduleContext context) {
        return this.embryoMachineIndex(context).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> (long) entry.getValue().size()));
    }

    /**
     * 单次构建胎胚与物理机台集合索引。
     *
     * @param context 当前正量结果及SKU来源
     * @return 胎胚物理机台索引
     */
    private Map<String, Set<String>> embryoMachineIndex(LhScheduleContext context) {
        Map<String, Set<String>> machines = new HashMap<>();
        context.getScheduleResultList().forEach(result -> this.indexEmbryoMachine(context, machines, result));
        return machines;
    }

    /**
     * 成功后只替换受影响物理机台的胎胚登记，保留其他机台索引。
     *
     * @param context 实时结果
     * @param machines 本步骤胎胚索引
     * @param machineCode 本次处理的运行侧
     */
    private void refreshEmbryoMachines(LhScheduleContext context, Map<String, Set<String>> machines,
            String machineCode) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        machines.values().forEach(codes -> codes.remove(physical));
        context.getScheduleResultList().stream().filter(result -> physical.equals(
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                .forEach(result -> this.indexEmbryoMachine(context, machines, result));
    }

    /**
     * 仅登记存在正量且能够回溯胎胚的真实生产结果。
     *
     * @param context 来源映射
     * @param machines 待更新索引
     * @param result 当前结果
     */
    private void indexEmbryoMachine(LhScheduleContext context, Map<String, Set<String>> machines,
            LhScheduleResult result) {
        SkuScheduleDTO sku = context.getScheduleResultSourceSkuMap().get(result);
        if (ShiftFieldUtil.resolveScheduledQty(result) > 0 && sku != null
                && StringUtils.isNotBlank(sku.getEmbryoCode())) {
            machines.computeIfAbsent(sku.getEmbryoCode(), key -> new HashSet<>()).add(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()));
        }
    }

    /**
     * 余量收尾与胎胚收尾属于同一优先层。
     *
     * @param context 运行态胎胚收尾快照
     * @param results 当前SKU结果
     * @return 收尾为零，其他为一
     */
    int endingRank(LhScheduleContext context, List<LhScheduleResult> results) {
        return results.stream().anyMatch(result -> "1".equals(result.getIsEnd()))
                || Integer.valueOf(1).equals(context.getEmbryoEndingFlagMap().get(
                        this.source(context, results).getEmbryoCode())) ? 0 : 1;
    }

    /**
     * 读取末段原始需求对象，避免以结果数量重建需求。
     *
     * @param context 来源映射
     * @param rows 末段结果
     * @return 原SKU对象
     */
    private SkuScheduleDTO source(LhScheduleContext context, List<LhScheduleResult> rows) {
        return context.getScheduleResultSourceSkuMap().get(rows.get(0));
    }

    /**
     * 单物理组完整尝试，时间后移时恢复基线重新截断，任何失败均恢复原结果和资源。
     *
     * @param context 当前上下文
     * @param rows 原料末段
     * @param pool 冻结候选身份
     * @param days 有序业务日
     * @param dayIndex 当前下机业务日偏移
     * @param shift 当前下机班次
     * @return 原子提交成功时为真
     */
    boolean tryMachine(LhScheduleContext context, List<LhScheduleResult> rows,
            List<SkuScheduleDTO> pool, List<DayScheduleContext> days, int dayIndex, LhShiftConfigVO shift) {
        if (!NewProductionDateAdmissionPolicy.isAllowed(context, days.get(dayIndex).getScheduleDate())) {
            return false;
        }
        SkuScheduleDTO source = this.source(context, rows);
        String machineCode = rows.get(0).getLhMachineCode();
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        // 候选收集后、真正截断前再次核对，整组失败后的单侧尝试同样不能绕过历史指定保护。
        if (this.historicalProtectedMachines(context).contains(physical)) {
            log.info("专供转产豁免历史指定机台, factoryCode={}, batchNo={}, machine={}, sku={}",
                    context.getFactoryCode(), context.getBatchNo(), machineCode,
                    StructureSwitchCandidateScope.skuKey(source));
            return false;
        }
        Date supply = NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, source);
        // 不撤销本段已经完成的换模/首检，最早从本段首个完整硫化周期结束后下机。
        Date floor = rows.stream().map(row -> context.getScheduleWindowShifts().stream()
                .map(windowShift -> ShiftFieldUtil.getShiftStartTime(row, windowShift.getShiftIndex()))
                .filter(Objects::nonNull).min(Date::compareTo).orElse(null))
                .filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        if (floor != null && source.getLhTimeSeconds() > 0) {
            floor = new Date(floor.getTime() + source.getLhTimeSeconds() * 1000L);
        }
        if (floor == null || days.isEmpty() || !supply.before(days.get(days.size() - 1).getDayEndTime())) {
            return false;
        }
        DayScheduleContext day = days.get(dayIndex);
        Date originalEnd = rows.stream().map(LhScheduleResult::getSpecEndTime)
                .max(Date::compareTo).get();
        Date initialCut = originalEnd;
        if (shift.resolveShiftTypeEnum() == ShiftEnum.AFTERNOON_SHIFT) {
            Date preferred = StructureEndingContinuationPolicy.firstDeadline(shift);
            initialCut = originalEnd.before(preferred) ? originalEnd : preferred;
        }
        if (initialCut.before(floor) || initialCut.before(shift.getShiftStartDateTime())) {
            return false;
        }
        List<SkuScheduleDTO> captured = new ArrayList<>(pool);
        captured.add(source);
        ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(context, captured);
        Map<String, int[]> beforeCounts = new HashMap<>();
        context.getDailyMouldChangeCountMap().forEach((date, counts) -> beforeCounts.put(date, counts.clone()));
        boolean committed = false;
        try {
            Date cut = initialCut;
            Set<String> rejected = new HashSet<>();
            while (!cut.after(shift.getShiftEndDateTime())) {
                snapshot.restore(context);
                context.setCurrentScheduleDate(day.getDayShifts().get(0).getWorkDate());
                ContinuationCutoverResult cutover = cutoverService.cutoverResults(context, source,
                        physical, cut, rows, TRANSFER_ANALYSIS);
                Date offline = cut;
                rows.forEach(row -> {
                    row.setSpecEndTime(offline);
                    if (cutover.getRemovedQty() > 0) {
                        row.setIsEnd("0");
                        ShiftFieldUtil.applyLastPlannedShiftEndMark(row, false);
                    }
                    context.getMachineScheduleMap().get(row.getLhMachineCode()).setEstimatedEndTime(offline);
                });
                if (context.getMouldResourceContext() != null) {
                    context.getMouldResourceContext().refreshAvailability(context);
                }
                capsuleReplacementRuleService.rebuildRuntimeState(context, null);
                context.rebuildScheduledMachineCountMaps(context.getScheduleWindowShifts());
                alignmentService.prepareStructureEndingAlignmentIndex(context);
                NewSpecScheduleProposal proposal = this.select(context, day, rows, source, pool, supply, rejected);
                if (proposal == null) {
                    log.debug("专供转产无合法后料, batchNo={}, machine={}, sourceSku={}, date={}, supply={}",
                            context.getBatchNo(), machineCode, StructureSwitchCandidateScope.skuKey(source),
                            day.getScheduleDate(), supply);
                    break;
                }
                Date actualCut = proposal.getAvailabilityPlan().getChangeoverStartTime();
                if (actualCut == null || actualCut.before(cut)
                        || !this.changeStartsInShift(context, shift, actualCut)) {
                    rejected.add(proposal.getCandidate().getSkuKey());
                    cut = initialCut;
                    continue;
                }
                if (actualCut.after(cut)) {
                    cut = actualCut;
                    continue;
                }
                Set<LhScheduleResult> before = Collections.newSetFromMap(new IdentityHashMap<>());
                before.addAll(context.getScheduleResultList());
                int bindingStart = context.getPreScheduledMachineBindingList().size();
                NewSpecMachineAssignmentPlan assignment = proposal.getAssignmentPlan().forPostProcessing();
                day.setCurrentPhase(assignment.getCandidate().hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                        ? DailySchedulePhase.EARLY_PRODUCTION : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
                boolean scheduled = this.withCandidateScope(context, assignment.getCandidate().getSku(),
                        () -> productionStrategy.executeMachineAssignment(context, day, assignment,
                                strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                                strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy()));
                if (!scheduled) {
                    rejected.add(proposal.getCandidate().getSkuKey());
                    cut = initialCut;
                    continue;
                }
                List<ActiveMachineBinding> bindings = new ArrayList<>(context.getPreScheduledMachineBindingList()
                        .subList(bindingStart, context.getPreScheduledMachineBindingList().size()));
                for (int next = dayIndex + 1; next < days.size(); next++) {
                    DayScheduleContext nextDay = days.get(next);
                    context.setCurrentScheduleDate(nextDay.getDayShifts().get(0).getWorkDate());
                    this.withCandidateScope(context, assignment.getCandidate().getSku(),
                            () -> productionStrategy.continueSpecifiedMachines(context, nextDay, bindings));
                }
                List<LhScheduleResult> added = context.getScheduleResultList().stream()
                        .filter(result -> !before.contains(result) && ShiftFieldUtil.resolveScheduledQty(result) > 0)
                        .collect(Collectors.toList());
                if (!this.validResults(context, added, supply, cut, beforeCounts)) {
                    rejected.add(proposal.getCandidate().getSkuKey());
                    cut = initialCut;
                    continue;
                }
                context.getDedicatedSupplyTransferResults().addAll(added);
                int remaining = quantityResolver.previewUnfulfilledProductionRemainingQty(context, source);
                if (cutover.getRemovedQty() > 0 && remaining > 0) {
                    unscheduledCollector.addOrReplace(context, source, unscheduledCollector.buildResult(context,
                            source, remaining, unscheduledCollector.resolveReasonCode(TRANSFER_ANALYSIS),
                            TRANSFER_ANALYSIS, "原料截断量已恢复共享需求账本"));
                }
                this.reconcileTargetUnscheduled(context, assignment.getCandidate().getSku());
                unscheduledCollector.finalizeResults(context);
                committed = true;
                log.info("专供转产成功, factoryCode={}, batchNo={}, machine={}, beforeSku={}, afterSku={}, offline={}, supply={}, restoredQty={}",
                        context.getFactoryCode(), context.getBatchNo(), machineCode,
                        StructureSwitchCandidateScope.skuKey(source), proposal.getCandidate().getSkuKey(),
                        cut, supply, cutover.getRemovedQty());
                return true;
            }
            return false;
        } finally {
            if (!committed) {
                snapshot.restore(context);
            }
        }
    }

    /**
     * 按公共禁换模起点规则校验本班接续，含预热的准备时长继续由公共时间轴处理。
     *
     * @param context 换模时间参数
     * @param shift 当前下机班次
     * @param start 换模开始
     * @return 在当前班释放且换模开始时间合法时为真
     */
    boolean changeStartsInShift(LhScheduleContext context, LhShiftConfigVO shift, Date start) {
        return start != null && !start.before(shift.getShiftStartDateTime())
                && !start.after(shift.getShiftEndDateTime())
                && !LhScheduleTimeUtil.isNoMouldChangeTime(context, start);
    }

    /**
     * 把后料真实余额同步到同一需求的全部未排投影，避免窗口归并继续取补排前的旧最大量。
     * 本方法只更新展示投影，生产量和日计划账本已由公共提交内核扣减，不能再次消费。
     *
     * @param context 成功提交后的数量账本及未排结果
     * @param sku 本次成功补排的后料
     */
    void reconcileTargetUnscheduled(LhScheduleContext context, SkuScheduleDTO sku) {
        int remaining = quantityResolver.previewUnfulfilledProductionRemainingQty(context, sku);
        if (remaining <= 0) {
            unscheduledCollector.remove(context, sku);
            return;
        }
        List<LhUnscheduledResult> matching = context.getUnscheduledResultList().stream()
                .filter(result -> unscheduledCollector.isSameDemand(context, sku, result))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            UnscheduledReasonEnum reason = UnscheduledReasonEnum.PARTIAL_UNSCHEDULED;
            unscheduledCollector.add(context, sku, unscheduledCollector.buildResult(context, sku, remaining,
                    reason, reason.getSummary(), TRANSFER_ANALYSIS + "，剩余量=" + remaining));
        } else {
            // 保留已有未排原因和原始需求身份，仅纠正已成功补排后的剩余数量。
            matching.forEach(result -> result.setUnscheduledQty(remaining));
        }
        log.info("专供转产后料未排量同步, batchNo={}, sku={}, remainingQty={}, existingProjectionCount={}",
                context.getBatchNo(), StructureSwitchCandidateScope.skuKey(sku), remaining, matching.size());
    }

    /**
     * 使用候选原有资格和真实时间轴形成提案，结构切换剩余候选沿用原分班约束。
     *
     * @param context 已临时截断的上下文
     * @param day 当前业务日
     * @param rows 允许使用的运行侧
     * @param source 当前下机SKU
     * @param pool 入口冻结的候选
     * @param supply 本次转产硬供胚下限
     * @param rejected 本机本日已失败SKU
     * @return 最佳合法提案，无候选时为空
     */
    NewSpecScheduleProposal select(LhScheduleContext context, DayScheduleContext day,
            List<LhScheduleResult> rows, SkuScheduleDTO source, List<SkuScheduleDTO> pool,
            Date supply, Set<String> rejected) {
        List<NewSpecScheduleProposal> proposals = new ArrayList<>();
        Set<String> allowed = rows.stream().map(LhScheduleResult::getLhMachineCode).collect(Collectors.toSet());
        NewSpecProposalRoundCache cache = new NewSpecProposalRoundCache(pool.size());
        String machineCode = rows.get(0).getLhMachineCode();
        LhMachineInfo info = context.getMachineInfoMap().get(machineCode);
        if (info == null) {
            info = context.getMachineInfoMap().get(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
        }
        if (info == null || StringUtils.isBlank(info.getDimensionSize())) {
            return null;
        }
        BigDecimal dimension = new BigDecimal(info.getDimensionSize().trim());
        for (SkuScheduleDTO sku : pool) {
            String key = StructureSwitchCandidateScope.skuKey(sku);
            if (StringUtils.equals(sku.getMaterialCode(), source.getMaterialCode()) || rejected.contains(key)
                    || quantityResolver.previewUnfulfilledProductionRemainingQty(context, sku) <= 0) {
                continue;
            }
            NewSpecScheduleProposal proposal = this.withCandidateScope(context, sku,
                    () -> this.previewCandidate(context, day, machineCode, allowed, sku, supply, cache, dimension));
            if (proposal != null) {
                proposals.add(proposal);
            }
        }
        return competitionService.selectRoundWinner(context, proposals, cache);
    }

    /**
     * 按候选原入口执行资格、机台缺口与资源预演，不授予历史指定豁免。
     *
     * @param context 临时释放后的上下文
     * @param day 当前业务日
     * @param machineCode 待接续机台
     * @param allowed 允许使用的运行侧集合
     * @param sku 后料候选
     * @param supply 原转产结构的供胚硬下限
     * @param cache 本轮只读预演缓存
     * @param dimension 机台尺寸，沿用普通完整适配比较规则
     * @return 可行提案；资格或资源不足时为空
     */
    private NewSpecScheduleProposal previewCandidate(LhScheduleContext context, DayScheduleContext day,
            String machineCode, Set<String> allowed, SkuScheduleDTO sku, Date supply,
            NewSpecProposalRoundCache cache, BigDecimal dimension) {
        eligibilityService.prepareNormalPlanOnDueDate(context, sku, day.getScheduleDate());
        DailyNewSpecCandidate candidate = eligibilityService.resolveCandidate(context, sku, day.getScheduleDate());
        candidatePoolBuilder.refreshRemainingMachineCount(context, day.getScheduleDate(), candidate);
        if (candidate.getReasons().isEmpty() || candidate.getRemainingMachineCount() <= 0) {
            log.debug("专供转产候选准入拒绝, batchNo={}, machine={}, sku={}, date={}, reasons={}, remainingMachines={}, failure={}",
                    context.getBatchNo(), machineCode, candidate.getSkuKey(), day.getScheduleDate(),
                    candidate.getReasons(), candidate.getRemainingMachineCount(), candidate.getLastFailure());
            return null;
        }
        day.setCurrentPhase(candidate.hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                ? DailySchedulePhase.EARLY_PRODUCTION : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
        NewSpecMachineAssignmentPlan assignment = productionStrategy.previewMachineAssignment(context, day,
                candidate, context.getMachineScheduleMap().get(machineCode),
                strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy(),
                supply, false, cache);
        if (assignment == null || !allowed.equals(new HashSet<>(assignment.getMatchResult().getDeclaredMachineCodes()))) {
            log.debug("专供转产候选资源拒绝, batchNo={}, machine={}, sku={}, date={}, failure={}",
                    context.getBatchNo(), machineCode, candidate.getSkuKey(), day.getScheduleDate(), candidate.getLastFailure());
            return null;
        }
        // 提前生产的需求日期随公共预演保留，不能改写为当前资源竞争日。
        return new NewSpecScheduleProposal(candidate, assignment.getMatchResult(), assignment.getTargetShiftIndex(),
                assignment.getPoolDate(), assignment.getAvailabilityPlan(), true, dimension);
    }

    /**
     * 已冻结的结构切换后料沿用原切换作用域，预演、提交和续排使用同一入口契约。
     * 作用域只解除普通入口的归属分流，供胚、分班名额、模具及换模限额仍由原内核校验。
     *
     * @param context 本批上下文
     * @param sku 当前后料
     * @param action 单候选操作
     * @param <T> 操作结果类型
     * @return 操作返回值
     */
    <T> T withCandidateScope(LhScheduleContext context, SkuScheduleDTO sku, Supplier<T> action) {
        boolean previous = context.isStructureSwitchScheduling();
        try {
            if (StructureSwitchCandidateScope.owns(context, sku)) {
                context.setStructureSwitchScheduling(true);
            }
            return action.get();
        } finally {
            context.setStructureSwitchScheduling(previous);
        }
    }

    /**
     * 复核提交后的真实起点及换模总次数，零量结果不能视为成功。
     *
     * @param context 已提交资源
     * @param added 新生成结果
     * @param supply 供胚硬下限
     * @param cut 原料截断时刻
     * @param beforeCounts 本次尝试前计数，仅检查本次增加的日期
     * @return 时间与资源均合法时为真
     */
    boolean validResults(LhScheduleContext context, List<LhScheduleResult> added, Date supply, Date cut,
            Map<String, int[]> beforeCounts) {
        return !added.isEmpty() && added.stream().allMatch(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0
                && Objects.equals(cut, result.getMouldChangeStartTime())
                && context.getScheduleWindowShifts().stream().allMatch(shift -> {
                    Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                    return start == null || (!start.before(supply) && !start.before(cut));
                })) && context.getDailyMouldChangeCountMap().entrySet().stream().allMatch(entry -> {
                    int[] count = entry.getValue();
                    int[] previous = beforeCounts.getOrDefault(entry.getKey(), new int[]{0, 0});
                    if (count[0] <= previous[0] && count[1] <= previous[1]) {
                        return true;
                    }
                    return count[0] <= LhScheduleTimeUtil.getMorningMouldChangeLimit(context, entry.getKey())
                            && count[1] <= LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, entry.getKey())
                            && count[0] + count[1] <= LhScheduleTimeUtil.getDailyMouldChangeLimit(context, entry.getKey());
                });
    }
}
