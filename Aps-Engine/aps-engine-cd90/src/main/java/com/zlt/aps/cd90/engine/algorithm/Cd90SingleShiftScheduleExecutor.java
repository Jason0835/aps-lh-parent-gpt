package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90CloseOutDecision;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoCloseOutItem;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoPlanSurplus;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialPlan;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialRequest;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90ShiftCommitRequest;
import com.zlt.aps.cd90.engine.model.Cd90ShiftCommitResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90MachineResourceService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleCandidatePreparationService;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import com.zlt.aps.cd90.engine.service.Cd90SingleShiftScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 单个直裁班次的候选规格执行器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90SingleShiftScheduleExecutor implements Cd90SingleShiftScheduleService {

    private final Cd90ScheduleCandidatePreparationService candidatePreparationService;
    private final Cd90ShiftDemandProvider demandProvider;
    private final Cd90MachineResourceService machineResourceService;
    private final Cd90MachineTrialPreparationService trialPreparationService;
    private final Cd90ShiftResourceCommitter resourceCommitter;
    private final Cd90CloseOutCalculator closeOutCalculator;
    private final Cd90ScheduleCandidateSorter candidateSorter;

    /**
     * 逐规格执行机台试算和资源原子提交；规格失败不会中断后续候选。
     */
    @Override
    public Cd90ShiftExecutionResult execute(Cd90AutoScheduleContext context,
                                            Cd90AutoScheduleInput input,
                                            Cd90ShiftDescriptor shift,
                                            Cd90ShiftResourceState initialState,
                                            Cd90RollingScheduleContext rolling) {
        validate(context, input, shift, initialState);
        // 候选已按当前班缺料时间、续作优先和库存保障时长排序，后续必须保持该稳定顺序执行。
        List<Cd90ScheduleCandidate> candidates = new ArrayList<>(candidatePreparationService.prepare(
                context, input, shift.getClassField(), rolling));
        attachBigRollCodes(candidates, input.getConstructionMaterials());
        Map<String, BigDecimal> continueDemandByCloth = copyContinueDemand(rolling);
        Map<String, String> lastMachineByCloth = copyLastMachineByCloth(rolling);
        appendContinueCandidates(candidates, continueDemandByCloth, lastMachineByCloth);
        attachBigRollCodes(candidates, input.getConstructionMaterials());
        Deque<Cd90ScheduleCandidate> immediateContinueCandidates = new ArrayDeque<>();
        // 机台快照在班次开始时一次加载，规格之间通过state扣减剩余秒数，避免重复查询造成漂移。
        Cd90MachineResourceSnapshot machineSnapshot = machineResourceService.load(
                context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
        Cd90ShiftResourceState state = initialState;
        Map<String, String> failures = new LinkedHashMap<>();
        List<Cd90ScheduleAttemptTrace> attemptTraces = new ArrayList<>();

        log.info("[直裁自动排程] 当前班次执行开始, classField={}, shiftCode={}, candidateCount={}",
                shift.getClassField(), shift.getShiftCode(), candidates.size());
        while (!candidates.isEmpty() || !immediateContinueCandidates.isEmpty()) {
            // 本班真实部分排产生的续作要尽量贴在上一段任务尾部继续排，避免被普通候选插队。
            Cd90ScheduleCandidate candidate = selectCandidate(candidates, immediateContinueCandidates, state);
            String clothCode = candidate == null ? null : candidate.getClothCode();
            if (!StringUtils.hasText(clothCode)) {
                continue;
            }
            // 优先使用前序真实部分排留下的续作需求；没有续作时再按库存和前序入库计算本班净需求。
            attachSourceMachine(candidate, lastMachineByCloth);
            boolean continueDemand = isContinueDemand(candidate, continueDemandByCloth);
            BigDecimal rawDemand = resolveDemandQuantity(
                    context, input, shift, candidate, rolling, continueDemandByCloth);
            if (rawDemand.signum() <= 0) {
                log.info("[直裁自动排程] 当前班次规格净需求为0，跳过资源试算, classField={}, shiftCode={}, clothCode={}, netDemand={}",
                        shift.getClassField(), shift.getShiftCode(), clothCode, rawDemand);
                continue;
            }
            Cd90ConstructionMaterial construction = findConstruction(
                    input.getConstructionMaterials(), clothCode);
            if (construction == null) {
                // 数据缺失只终止当前规格，保留轨迹后继续处理后续候选。
                String reason = "CONSTRUCTION_MISSING";
                recordFailure(failures, shift, clothCode, reason);
                attemptTraces.add(trace(shift, clothCode, null, rawDemand,
                        BigDecimal.ZERO, reason, attemptTraces.size() + 1));
                continue;
            }
            BigDecimal netDemand = continueDemand ? normalize(rawDemand)
                    : roundUpByCraftWidth(shift, clothCode, rawDemand, construction.getCraftWidth());
            if (input.getBigRollAgingDataMissingCodes() != null
                    && input.getBigRollAgingDataMissingCodes().contains(construction.getBigRollCode())) {
                String reason = "DATA_MISSING";
                recordFailure(failures, shift, clothCode, reason);
                attemptTraces.add(trace(shift, clothCode, construction.getBigRollCode(),
                        netDemand, BigDecimal.ZERO, reason, attemptTraces.size() + 1));
                continue;
            }
            Cd90CloseOutDecision closeOut = closeOutCalculator.decide(
                    closeOutItems(clothCode, input));
            if (closeOut.isMissingPlanSurplusWarning()) {
                log.warn("[直裁自动排程] 月计划剩余量缺失, classField={}, clothCode={}",
                        shift.getClassField(), clothCode);
            }
            log.info("[直裁自动排程] 规格收尾判断完成, classField={}, clothCode={}, closeOut={}, details={}",
                    shift.getClassField(), clothCode, closeOut.isCloseOut(), closeOut.getEmbryoItems());
            // 先生成全部可行机台试算，再由资源提交器按优先级逐个尝试库排和工装占用。
            Cd90MachineTrialPlan trialPlan = trialPreparationService.prepare(
                    trialRequest(context, shift, state, construction, netDemand, closeOut.isCloseOut(), candidate, rolling),
                    machineSnapshot);
            Cd90ShiftCommitResult commit = resourceCommitter.commit(
                    commitRequest(context, shift, construction, trialPlan, closeOut.isCloseOut()), state);
            if (!commit.isSuccess()) {
                // 提交失败返回原state，因此失败规格不会污染后续规格的机台、库排和工装资源。
                recordFailure(failures, shift, clothCode, commit.getFailureReason());
                attemptTraces.add(trace(shift, clothCode, construction.getBigRollCode(),
                        netDemand, BigDecimal.ZERO, commit.getFailureReason(),
                        attemptTraces.size() + 1));
                continue;
            }
            // 只有完整通过机台、库排和工装校验后，才用新状态替换当前班资源。
            state = commit.getState();
            BigDecimal scheduledQuantity = commit.getTask().getPlanQuantity();
            String partialReason = commit.getPartialReason();
            if (scheduledQuantity.compareTo(netDemand) < 0 && !StringUtils.hasText(partialReason)) {
                partialReason = "EQUAL_SHARE";
            }
            attemptTraces.add(trace(shift, clothCode, construction.getBigRollCode(),
                    netDemand, scheduledQuantity, null, partialReason,
                    attemptTraces.size() + 1));
            handleContinueDemand(shift, candidate, netDemand, scheduledQuantity, commit.getTask().getMachineCode(), partialReason,
                    continueDemandByCloth, immediateContinueCandidates);
        }
        saveContinueDemand(rolling, continueDemandByCloth);
        log.info("[直裁自动排程] 当前班次执行完成, classField={}, taskCount={}, failureCount={}",
                shift.getClassField(), state.getTasks().size(), failures.size());
        return Cd90ShiftExecutionResult.builder().shift(shift).state(state)
                .tasks(state.getTasks()).failures(failures)
                .attemptTraces(attemptTraces).build();
    }

    private Cd90ScheduleCandidate selectCandidate(List<Cd90ScheduleCandidate> candidates,
                                                  Deque<Cd90ScheduleCandidate> immediateContinueCandidates,
                                                  Cd90ShiftResourceState state) {
        if (immediateContinueCandidates != null && !immediateContinueCandidates.isEmpty()) {
            return immediateContinueCandidates.removeFirst();
        }
        List<Cd90ScheduleCandidate> sorted = candidateSorter.sort(candidates, this.continuityTails(state));
        candidates.clear();
        candidates.addAll(sorted);
        return candidates.remove(0);
    }

    private BigDecimal resolveDemandQuantity(Cd90AutoScheduleContext context,
                                             Cd90AutoScheduleInput input,
                                             Cd90ShiftDescriptor shift,
                                             Cd90ScheduleCandidate candidate,
                                             Cd90RollingScheduleContext rolling,
                                             Map<String, BigDecimal> continueDemandByCloth) {
        BigDecimal continueDemand = continueDemandByCloth.get(candidate.getClothCode());
        if (continueDemand != null && continueDemand.signum() > 0) {
            log.info("[直裁自动排程] 使用续作需求进入本班试算, classField={}, shiftCode={}, clothCode={}, continueDemand={}",
                    shift.getClassField(), shift.getShiftCode(), candidate.getClothCode(), continueDemand);
            return continueDemand;
        }
        Cd90ShiftDemandDecision demand = demandProvider.resolve(context, input, shift, candidate, rolling);
        return demand == null || demand.getNetDemandQuantity() == null
                ? BigDecimal.ZERO : demand.getNetDemandQuantity();
    }

    private BigDecimal roundUpByCraftWidth(Cd90ShiftDescriptor shift,
                                           String clothCode,
                                           BigDecimal rawDemand,
                                           BigDecimal craftWidth) {
        if (rawDemand == null || rawDemand.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (craftWidth == null || craftWidth.signum() <= 0) {
            log.warn("[直裁自动排程] 直裁宽度缺失，无法按整条取整，沿用原始净需求, classField={}, clothCode={}, rawDemand={}",
                    shift.getClassField(), clothCode, rawDemand);
            return rawDemand;
        }
        // craftWidth来源施工表TIRE_FABRIC_CRAFT1/2/3，单位毫米，需转为米后与rawDemand（单位米）运算。
        BigDecimal craftWidthInMeters = craftWidth.divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        BigDecimal stripCount = rawDemand.divide(craftWidthInMeters, 0, RoundingMode.CEILING);
        BigDecimal roundedDemand = this.normalize(stripCount.multiply(craftWidthInMeters));
        if (roundedDemand.compareTo(rawDemand) != 0) {
            log.info("[直裁自动排程] 净需求按直裁宽度向上取整, classField={}, clothCode={}, rawDemand={}, craftWidth={}mm, roundedDemand={}",
                    shift.getClassField(), clothCode, rawDemand, craftWidth, roundedDemand);
        }
        return roundedDemand;
    }

    private void handleContinueDemand(Cd90ShiftDescriptor shift,
                                      Cd90ScheduleCandidate candidate,
                                      BigDecimal netDemand,
                                      BigDecimal scheduledQuantity,
                                      String sourceMachineCode,
                                      String partialReason,
                                      Map<String, BigDecimal> continueDemandByCloth,
                                      Deque<Cd90ScheduleCandidate> immediateContinueCandidates) {
        BigDecimal remainingDemand = this.remainingDemand(netDemand, scheduledQuantity);
        if (remainingDemand.signum() <= 0 || "EQUAL_SHARE".equals(partialReason)) {
            continueDemandByCloth.remove(candidate.getClothCode());
            return;
        }
        continueDemandByCloth.put(candidate.getClothCode(), remainingDemand);
        candidate.setContinueFromPreviousShift(true);
        if (!StringUtils.hasText(candidate.getSourceMachineCode())) {
            candidate.setSourceMachineCode(sourceMachineCode);
        }
        // 机台产能不足时，本班不再把剩余量切到其他机台，留到后续班次优先回原机台续作。
        if (!"CAPACITY_LIMIT".equals(partialReason)) {
            immediateContinueCandidates.addFirst(candidate);
        }
        log.info("[直裁自动排程] 当前规格部分排后进入续作队列, classField={}, clothCode={}, sourceMachineCode={}, scheduledQuantity={}, remainingDemand={}, partialReason={}",
                shift.getClassField(), candidate.getClothCode(), candidate.getSourceMachineCode(), scheduledQuantity, remainingDemand, partialReason);
    }

    private BigDecimal remainingDemand(BigDecimal netDemand, BigDecimal scheduledQuantity) {
        BigDecimal safeNetDemand = netDemand == null ? BigDecimal.ZERO : netDemand;
        BigDecimal safeScheduledQuantity = scheduledQuantity == null ? BigDecimal.ZERO : scheduledQuantity;
        BigDecimal remaining = safeNetDemand.subtract(safeScheduledQuantity);
        return remaining.signum() <= 0 ? BigDecimal.ZERO : this.normalize(remaining);
    }

    private Map<String, BigDecimal> copyContinueDemand(Cd90RollingScheduleContext rolling) {
        if (rolling == null || rolling.getContinueDemandByCloth() == null) {
            return new LinkedHashMap<>();
        }
        return rolling.getContinueDemandByCloth().entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> second, LinkedHashMap::new));
    }
    private Map<String, String> copyLastMachineByCloth(Cd90RollingScheduleContext rolling) {
        if (rolling == null || rolling.getLastMachineByCloth() == null) {
            return new LinkedHashMap<>();
        }
        return rolling.getLastMachineByCloth().entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> second, LinkedHashMap::new));
    }

    private boolean isContinueDemand(Cd90ScheduleCandidate candidate,
                                     Map<String, BigDecimal> continueDemandByCloth) {
        BigDecimal continueDemand = candidate == null || continueDemandByCloth == null
                ? null : continueDemandByCloth.get(candidate.getClothCode());
        return continueDemand != null && continueDemand.signum() > 0;
    }

    private void attachSourceMachine(Cd90ScheduleCandidate candidate,
                                     Map<String, String> lastMachineByCloth) {
        if (candidate == null || StringUtils.hasText(candidate.getSourceMachineCode())
                || lastMachineByCloth == null) {
            return;
        }
        candidate.setSourceMachineCode(lastMachineByCloth.get(candidate.getClothCode()));
    }

    private String preferredHistoryMachineCode(Cd90ScheduleCandidate candidate,
                                               Cd90RollingScheduleContext rolling) {
        if (candidate == null) {
            return null;
        }
        if (StringUtils.hasText(candidate.getSourceMachineCode())) {
            return candidate.getSourceMachineCode();
        }
        if (rolling == null || rolling.getLastMachineByCloth() == null) {
            return null;
        }
        return rolling.getLastMachineByCloth().get(candidate.getClothCode());
    }

    private void appendContinueCandidates(List<Cd90ScheduleCandidate> candidates,
                                          Map<String, BigDecimal> continueDemandByCloth,
                                          Map<String, String> lastMachineByCloth) {
        if (continueDemandByCloth.isEmpty()) {
            return;
        }
        Set<String> existingClothCodes = this.safe(candidates).stream()
                .map(Cd90ScheduleCandidate::getClothCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        continueDemandByCloth.keySet().stream()
                .filter(clothCode -> !existingClothCodes.contains(clothCode))
                .map(clothCode -> Cd90ScheduleCandidate.builder()
                        .clothCode(clothCode).continueFromPreviousShift(true)
                        .sourceMachineCode(lastMachineByCloth.get(clothCode)).build())
                .forEach(candidates::add);
    }

    private void saveContinueDemand(Cd90RollingScheduleContext rolling,
                                    Map<String, BigDecimal> continueDemandByCloth) {
        if (rolling == null) {
            return;
        }
        Map<String, BigDecimal> validContinueDemand = continueDemandByCloth.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> second, LinkedHashMap::new));
        rolling.setContinueDemandByCloth(validContinueDemand);
        log.info("[直裁自动排程] 当前班次续作需求已保存, continueDemandByCloth={}", validContinueDemand);
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }

    private Cd90ScheduleAttemptTrace trace(Cd90ShiftDescriptor shift,
                                           String clothCode,
                                           String bigRollCode,
                                           BigDecimal netDemand,
                                           BigDecimal scheduled,
                                           String failureReason,
                                           int sequence) {
        return trace(shift, clothCode, bigRollCode, netDemand, scheduled,
                failureReason, null, sequence);
    }
    private Cd90ScheduleAttemptTrace trace(Cd90ShiftDescriptor shift,
                                           String clothCode,
                                           String bigRollCode,
                                           BigDecimal netDemand,
                                           BigDecimal scheduled,
                                           String failureReason,
                                           String partialReason,
                                           int sequence) {
        return Cd90ScheduleAttemptTrace.builder()
                .classField(shift.getClassField()).shiftCode(shift.getShiftCode())
                .clothCode(clothCode).bigRollCode(bigRollCode)
                .netDemandQuantity(netDemand).scheduledQuantity(scheduled)
                .failureReason(failureReason).partialReason(partialReason)
                .sequence(sequence).build();
    }

    private Cd90MachineTrialRequest trialRequest(Cd90AutoScheduleContext context,
                                                  Cd90ShiftDescriptor shift,
                                                  Cd90ShiftResourceState state,
                                                  Cd90ConstructionMaterial construction,
                                                  BigDecimal netDemand,
                                                  boolean closeOut,
                                                  Cd90ScheduleCandidate candidate,
                                                  Cd90RollingScheduleContext rolling) {
        return Cd90MachineTrialRequest.builder()
                .clothCode(construction.getClothCode())
                .bigRollCode(construction.getBigRollCode())
                .cordSpec(construction.getCordSpec())
                .craftWidth(construction.getCraftWidth())
                .curlLength(effectiveCurlLength(context, construction))
                .shiftCode(shift.getShiftCode())
                .shiftStart(shift.getStartTime()).shiftEnd(shift.getEndTime())
                .netDemandQuantity(netDemand).closeOut(closeOut)
                .occupiedVehicleCount(occupiedVehicles(state.getLanes()))
                .shiftHours(Math.max(1, shift.getDurationSeconds() / 3600))
                .remainingSecondsByMachine(state.getRemainingSecondsByMachine())
                .previousSpecByMachine(state.getTailSpecByMachine())
                .previousTailByMachine(state.getTailByMachine())
                .bigRollAgingStocks(state.getBigRollAgingStocks())
                .preferredHistoryMachineCode(preferredHistoryMachineCode(candidate, rolling))
                .parameters(context.getParameters()).build();
    }

    private Cd90ShiftCommitRequest commitRequest(Cd90AutoScheduleContext context,
                                                  Cd90ShiftDescriptor shift,
                                                  Cd90ConstructionMaterial construction,
                                                  Cd90MachineTrialPlan trialPlan,
                                                  boolean closeOut) {
        return Cd90ShiftCommitRequest.builder()
                .clothCode(construction.getClothCode())
                .bigRollCode(construction.getBigRollCode())
                .cordSpec(construction.getCordSpec())
                .classField(shift.getClassField())
                .shiftStart(shift.getStartTime()).shiftEnd(shift.getEndTime())
                .coilMeter(effectiveCurlLength(context, construction))
                .closeOut(closeOut)
                .partialMinVehicleCount(context.getParameters().getPartialMinVehicleCount())
                .trialPlan(trialPlan).build();
    }

    /**
     * 获取本规格实际采用的卷曲长度，单位米。
     * 优先使用t_cd90_curl_length维护的标准卷曲长度；没有维护时，才启用参数CRIMP_LENGTH作为兜底值。
     */
    private BigDecimal effectiveCurlLength(Cd90AutoScheduleContext context,
                                           Cd90ConstructionMaterial construction) {
        if (construction.getCurlLength() != null && construction.getCurlLength().signum() > 0) {
            return construction.getCurlLength();
        }
        BigDecimal fallback = context.getParameters().getRollCoilMeter();
        log.warn("[直裁自动排程] 当前规格未匹配到标准卷曲长度，使用参数CRIMP_LENGTH兜底, clothCode={}, bigRollCode={}, fallbackMeter={}",
                construction.getClothCode(), construction.getBigRollCode(), fallback);
        return fallback;
    }

    private Cd90ConstructionMaterial findConstruction(List<Cd90ConstructionMaterial> materials,
                                                       String clothCode) {
        return safe(materials).stream()
                .filter(item -> item != null && clothCode.equals(item.getClothCode()))
                .filter(item -> StringUtils.hasText(item.getBigRollCode())
                        && StringUtils.hasText(item.getCordSpec()))
                .findFirst().orElse(null);
    }

    /** 根据施工映射补充候选大卷，供非开产模式连续排序使用。 */
    private void attachBigRollCodes(List<Cd90ScheduleCandidate> candidates,
                                    List<Cd90ConstructionMaterial> materials) {
        for (Cd90ScheduleCandidate candidate : safe(candidates)) {
            Cd90ConstructionMaterial material = candidate == null ? null
                    : findConstruction(materials, candidate.getClothCode());
            if (material != null) {
                candidate.setBigRollCode(material.getBigRollCode());
            }
        }
    }

    /**
     * 当前班已有任务时只采用最后提交任务作为强连续参照；班次刚开始时使用全部跨班机台尾状态。
     */
    private List<Cd90MachineTailState> continuityTails(Cd90ShiftResourceState state) {
        if (state == null) {
            return Collections.emptyList();
        }
        if (state.getTasks() != null && !state.getTasks().isEmpty()) {
            com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask task =
                    state.getTasks().get(state.getTasks().size() - 1);
            return Collections.singletonList(Cd90MachineTailState.builder()
                    .clothCode(task.getClothCode()).bigRollCode(task.getBigRollCode()).build());
        }
        if (state.getTailByMachine() == null || state.getTailByMachine().isEmpty()) {
            return Collections.emptyList();
        }
        return state.getTailByMachine().values().stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private int occupiedVehicles(List<Cd90StorageLaneState> lanes) {
        return safe(lanes).stream().mapToInt(Cd90StorageLaneState::getVehicleCount).sum();
    }

    /** 汇总当前直裁规格关联胎胚的计划量，并与各自月计划剩余量配对。 */
    private List<Cd90EmbryoCloseOutItem> closeOutItems(String clothCode,
                                                       Cd90AutoScheduleInput input) {
        Set<String> embryoCodes = safe(input.getConstructionMaterials()).stream()
                .filter(item -> item != null && clothCode.equals(item.getClothCode()))
                .map(Cd90ConstructionMaterial::getConstructionCode)
                .filter(StringUtils::hasText).collect(Collectors.toCollection(HashSet::new));
        Map<String, BigDecimal> planByEmbryo = safe(input.getFormingSchedules()).stream()
                .filter(item -> item != null && embryoCodes.contains(item.getEmbryoCode()))
                .collect(Collectors.groupingBy(Cd90FormingScheduleSource::getEmbryoCode,
                        Collectors.reducing(BigDecimal.ZERO, this::sumClassPlanQuantities,
                                BigDecimal::add)));
        Map<String, BigDecimal> surplusByEmbryo = safe(input.getEmbryoPlanSurpluses()).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getEmbryoCode()))
                .collect(Collectors.toMap(Cd90EmbryoPlanSurplus::getEmbryoCode,
                        Cd90EmbryoPlanSurplus::getPlanSurplusQuantity, (first, second) -> second));
        return embryoCodes.stream().sorted().map(embryoCode -> Cd90EmbryoCloseOutItem.builder()
                .embryoCode(embryoCode)
                .calculatedPlanQuantity(planByEmbryo.getOrDefault(embryoCode, BigDecimal.ZERO))
                .planSurplusQuantity(surplusByEmbryo.get(embryoCode)).build())
                .collect(Collectors.toList());
    }

    private BigDecimal sumClassPlanQuantities(Cd90FormingScheduleSource source) {
        return safe(source.getClassPlanQuantities()).stream().filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recordFailure(Map<String, String> failures, Cd90ShiftDescriptor shift,
                               String clothCode, String reason) {
        String stableReason = StringUtils.hasText(reason) ? reason : "NO_AVAILABLE_MACHINE";
        failures.put(clothCode, stableReason);
        log.warn("[直裁自动排程] 当前班次规格排程失败, classField={}, clothCode={}, reason={}",
                shift.getClassField(), clothCode, stableReason);
    }

    private void validate(Cd90AutoScheduleContext context, Cd90AutoScheduleInput input,
                          Cd90ShiftDescriptor shift, Cd90ShiftResourceState state) {
        if (context == null || context.getParameters() == null || input == null
                || shift == null || state == null) {
            throw new IllegalArgumentException("单班排程上下文、输入、班次和资源状态不能为空");
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
