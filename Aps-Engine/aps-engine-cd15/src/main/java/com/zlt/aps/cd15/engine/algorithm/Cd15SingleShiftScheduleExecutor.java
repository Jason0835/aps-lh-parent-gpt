package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15CloseOutDecision;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15EmbryoCloseOutItem;
import com.zlt.aps.cd15.engine.model.Cd15EmbryoPlanSurplus;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialPlan;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialRequest;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitRequest;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.model.Cd15SplitCutGroup;
import com.zlt.aps.cd15.engine.service.Cd15MachineResourceService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleCandidatePreparationService;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import com.zlt.aps.cd15.engine.service.Cd15SingleShiftScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
 * 单个斜裁班次的候选规格执行器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15SingleShiftScheduleExecutor implements Cd15SingleShiftScheduleService {

    private final Cd15ScheduleCandidatePreparationService candidatePreparationService;
    private final Cd15ShiftDemandProvider demandProvider;
    private final Cd15MachineResourceService machineResourceService;
    private final Cd15MachineTrialPreparationService trialPreparationService;
    private final Cd15ShiftResourceCommitter resourceCommitter;
    private final Cd15CloseOutCalculator closeOutCalculator;
    private final Cd15ScheduleCandidateSorter candidateSorter;
    private final Cd15SplitCutGroupBuilder splitCutGroupBuilder;

    /**
     * 逐规格执行机台试算和资源原子提交；规格失败不会中断后续候选。
     */
    @Override
    public Cd15ShiftExecutionResult execute(Cd15AutoScheduleContext context,
                                            Cd15AutoScheduleInput input,
                                            Cd15ShiftDescriptor shift,
                                            Cd15ShiftResourceState initialState,
                                            Cd15RollingScheduleContext rolling) {
        return executePrepared(context, input, shift, initialState, rolling,
                null, Collections.emptyMap(), false);
    }

    /** 使用滚动规划器已排好序的候选执行单班资源试算。 */
    Cd15ShiftExecutionResult executePrepared(Cd15AutoScheduleContext context,
                                              Cd15AutoScheduleInput input,
                                              Cd15ShiftDescriptor shift,
                                              Cd15ShiftResourceState initialState,
                                              Cd15RollingScheduleContext rolling,
                                              List<Cd15ScheduleCandidate> preparedCandidates,
                                              Map<String, Cd15RollingPendingTask> sourceTasks,
                                              boolean preservePreparedOrder) {
        validate(context, input, shift, initialState);
        // 候选已按当前班缺料时间、续作优先和库存保障时长排序，后续必须保持该稳定顺序执行。
        Map<String, BigDecimal> continueDemandBySteelStrip = copyContinueDemand(rolling);
        Map<String, String> lastMachineBySteelStrip = copyLastMachineBySteelStrip(rolling);
        List<Cd15ScheduleCandidate> candidates;
        if (preparedCandidates == null) {
            candidates = new ArrayList<>(candidatePreparationService.prepare(
                    context, input, shift.getClassField(), rolling));
            appendContinueCandidates(candidates, continueDemandBySteelStrip,
                    lastMachineBySteelStrip, input.getConstructionMaterials());
        } else {
            candidates = new ArrayList<>(preparedCandidates);
        }
        attachConstructionFields(candidates, input.getConstructionMaterials());

        int candidateCount = candidates.size();
        Deque<Cd15ScheduleCandidate> immediateContinueCandidates = new ArrayDeque<>();
        Deque<Cd15ScheduleCandidate> shiftStartTailCandidates = preservePreparedOrder
                ? new ArrayDeque<>() : buildShiftStartTailCandidates(candidates, initialState);
        // 机台快照在班次开始时一次加载，规格之间通过state扣减剩余秒数，避免重复查询造成漂移。
        Cd15MachineResourceSnapshot machineSnapshot = machineResourceService.load(
                context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
        Cd15ShiftResourceState state = initialState;
        Map<String, String> failures = new LinkedHashMap<>();
        List<Cd15ScheduleAttemptTrace> attemptTraces = new ArrayList<>();

        log.info("[斜裁自动排程] 当前班次执行开始, classField={}, shiftCode={}, candidateCount={}",
                shift.getClassField(), shift.getShiftCode(), candidateCount);
        while (!candidates.isEmpty() || !immediateContinueCandidates.isEmpty()
                || !shiftStartTailCandidates.isEmpty()) {
            // 本班真实部分排产生的续作要尽量贴在上一段任务尾部继续排，避免被普通候选插队。
            Cd15ScheduleCandidate candidate = selectCandidate(candidates, immediateContinueCandidates,
                    shiftStartTailCandidates, state, preservePreparedOrder);
            String steelStripCode = candidate == null ? null : candidate.getSteelStripCode();
            if (!StringUtils.hasText(steelStripCode)) {
                continue;
            }
            java.util.Optional<Cd15SplitCutGroup> splitGroup = this.splitCutGroupBuilder.find(
                    candidate, candidates, machineSnapshot.getAngleWidthMaxByAngle(),
                    shift.getClassField());
            if (splitGroup.isPresent()) {
                Cd15ShiftResourceState splitState = this.tryExecuteSplitGroup(
                        context, input, shift, state, rolling, machineSnapshot,
                        splitGroup.get(), sourceTasks, failures, attemptTraces,
                        continueDemandBySteelStrip, immediateContinueCandidates);
                if (splitState != null) {
                    candidates.remove(splitGroup.get().getSecondCandidate());
                    state = splitState;
                    continue;
                }
            }
            // 优先使用前序真实部分排留下的续作需求；没有续作时再按库存和前序入库计算本班净需求。
            attachSourceMachine(candidate, lastMachineBySteelStrip);
            boolean continueDemand = isContinueDemand(candidate, continueDemandBySteelStrip);
            BigDecimal rawDemand = resolveDemandQuantity(
                    context, input, shift, candidate, rolling, continueDemandBySteelStrip);
            if (rawDemand.signum() <= 0) {
                log.info("[斜裁自动排程] 当前班次规格净需求为0，跳过资源试算, classField={}, shiftCode={}, steelStripCode={}, netDemand={}",
                        shift.getClassField(), shift.getShiftCode(), steelStripCode, rawDemand);
                continue;
            }
            Cd15ConstructionMaterial construction = findConstruction(
                    input.getConstructionMaterials(), candidate);
            if (construction == null) {
                // 数据缺失只终止当前规格，保留轨迹后继续处理后续候选。
                String reason = "CONSTRUCTION_MISSING";
                recordFailure(failures, shift, candidate, reason);
                attemptTraces.add(trace(shift, candidate, null, rawDemand,
                        BigDecimal.ZERO, reason, attemptTraces.size() + 1));
                continue;
            }
            BigDecimal netDemand;
            BigDecimal vehicleDemand;
            if (continueDemand || candidate.isNewSpecAdvanceQuantityNormalized()) {
                netDemand = this.normalize(rawDemand);
                vehicleDemand = netDemand;
            } else {
                BigDecimal unitLengthMm = construction.getUnitConsumeMillimeter();
                BigDecimal craftWidthMm = construction.getCraftWidth();
                BigDecimal unitLengthM = unitLengthMm.divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
                BigDecimal craftWidthM = craftWidthMm.divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
                BigDecimal pieceCount = rawDemand.divide(unitLengthM, 0, RoundingMode.CEILING);
                // netDemand: 钢带纵向消耗米数 = pieceCount x craftWidth (斜裁宽度方向)
                // 用于 closeOut 判断、续作剩余量计算和 trace 日志，方向为钢带走料方向。
                netDemand = this.normalize(pieceCount.multiply(craftWidthM));
                // vehicleDemand: 小车实际卷取米数 = pieceCount x unitLength (胎体长度方向)
                // 小车收取是头尾相连沿 TIRE_FABRIC_LENGTH 方向，不是 craftWidth 方向。
                // 此值仅用于日志复盘原需求方向；试算服务按施工数据独立计算单车等价量。
                vehicleDemand = this.normalize(pieceCount.multiply(unitLengthM));
                log.info("[斜裁自动排程] 净需求按单片长度取整并按斜裁宽度换算, classField={}, steelStripCode={}, rawDemand={}, unitLength={}mm, craftWidth={}mm, pieceCount={}, roundedDemand={}, vehicleDemand={}",
                        shift.getClassField(), steelStripCode, rawDemand, unitLengthMm, craftWidthMm, pieceCount, netDemand, vehicleDemand);
            }
            if (input.getBigRollAgingDataMissingCodes() != null
                    && input.getBigRollAgingDataMissingCodes().contains(construction.getBigRollCode())) {
                String reason = "DATA_MISSING";
                recordFailure(failures, shift, candidate, reason);
                attemptTraces.add(trace(shift, candidate, construction.getBigRollCode(),
                        netDemand, BigDecimal.ZERO, reason, attemptTraces.size() + 1));
                continue;
            }
            Cd15CloseOutDecision closeOut = closeOutCalculator.decide(
                    closeOutItems(steelStripCode, input));
            if (closeOut.isMissingPlanSurplusWarning()) {
                log.warn("[斜裁自动排程] 月计划剩余量缺失, classField={}, steelStripCode={}",
                        shift.getClassField(), steelStripCode);
            }
            log.info("[斜裁自动排程] 规格收尾判断完成, classField={}, steelStripCode={}, closeOut={}, details={}",
                    shift.getClassField(), steelStripCode, closeOut.isCloseOut(), closeOut.getEmbryoItems());
            // 先生成全部可行机台试算，再由资源提交器按优先级逐个尝试库排和工装占用。
            Cd15MachineTrialPlan trialPlan = trialPreparationService.prepare(
                    trialRequest(context, shift, state, construction, netDemand,
                            closeOut.isCloseOut(), candidate, rolling,
                            false),
                    machineSnapshot);
            String cutMode = "SINGLE";
            String splitGroupKey = null;
            Cd15ShiftCommitResult commit = resourceCommitter.commit(
                    commitRequest(context, shift, construction, candidate, trialPlan,
                            closeOut.isCloseOut(), cutMode, splitGroupKey), state);
            if (!commit.isSuccess()) {
                // 提交失败返回原state，因此失败规格不会污染后续规格的机台、库排和工装资源。
                recordFailure(failures, shift, candidate, commit.getFailureReason());
                attemptTraces.add(trace(shift, candidate, construction.getBigRollCode(),
                        netDemand, BigDecimal.ZERO, commit.getFailureReason(),
                        attemptTraces.size() + 1));
                continue;
            }
            // 只有完整通过机台、库排和工装校验后，才用新状态替换当前班资源。
            state = commit.getState();
            BigDecimal scheduledQuantity = commit.getTask().getPlanQuantity();
            String partialReason = commit.getPartialReason();
            Cd15RollingPendingTask sourceTask = sourceTasks == null
                    ? null : sourceTasks.get(candidate.getRollingTaskKey());
            if (sourceTask != null) {
                commit.getTask().setSourceTaskKey(sourceTask.getTaskKey());
                commit.getTask().setSourceResultId(sourceTask.getSourceResultId());
            }
            if (scheduledQuantity.compareTo(netDemand) < 0 && !StringUtils.hasText(partialReason)) {
                partialReason = "EQUAL_SHARE";
            }
            attemptTraces.add(trace(shift, candidate, construction.getBigRollCode(),
                    netDemand, scheduledQuantity, null, partialReason,
                    attemptTraces.size() + 1));
            this.handleRemainingDemand(shift, candidate, netDemand, scheduledQuantity,
                    commit.getTask().getMachineCode(), partialReason, rolling,
                    continueDemandBySteelStrip, immediateContinueCandidates);
        }
        saveContinueDemand(rolling, continueDemandBySteelStrip);
        log.info("[斜裁自动排程] 当前班次执行完成, classField={}, taskCount={}, failureCount={}",
                shift.getClassField(), state.getTasks().size(), failures.size());
        return Cd15ShiftExecutionResult.builder().shift(shift).state(state)
                .tasks(state.getTasks()).failures(failures)
                .attemptTraces(attemptTraces).build();
    }

    /**
     * 尝试把两条候选作为一个分裁组合原子提交。
     * 任一候选没有同机台可行方案时返回空状态，由主循环继续按普通单裁处理。
     */
    private Cd15ShiftResourceState tryExecuteSplitGroup(
            Cd15AutoScheduleContext context,
            Cd15AutoScheduleInput input,
            Cd15ShiftDescriptor shift,
            Cd15ShiftResourceState originalState,
            Cd15RollingScheduleContext rolling,
            Cd15MachineResourceSnapshot machineSnapshot,
            Cd15SplitCutGroup group,
            Map<String, Cd15RollingPendingTask> sourceTasks,
            Map<String, String> failures,
            List<Cd15ScheduleAttemptTrace> attemptTraces,
            Map<String, BigDecimal> continueDemandBySteelStrip,
            Deque<Cd15ScheduleCandidate> immediateContinueCandidates) {
        SplitPreparedCandidate first = this.prepareSplitCandidate(
                context, input, shift, group.getFirstCandidate(), rolling,
                continueDemandBySteelStrip);
        SplitPreparedCandidate second = this.prepareSplitCandidate(
                context, input, shift, group.getSecondCandidate(), rolling,
                continueDemandBySteelStrip);
        if (first == null || second == null) {
            return null;
        }

        Cd15MachineTrialPlan firstPlan = this.trialPreparationService.prepare(
                this.trialRequest(context, shift, originalState,
                        first.construction, first.netDemand, first.closeOut,
                        first.candidate, rolling, true),
                machineSnapshot);
        Cd15MachineTrialPlan secondPlan = this.trialPreparationService.prepare(
                this.trialRequest(context, shift, originalState,
                        second.construction, second.netDemand, second.closeOut,
                        second.candidate, rolling, true),
                machineSnapshot);
        Set<String> firstMachineCodes = this.safe(firstPlan.getTrials()).stream()
                .filter(item -> item != null
                        && item.getFinalSchedulableQuantity() != null
                        && item.getFinalSchedulableQuantity().signum() > 0)
                .map(com.zlt.aps.cd15.engine.model.Cd15MachineTrial::getMachineCode)
                .collect(Collectors.toSet());
        Set<String> secondMachineCodes = this.safe(secondPlan.getTrials()).stream()
                .filter(item -> item != null
                        && item.getFinalSchedulableQuantity() != null
                        && item.getFinalSchedulableQuantity().signum() > 0)
                .map(com.zlt.aps.cd15.engine.model.Cd15MachineTrial::getMachineCode)
                .collect(Collectors.toSet());
        if (firstMachineCodes.isEmpty() || !firstMachineCodes.equals(secondMachineCodes)) {
            return null;
        }
        com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult splitCommit =
                this.resourceCommitter.commitSplit(
                        this.commitRequest(context, shift, first.construction,
                                first.candidate, firstPlan, first.closeOut,
                                "SPLIT", group.getGroupKey()),
                        this.commitRequest(context, shift, second.construction,
                                second.candidate, secondPlan, second.closeOut,
                                "SPLIT", group.getGroupKey()),
                        originalState);
        if (!splitCommit.isSuccess()) {
            return null;
        }

        first.candidate.setCutMode("SPLIT");
        first.candidate.setSplitGroupKey(group.getGroupKey());
        second.candidate.setCutMode("SPLIT");
        second.candidate.setSplitGroupKey(group.getGroupKey());
        this.attachRollingSource(
                first.candidate, splitCommit.getFirstTask(), sourceTasks);
        this.attachRollingSource(
                second.candidate, splitCommit.getSecondTask(), sourceTasks);
        Cd15ShiftCommitResult firstCommit = Cd15ShiftCommitResult.builder()
                .success(true).state(splitCommit.getState())
                .task(splitCommit.getFirstTask())
                .partialReason(splitCommit.getFirstPartialReason()).build();
        Cd15ShiftCommitResult secondCommit = Cd15ShiftCommitResult.builder()
                .success(true).state(splitCommit.getState())
                .task(splitCommit.getSecondTask())
                .partialReason(splitCommit.getSecondPartialReason()).build();
        this.recordSplitSuccess(shift, first, firstCommit, rolling,
                attemptTraces, continueDemandBySteelStrip,
                immediateContinueCandidates);
        this.recordSplitSuccess(shift, second, secondCommit, rolling,
                attemptTraces, continueDemandBySteelStrip,
                immediateContinueCandidates);
        failures.remove(this.candidateKey(first.candidate));
        failures.remove(this.candidateKey(second.candidate));
        log.info("[斜裁自动排程] 分裁组合原子提交成功, classField={}, groupKey={}, "
                        + "machineCode={}, firstSteelStrip={}, secondSteelStrip={}, combinedWidth={}",
                shift.getClassField(), group.getGroupKey(),
                splitCommit.getFirstTask().getMachineCode(),
                first.candidate.getSteelStripCode(),
                second.candidate.getSteelStripCode(), group.getCombinedWidth());
        return splitCommit.getState();
    }

    private SplitPreparedCandidate prepareSplitCandidate(
            Cd15AutoScheduleContext context,
            Cd15AutoScheduleInput input,
            Cd15ShiftDescriptor shift,
            Cd15ScheduleCandidate candidate,
            Cd15RollingScheduleContext rolling,
            Map<String, BigDecimal> continueDemandBySteelStrip) {
        BigDecimal rawDemand = this.resolveDemandQuantity(
                context, input, shift, candidate, rolling, continueDemandBySteelStrip);
        if (rawDemand.signum() <= 0) {
            return null;
        }
        Cd15ConstructionMaterial construction = this.findConstruction(
                input.getConstructionMaterials(), candidate);
        if (construction == null
                || input.getBigRollAgingDataMissingCodes() != null
                && input.getBigRollAgingDataMissingCodes().contains(candidate.getBigRollCode())) {
            return null;
        }
        BigDecimal unitLengthMeter = construction.getUnitConsumeMillimeter()
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        BigDecimal craftWidthMeter = construction.getCraftWidth()
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        BigDecimal pieceCount = rawDemand.divide(
                unitLengthMeter, 0, RoundingMode.CEILING);
        BigDecimal netDemand = this.normalize(pieceCount.multiply(craftWidthMeter));
        Cd15CloseOutDecision closeOut = this.closeOutCalculator.decide(
                this.closeOutItems(candidate.getSteelStripCode(), input));
        return new SplitPreparedCandidate(
                candidate, construction, netDemand, closeOut.isCloseOut());
    }

    private Cd15MachineTrialPlan singleMachinePlan(
            Cd15MachineTrialPlan source, Cd15MachineTrial trial) {
        return Cd15MachineTrialPlan.builder()
                .trials(Collections.singletonList(trial))
                .selectedTrial(trial)
                .failureReason(source.getFailureReason())
                .boundMachineCodes(source.getBoundMachineCodes())
                .build();
    }

    private void attachRollingSource(
            Cd15ScheduleCandidate candidate,
            Cd15ShiftScheduleTask task,
            Map<String, Cd15RollingPendingTask> sourceTasks) {
        Cd15RollingPendingTask sourceTask = sourceTasks == null
                ? null : sourceTasks.get(candidate.getRollingTaskKey());
        if (sourceTask != null) {
            task.setSourceTaskKey(sourceTask.getTaskKey());
            task.setSourceResultId(sourceTask.getSourceResultId());
        }
    }

    private void alignSplitTasks(Cd15ShiftScheduleTask first,
                                 Cd15ShiftScheduleTask second) {
        int produceOrder = first.getProduceOrder();
        LocalDateTime expectedStart = first.getExpectedStartTime().isBefore(
                second.getExpectedStartTime())
                ? first.getExpectedStartTime() : second.getExpectedStartTime();
        LocalDateTime expectedEnd = first.getExpectedEndTime().isAfter(
                second.getExpectedEndTime())
                ? first.getExpectedEndTime() : second.getExpectedEndTime();
        first.setProduceOrder(produceOrder);
        second.setProduceOrder(produceOrder);
        first.setExpectedStartTime(expectedStart);
        second.setExpectedStartTime(expectedStart);
        first.setExpectedEndTime(expectedEnd);
        second.setExpectedEndTime(expectedEnd);
    }

    private void recordSplitSuccess(
            Cd15ShiftDescriptor shift,
            SplitPreparedCandidate prepared,
            Cd15ShiftCommitResult commit,
            Cd15RollingScheduleContext rolling,
            List<Cd15ScheduleAttemptTrace> attemptTraces,
            Map<String, BigDecimal> continueDemandBySteelStrip,
            Deque<Cd15ScheduleCandidate> immediateContinueCandidates) {
        BigDecimal scheduledQuantity = commit.getTask().getPlanQuantity();
        String partialReason = commit.getPartialReason();
        if (scheduledQuantity.compareTo(prepared.netDemand) < 0
                && !StringUtils.hasText(partialReason)) {
            partialReason = "EQUAL_SHARE";
        }
        attemptTraces.add(this.trace(
                shift, prepared.candidate, prepared.construction.getBigRollCode(),
                prepared.netDemand, scheduledQuantity, null, partialReason,
                attemptTraces.size() + 1));
        this.handleRemainingDemand(
                shift, prepared.candidate, prepared.netDemand, scheduledQuantity,
                commit.getTask().getMachineCode(), partialReason, rolling,
                continueDemandBySteelStrip, immediateContinueCandidates);
    }

    private static final class SplitPreparedCandidate {
        private final Cd15ScheduleCandidate candidate;
        private final Cd15ConstructionMaterial construction;
        private final BigDecimal netDemand;
        private final boolean closeOut;

        private SplitPreparedCandidate(
                Cd15ScheduleCandidate candidate,
                Cd15ConstructionMaterial construction,
                BigDecimal netDemand,
                boolean closeOut) {
            this.candidate = candidate;
            this.construction = construction;
            this.netDemand = netDemand;
            this.closeOut = closeOut;
        }
    }

    private Cd15ScheduleCandidate selectCandidate(List<Cd15ScheduleCandidate> candidates,
                                                  Deque<Cd15ScheduleCandidate> immediateContinueCandidates,
                                                  Deque<Cd15ScheduleCandidate> shiftStartTailCandidates,
                                                  Cd15ShiftResourceState state,
                                                  boolean preservePreparedOrder) {
        if (immediateContinueCandidates != null && !immediateContinueCandidates.isEmpty()) {
            return immediateContinueCandidates.removeFirst();
        }
        if (preservePreparedOrder) {
            return candidates.remove(0);
        }
        if (shiftStartTailCandidates != null && !shiftStartTailCandidates.isEmpty()) {
            return shiftStartTailCandidates.removeFirst();
        }
        List<Cd15ScheduleCandidate> sorted = candidateSorter.sort(candidates, this.continuityTails(state));
        candidates.clear();
        candidates.addAll(sorted);
        return candidates.remove(0);
    }

    private Deque<Cd15ScheduleCandidate> buildShiftStartTailCandidates(
            List<Cd15ScheduleCandidate> candidates, Cd15ShiftResourceState state) {
        Deque<Cd15ScheduleCandidate> result = new ArrayDeque<>();
        if (state == null || state.getTailByMachine() == null
                || state.getTailByMachine().isEmpty() || candidates == null || candidates.isEmpty()) {
            return result;
        }
        Map<String, Cd15ScheduleCandidate> candidateBySteelStrip = candidates.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode()))
                .collect(Collectors.toMap(Cd15ScheduleCandidate::getSteelStripCode,
                        item -> item, (first, second) -> first, LinkedHashMap::new));
        state.getTailByMachine().entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> candidateBySteelStrip.get(entry.getValue().getSteelStripCode()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(candidate -> {
                    candidates.remove(candidate);
                    result.addLast(candidate);
                });
        return result;
    }

    private BigDecimal resolveDemandQuantity(Cd15AutoScheduleContext context,
                                             Cd15AutoScheduleInput input,
                                             Cd15ShiftDescriptor shift,
                                             Cd15ScheduleCandidate candidate,
                                             Cd15RollingScheduleContext rolling,
                                             Map<String, BigDecimal> continueDemandBySteelStrip) {
        if (candidate.getRollingRequestedQuantity() != null) {
            return candidate.getRollingRequestedQuantity();
        }
        BigDecimal continueDemand = continueDemandBySteelStrip.get(this.candidateKey(candidate));
        if (continueDemand != null && continueDemand.signum() > 0) {
            log.info("[斜裁自动排程] 使用续作需求进入本班试算, classField={}, shiftCode={}, steelStripCode={}, continueDemand={}",
                    shift.getClassField(), shift.getShiftCode(), candidate.getSteelStripCode(), continueDemand);
            return continueDemand;
        }
        Cd15ShiftDemandDecision demand = demandProvider.resolve(context, input, shift, candidate, rolling);
        return demand == null || demand.getNetDemandQuantity() == null
                ? BigDecimal.ZERO : demand.getNetDemandQuantity();
    }

    private BigDecimal roundUpByUnitLength(Cd15ShiftDescriptor shift,
                                           String steelStripCode,
                                           BigDecimal rawDemand,
                                           BigDecimal unitConsumeMillimeter,
                                           BigDecimal craftWidth) {
        if (rawDemand == null || rawDemand.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (unitConsumeMillimeter == null || unitConsumeMillimeter.signum() <= 0) {
            log.warn("[斜裁自动排程] 单片斜裁长度缺失，无法按胎体片数取整，沿用原始净需求, classField={}, steelStripCode={}, rawDemand={}",
                    shift.getClassField(), steelStripCode, rawDemand);
            return this.normalize(rawDemand);
        }
        if (craftWidth == null || craftWidth.signum() <= 0) {
            log.warn("[斜裁自动排程] 单片斜裁宽度缺失，无法按胎体片数换算需求，沿用原始净需求, classField={}, steelStripCode={}, rawDemand={}",
                    shift.getClassField(), steelStripCode, rawDemand);
            return this.normalize(rawDemand);
        }
        // rawDemand 是按胎体长度累计的需求米数；先按单片长度算片数，再用单片斜裁宽换算为斜裁排程净需求。
        BigDecimal unitLengthInMeters = unitConsumeMillimeter.divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        BigDecimal craftWidthInMeters = craftWidth.divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        BigDecimal pieceCount = rawDemand.divide(unitLengthInMeters, 0, RoundingMode.CEILING);
        BigDecimal roundedDemand = this.normalize(pieceCount.multiply(craftWidthInMeters));
        log.info("[斜裁自动排程] 净需求按单片长度取整并按斜裁宽度换算, classField={}, steelStripCode={}, rawDemand={}, unitLength={}mm, craftWidth={}mm, pieceCount={}, roundedDemand={}",
                shift.getClassField(), steelStripCode, rawDemand, unitConsumeMillimeter, craftWidth, pieceCount, roundedDemand);
        return roundedDemand;
    }

    /**
     * 保存部分排后的剩余需求。均分只是计划策略切分，仍保持新增规格提前属性；
     * 机台、库排等真实资源限制产生的剩余量，才转为后续班次续作。
     */
    private void handleRemainingDemand(Cd15ShiftDescriptor shift,
                                       Cd15ScheduleCandidate candidate,
                                       BigDecimal netDemand,
                                       BigDecimal scheduledQuantity,
                                       String sourceMachineCode,
                                       String partialReason,
                                       Cd15RollingScheduleContext rolling,
                                       Map<String, BigDecimal> continueDemandBySteelStrip,
                                       Deque<Cd15ScheduleCandidate> immediateContinueCandidates) {
        if (!candidate.isNewSpecAdvance()) {
            this.handleContinueDemand(shift, candidate, netDemand, scheduledQuantity,
                    sourceMachineCode, partialReason, rolling, continueDemandBySteelStrip,
                    immediateContinueCandidates);
            return;
        }
        BigDecimal remainingDemand = this.remainingDemand(netDemand, scheduledQuantity);
        Map<String, BigDecimal> advanceRemainingBySteelStrip = rolling == null
                ? null : rolling.getNewSpecAdvanceRemainingBySteelStrip();
        Set<String> normalizedAdvanceSteelStripCodes = rolling == null
                ? null : rolling.getNormalizedNewSpecAdvanceSteelStripCodes();
        if (advanceRemainingBySteelStrip == null || normalizedAdvanceSteelStripCodes == null) {
            log.warn("[斜裁自动排程] 新增规格提前需求滚动快照缺失, classField={}, steelStripCode={}",
                    shift.getClassField(), candidate.getSteelStripCode());
            return;
        }
        if (remainingDemand.signum() <= 0) {
            advanceRemainingBySteelStrip.remove(candidate.getSteelStripCode());
            normalizedAdvanceSteelStripCodes.remove(candidate.getSteelStripCode());
            continueDemandBySteelStrip.remove(this.candidateKey(candidate));
            return;
        }
        if ("EQUAL_SHARE".equals(partialReason)) {
            advanceRemainingBySteelStrip.put(candidate.getSteelStripCode(), remainingDemand);
            normalizedAdvanceSteelStripCodes.add(candidate.getSteelStripCode());
            continueDemandBySteelStrip.remove(this.candidateKey(candidate));
            log.info("[斜裁自动排程] 新增规格均分后保留提前需求属性, classField={}, steelStripCode={}, "
                            + "scheduledQuantity={}, remainingDemand={}",
                    shift.getClassField(), candidate.getSteelStripCode(), scheduledQuantity, remainingDemand);
            return;
        }
        advanceRemainingBySteelStrip.remove(candidate.getSteelStripCode());
        normalizedAdvanceSteelStripCodes.remove(candidate.getSteelStripCode());
        this.handleContinueDemand(shift, candidate, netDemand, scheduledQuantity,
                sourceMachineCode, partialReason, rolling, continueDemandBySteelStrip,
                immediateContinueCandidates);
        log.info("[斜裁自动排程] 新增规格因资源限制转为续作, classField={}, steelStripCode={}, "
                        + "scheduledQuantity={}, remainingDemand={}, partialReason={}",
                shift.getClassField(), candidate.getSteelStripCode(), scheduledQuantity,
                remainingDemand, partialReason);
    }

    private void handleContinueDemand(Cd15ShiftDescriptor shift,
                                      Cd15ScheduleCandidate candidate,
                                      BigDecimal netDemand,
                                      BigDecimal scheduledQuantity,
                                      String sourceMachineCode,
                                      String partialReason,
                                      Cd15RollingScheduleContext rolling,
                                      Map<String, BigDecimal> continueDemandBySteelStrip,
                                      Deque<Cd15ScheduleCandidate> immediateContinueCandidates) {
        BigDecimal remainingDemand = this.remainingDemand(netDemand, scheduledQuantity);
        if (remainingDemand.signum() <= 0 || "EQUAL_SHARE".equals(partialReason)) {
            continueDemandBySteelStrip.remove(this.candidateKey(candidate));
            this.removePendingTask(rolling, this.candidateKey(candidate));
            return;
        }
        continueDemandBySteelStrip.put(this.candidateKey(candidate), remainingDemand);
        this.savePendingTask(rolling, shift, candidate, netDemand, scheduledQuantity,
                remainingDemand, sourceMachineCode, partialReason);
        candidate.setContinueFromPreviousShift(true);
        if (!StringUtils.hasText(candidate.getSourceMachineCode())) {
            candidate.setSourceMachineCode(sourceMachineCode);
        }
        // 机台产能不足时，本班不再把剩余量切到其他机台，留到后续班次优先回原机台续作。
        if (!"CAPACITY_LIMIT".equals(partialReason)) {
            immediateContinueCandidates.addFirst(candidate);
        }
        log.info("[斜裁自动排程] 当前规格部分排后进入续作队列, classField={}, steelStripCode={}, sourceMachineCode={}, scheduledQuantity={}, remainingDemand={}, partialReason={}",
                shift.getClassField(), candidate.getSteelStripCode(), candidate.getSourceMachineCode(), scheduledQuantity, remainingDemand, partialReason);
    }

    /** 自动排程同时保存任务级续作节点，供插单和后续滚动共享任务身份模型。 */
    private void savePendingTask(Cd15RollingScheduleContext rolling,
                                 Cd15ShiftDescriptor shift,
                                 Cd15ScheduleCandidate candidate,
                                 BigDecimal originalQuantity,
                                 BigDecimal scheduledQuantity,
                                 BigDecimal remainingQuantity,
                                 String sourceMachineCode,
                                 String partialReason) {
        if (rolling == null || rolling.getPendingTasks() == null) {
            return;
        }
        String materialKey = this.candidateKey(candidate);
        this.removePendingTask(rolling, materialKey);
        rolling.getPendingTasks().add(Cd15RollingPendingTask.builder()
                .taskKey("AUTO|" + shift.getClassField() + "|" + materialKey)
                .originalClassField(shift.getClassField())
                .targetClassField(nextClassField(shift.getClassField()))
                .materialKey(candidate.getMaterialKey())
                .steelStripCode(candidate.getSteelStripCode())
                .bigRollCode(candidate.getBigRollCode())
                .cuttingAngle(candidate.getCuttingAngle())
                .craftWidth(candidate.getCraftWidth())
                .unitConsumeMillimeter(candidate.getUnitConsumeMillimeter())
                .cordWidth(candidate.getCordWidth())
                .curlLength(candidate.getCurlLength())
                .cutMode(candidate.getCutMode())
                .splitGroupKey(candidate.getSplitGroupKey())
                .sourceMachineCode(sourceMachineCode)
                .requiredMachineCode(sourceMachineCode)
                .originalQuantity(originalQuantity)
                .scheduledQuantity(scheduledQuantity)
                .remainingQuantity(remainingQuantity)
                .hardInsert(false).locked(false).continueFromPreviousShift(true)
                .lastLimitReason(partialReason).build());
    }

    private String nextClassField(String classField) {
        if (!StringUtils.hasText(classField) || !classField.startsWith("CLASS")) {
            return null;
        }
        int classIndex = Integer.parseInt(classField.substring("CLASS".length()));
        return classIndex >= 8 ? null : "CLASS" + (classIndex + 1);
    }

    private void removePendingTask(Cd15RollingScheduleContext rolling, String materialKey) {
        if (rolling == null || rolling.getPendingTasks() == null) {
            return;
        }
        rolling.getPendingTasks().removeIf(item -> materialKey.equals(item.getMaterialKey()));
    }

    private BigDecimal remainingDemand(BigDecimal netDemand, BigDecimal scheduledQuantity) {
        BigDecimal safeNetDemand = netDemand == null ? BigDecimal.ZERO : netDemand;
        BigDecimal safeScheduledQuantity = scheduledQuantity == null ? BigDecimal.ZERO : scheduledQuantity;
        BigDecimal remaining = safeNetDemand.subtract(safeScheduledQuantity);
        return remaining.signum() <= 0 ? BigDecimal.ZERO : this.normalize(remaining);
    }

    private Map<String, BigDecimal> copyContinueDemand(Cd15RollingScheduleContext rolling) {
        if (rolling == null || rolling.getContinueDemandBySteelStrip() == null) {
            return new LinkedHashMap<>();
        }
        return rolling.getContinueDemandBySteelStrip().entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> second, LinkedHashMap::new));
    }
    private Map<String, String> copyLastMachineBySteelStrip(Cd15RollingScheduleContext rolling) {
        if (rolling == null || rolling.getLastMachineBySteelStrip() == null) {
            return new LinkedHashMap<>();
        }
        return rolling.getLastMachineBySteelStrip().entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> second, LinkedHashMap::new));
    }

    private boolean isContinueDemand(Cd15ScheduleCandidate candidate,
                                     Map<String, BigDecimal> continueDemandBySteelStrip) {
        BigDecimal continueDemand = candidate == null || continueDemandBySteelStrip == null
                ? null : continueDemandBySteelStrip.get(this.candidateKey(candidate));
        return continueDemand != null && continueDemand.signum() > 0;
    }

    private void attachSourceMachine(Cd15ScheduleCandidate candidate,
                                     Map<String, String> lastMachineBySteelStrip) {
        if (candidate == null || StringUtils.hasText(candidate.getSourceMachineCode())
                || lastMachineBySteelStrip == null) {
            return;
        }
        candidate.setSourceMachineCode(lastMachineBySteelStrip.get(this.candidateKey(candidate)));
    }

    private String preferredHistoryMachineCode(Cd15ScheduleCandidate candidate,
                                               Cd15RollingScheduleContext rolling) {
        if (candidate == null) {
            return null;
        }
        if (StringUtils.hasText(candidate.getSourceMachineCode())) {
            return candidate.getSourceMachineCode();
        }
        if (rolling == null || rolling.getLastMachineBySteelStrip() == null) {
            return null;
        }
        return rolling.getLastMachineBySteelStrip().get(this.candidateKey(candidate));
    }

    private void appendContinueCandidates(List<Cd15ScheduleCandidate> candidates,
                                          Map<String, BigDecimal> continueDemandBySteelStrip,
                                          Map<String, String> lastMachineBySteelStrip,
                                          List<Cd15ConstructionMaterial> materials) {
        if (continueDemandBySteelStrip.isEmpty()) {
            return;
        }
        Set<String> existingMaterialKeys = this.safe(candidates).stream()
                .map(this::candidateKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        continueDemandBySteelStrip.keySet().stream()
                .filter(materialKey -> !existingMaterialKeys.contains(materialKey))
                .map(materialKey -> this.findConstructionByMaterialKey(materials, materialKey))
                .filter(java.util.Objects::nonNull)
                .map(material -> this.candidateFromMaterial(material,
                        lastMachineBySteelStrip.get(this.materialKey(material))))
                .forEach(candidates::add);
    }

    private void saveContinueDemand(Cd15RollingScheduleContext rolling,
                                    Map<String, BigDecimal> continueDemandBySteelStrip) {
        if (rolling == null) {
            return;
        }
        Map<String, BigDecimal> validContinueDemand = continueDemandBySteelStrip.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> entry.getValue() != null && entry.getValue().signum() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> second, LinkedHashMap::new));
        rolling.setContinueDemandBySteelStrip(validContinueDemand);
        log.info("[斜裁自动排程] 当前班次续作需求已保存, continueDemandBySteelStrip={}", validContinueDemand);
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }

    private Cd15ScheduleAttemptTrace trace(Cd15ShiftDescriptor shift,
                                           Cd15ScheduleCandidate candidate,
                                           String bigRollCode,
                                           BigDecimal netDemand,
                                           BigDecimal scheduled,
                                           String failureReason,
                                           int sequence) {
        return trace(shift, candidate, bigRollCode, netDemand, scheduled,
                failureReason, null, sequence);
    }

    private Cd15ScheduleAttemptTrace trace(Cd15ShiftDescriptor shift,
                                           Cd15ScheduleCandidate candidate,
                                           String bigRollCode,
                                           BigDecimal netDemand,
                                           BigDecimal scheduled,
                                           String failureReason,
                                           String partialReason,
                                           int sequence) {
        return Cd15ScheduleAttemptTrace.builder()
                .classField(shift.getClassField())
                .shiftCode(shift.getShiftCode())
                .shiftDisplayName(shift.getShiftDisplayName())
                .steelStripCode(candidate.getSteelStripCode())
                .cuttingAngle(candidate.getCuttingAngle())
                .bigRollCode(bigRollCode)
                .netDemandQuantity(netDemand).scheduledQuantity(scheduled)
                .failureReason(failureReason).partialReason(partialReason)
                .sequence(sequence).build();
    }

    private Cd15MachineTrialRequest trialRequest(Cd15AutoScheduleContext context,
                                                  Cd15ShiftDescriptor shift,
                                                  Cd15ShiftResourceState state,
                                                  Cd15ConstructionMaterial construction,
                                                  BigDecimal netDemand,
                                                  boolean closeOut,
                                                  Cd15ScheduleCandidate candidate,
                                                  Cd15RollingScheduleContext rolling,
                                                  boolean splitCut) {
        return Cd15MachineTrialRequest.builder()
                .materialKey(candidate.getMaterialKey())
                .steelStripCode(construction.getSteelStripCode())
                .bigRollCode(construction.getBigRollCode())
                .cuttingAngle(construction.getCuttingAngle())
                .cordSpec(construction.getSteelStripCode())
                .splitCut(splitCut)
                .craftWidth(construction.getCraftWidth())
                .unitConsumeMillimeter(construction.getUnitConsumeMillimeter())
                .curlLength(effectiveCurlLength(context, construction))
                .cordWidth(construction.getCordWidth())

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


    private Cd15ShiftCommitRequest commitRequest(Cd15AutoScheduleContext context,
                                                  Cd15ShiftDescriptor shift,
                                                  Cd15ConstructionMaterial construction,
                                                  Cd15ScheduleCandidate candidate,
                                                  Cd15MachineTrialPlan trialPlan,
                                                  boolean closeOut,
                                                  String cutMode,
                                                  String splitGroupKey) {
        return Cd15ShiftCommitRequest.builder()
                .materialKey(candidate.getMaterialKey())
                .steelStripCode(construction.getSteelStripCode())
                .bigRollCode(construction.getBigRollCode())
                .cordSpec(construction.getSteelStripCode())
                .cuttingAngle(construction.getCuttingAngle())
                .craftWidth(construction.getCraftWidth())
                .unitConsumeMillimeter(construction.getUnitConsumeMillimeter())
                .cordWidth(construction.getCordWidth())
                .curlLength(effectiveCurlLength(context, construction))
                .cutMode(cutMode)
                .splitGroupKey(splitGroupKey)
                .classField(shift.getClassField())
                .shiftStart(shift.getStartTime()).shiftEnd(shift.getEndTime())
                .closeOut(closeOut)
                .partialMinVehicleCount(context.getParameters().getPartialMinVehicleCount())
                .trialPlan(trialPlan).build();
    }

    /**
     * 获取本规格实际采用的卷曲长度，单位米。
     * 优先使用t_cd15_curl_length维护的标准卷曲长度；没有维护时，才启用参数CRIMP_LENGTH作为兜底值。
     */
    private BigDecimal effectiveCurlLength(Cd15AutoScheduleContext context,
                                           Cd15ConstructionMaterial construction) {
        if (construction.getCurlLength() != null && construction.getCurlLength().signum() > 0) {
            return construction.getCurlLength();
        }
        BigDecimal fallback = context.getParameters().getRollCoilMeter();
        log.warn("[斜裁自动排程] 当前规格未匹配到标准卷曲长度，使用参数CRIMP_LENGTH兜底, steelStripCode={}, bigRollCode={}, fallbackMeter={}",
                construction.getSteelStripCode(), construction.getBigRollCode(), fallback);
        return fallback;
    }

    private Cd15ConstructionMaterial findConstruction(List<Cd15ConstructionMaterial> materials,
                                                       Cd15ScheduleCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        if (StringUtils.hasText(candidate.getMaterialKey())) {
            Cd15ConstructionMaterial exact = this.findConstructionByMaterialKey(
                    materials, candidate.getMaterialKey());
            if (exact != null) {
                return exact;
            }
        }
        return safe(materials).stream()
                .filter(item -> item != null
                        && candidate.getSteelStripCode().equals(item.getSteelStripCode()))
                .filter(item -> !StringUtils.hasText(candidate.getBigRollCode())
                        || candidate.getBigRollCode().equals(item.getBigRollCode()))
                .filter(item -> !StringUtils.hasText(candidate.getCuttingAngle())
                        || candidate.getCuttingAngle().equals(item.getCuttingAngle()))
                .findFirst().orElse(null);
    }

    private Cd15ConstructionMaterial findConstructionByMaterialKey(
            List<Cd15ConstructionMaterial> materials, String materialKey) {
        return safe(materials).stream()
                .filter(java.util.Objects::nonNull)
                .filter(item -> materialKey.equals(this.materialKey(item)))
                .findFirst().orElse(null);
    }

    /** 根据施工映射补充候选的完整材料身份，供续作和滚动任务稳定匹配。 */
    private void attachConstructionFields(List<Cd15ScheduleCandidate> candidates,
                                          List<Cd15ConstructionMaterial> materials) {
        for (Cd15ScheduleCandidate candidate : safe(candidates)) {
            Cd15ConstructionMaterial material = this.findConstruction(materials, candidate);
            if (material == null) {
                continue;
            }
            candidate.setMaterialKey(this.materialKey(material));
            candidate.setSteelStripCode(material.getSteelStripCode());
            candidate.setBigRollCode(material.getBigRollCode());
            candidate.setCuttingAngle(material.getCuttingAngle());
            candidate.setCraftWidth(material.getCraftWidth());
            candidate.setUnitConsumeMillimeter(material.getUnitConsumeMillimeter());
            candidate.setCordWidth(material.getCordWidth());
            candidate.setCurlLength(material.getCurlLength());
        }
    }

    private Cd15ScheduleCandidate candidateFromMaterial(
            Cd15ConstructionMaterial material, String sourceMachineCode) {
        return Cd15ScheduleCandidate.builder()
                .materialKey(this.materialKey(material))
                .steelStripCode(material.getSteelStripCode())
                .bigRollCode(material.getBigRollCode())
                .cuttingAngle(material.getCuttingAngle())
                .craftWidth(material.getCraftWidth())
                .unitConsumeMillimeter(material.getUnitConsumeMillimeter())
                .continueFromPreviousShift(true)
                .cordWidth(material.getCordWidth())
                .curlLength(material.getCurlLength())
                .sourceMachineCode(sourceMachineCode)
                .build();
    }

    private String candidateKey(Cd15ScheduleCandidate candidate) {
        return candidate == null ? null : StringUtils.hasText(candidate.getMaterialKey())
                ? candidate.getMaterialKey() : candidate.getSteelStripCode();
    }

    private String materialKey(Cd15ConstructionMaterial material) {
        return this.text(material.getSteelStripCode()) + "|"
                + this.text(material.getBigRollCode()) + "|"
                + this.text(material.getCuttingAngle()) + "|"
                + this.decimalText(material.getCraftWidth()) + "|"
                + this.decimalText(material.getUnitConsumeMillimeter()) + "|"
                + this.decimalText(material.getCurlLength());
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 当前班已有任务时只采用最后提交任务作为强连续参照；班次刚开始时使用全部跨班机台尾状态。
     */
    private List<Cd15MachineTailState> continuityTails(Cd15ShiftResourceState state) {
        if (state == null) {
            return Collections.emptyList();
        }
        if (state.getTasks() != null && !state.getTasks().isEmpty()) {
            com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask task =
                    state.getTasks().get(state.getTasks().size() - 1);
            return Collections.singletonList(Cd15MachineTailState.builder()
                    .materialKey(task.getMaterialKey())
                    .steelStripCode(task.getSteelStripCode())
                    .bigRollCode(task.getBigRollCode())
                    .cuttingAngle(task.getCuttingAngle())
                    .build());
        }
        if (state.getTailByMachine() == null || state.getTailByMachine().isEmpty()) {
            return Collections.emptyList();
        }
        return state.getTailByMachine().values().stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private int occupiedVehicles(List<Cd15StorageLaneState> lanes) {
        return safe(lanes).stream().mapToInt(Cd15StorageLaneState::getVehicleCount).sum();
    }

    /** 汇总当前斜裁规格关联胎胚的计划量，并与各自月计划剩余量配对。 */
    private List<Cd15EmbryoCloseOutItem> closeOutItems(String steelStripCode,
                                                       Cd15AutoScheduleInput input) {
        Set<String> embryoCodes = safe(input.getConstructionMaterials()).stream()
                .filter(item -> item != null && steelStripCode.equals(item.getSteelStripCode()))
                .map(Cd15ConstructionMaterial::getConstructionCode)
                .filter(StringUtils::hasText).collect(Collectors.toCollection(HashSet::new));
        Map<String, BigDecimal> planByEmbryo = safe(input.getFormingSchedules()).stream()
                .filter(item -> item != null && embryoCodes.contains(item.getEmbryoCode()))
                .collect(Collectors.groupingBy(Cd15FormingScheduleSource::getEmbryoCode,
                        Collectors.reducing(BigDecimal.ZERO, this::sumClassPlanQuantities,
                                BigDecimal::add)));
        Map<String, BigDecimal> surplusByEmbryo = safe(input.getEmbryoPlanSurpluses()).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getEmbryoCode()))
                .collect(Collectors.toMap(Cd15EmbryoPlanSurplus::getEmbryoCode,
                        Cd15EmbryoPlanSurplus::getPlanSurplusQuantity, (first, second) -> second));
        return embryoCodes.stream().sorted().map(embryoCode -> Cd15EmbryoCloseOutItem.builder()
                .embryoCode(embryoCode)
                .calculatedPlanQuantity(planByEmbryo.getOrDefault(embryoCode, BigDecimal.ZERO))
                .planSurplusQuantity(surplusByEmbryo.get(embryoCode)).build())
                .collect(Collectors.toList());
    }

    private BigDecimal sumClassPlanQuantities(Cd15FormingScheduleSource source) {
        return safe(source.getClassPlanQuantities()).stream().filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recordFailure(Map<String, String> failures, Cd15ShiftDescriptor shift,
                               Cd15ScheduleCandidate candidate, String reason) {
        String stableReason = StringUtils.hasText(reason) ? reason : "NO_AVAILABLE_MACHINE";
        failures.put(this.candidateKey(candidate), stableReason);
        log.warn("[斜裁自动排程] 当前班次规格排程失败, classField={}, steelStripCode={}, cuttingAngle={}, reason={}",
                shift.getClassField(), candidate.getSteelStripCode(),
                candidate.getCuttingAngle(), stableReason);
    }

    private void validate(Cd15AutoScheduleContext context, Cd15AutoScheduleInput input,
                          Cd15ShiftDescriptor shift, Cd15ShiftResourceState state) {
        if (context == null || context.getParameters() == null || input == null
                || shift == null || state == null) {
            throw new IllegalArgumentException("单班排程上下文、输入、班次和资源状态不能为空");
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
