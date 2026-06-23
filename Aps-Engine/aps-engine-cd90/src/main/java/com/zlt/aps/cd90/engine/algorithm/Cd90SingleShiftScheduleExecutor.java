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
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        // 候选已按当前班缺料时间和库存保障时长排序，后续必须保持该稳定顺序执行。
        List<Cd90ScheduleCandidate> candidates = new ArrayList<>(candidatePreparationService.prepare(
                context, input, shift.getClassField()));
        attachBigRollCodes(candidates, input.getConstructionMaterials());
        // 机台快照在班次开始时一次加载，规格之间通过state扣减剩余秒数，避免重复查询造成漂移。
        Cd90MachineResourceSnapshot machineSnapshot = machineResourceService.load(
                context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
        Cd90ShiftResourceState state = initialState;
        Map<String, String> failures = new LinkedHashMap<>();
        List<Cd90ScheduleAttemptTrace> attemptTraces = new ArrayList<>();

        log.info("[直裁自动排程] 当前班次执行开始, classField={}, shiftCode={}, candidateCount={}",
                shift.getClassField(), shift.getShiftCode(), candidates.size());
        while (!candidates.isEmpty()) {
            // 每提交一个任务后按最新链尾重新排序，使同规格优先规则能够连续生效。
            Cd90MachineTailState latestTail = latestTail(state);
            candidates = candidateSorter.sort(candidates, latestTail);
            Cd90ScheduleCandidate candidate = candidates.remove(0);
            String clothCode = candidate == null ? null : candidate.getClothCode();
            if (!StringUtils.hasText(clothCode)) {
                continue;
            }
            // 净需求会扣除库存和前序计划入库；没有正需求的规格不进入资源试算。
            Cd90ShiftDemandDecision demand = demandProvider.resolve(
                    context, input, shift, candidate, rolling);
            BigDecimal netDemand = demand == null || demand.getNetDemandQuantity() == null
                    ? BigDecimal.ZERO : demand.getNetDemandQuantity();
            if (netDemand.signum() <= 0) {
                continue;
            }
            Cd90ConstructionMaterial construction = findConstruction(
                    input.getConstructionMaterials(), clothCode);
            if (construction == null) {
                // 数据缺失只终止当前规格，保留轨迹后继续处理后续候选。
                String reason = "CONSTRUCTION_MISSING";
                recordFailure(failures, shift, clothCode, reason);
                attemptTraces.add(trace(shift, clothCode, null, netDemand,
                        BigDecimal.ZERO, reason, attemptTraces.size() + 1));
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
                    trialRequest(context, shift, state, construction, netDemand, closeOut.isCloseOut()),
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
            attemptTraces.add(trace(shift, clothCode, construction.getBigRollCode(),
                    netDemand, commit.getTask().getPlanQuantity(), null,
                    attemptTraces.size() + 1));
        }
        log.info("[直裁自动排程] 当前班次执行完成, classField={}, taskCount={}, failureCount={}",
                shift.getClassField(), state.getTasks().size(), failures.size());
        return Cd90ShiftExecutionResult.builder().shift(shift).state(state)
                .tasks(state.getTasks()).failures(failures)
                .attemptTraces(attemptTraces).build();
    }

    private Cd90ScheduleAttemptTrace trace(Cd90ShiftDescriptor shift,
                                           String clothCode,
                                           String bigRollCode,
                                           BigDecimal netDemand,
                                           BigDecimal scheduled,
                                           String failureReason,
                                           int sequence) {
        return Cd90ScheduleAttemptTrace.builder()
                .classField(shift.getClassField()).shiftCode(shift.getShiftCode())
                .clothCode(clothCode).bigRollCode(bigRollCode)
                .netDemandQuantity(netDemand).scheduledQuantity(scheduled)
                .failureReason(failureReason).sequence(sequence).build();
    }

    private Cd90MachineTrialRequest trialRequest(Cd90AutoScheduleContext context,
                                                  Cd90ShiftDescriptor shift,
                                                  Cd90ShiftResourceState state,
                                                  Cd90ConstructionMaterial construction,
                                                  BigDecimal netDemand,
                                                  boolean closeOut) {
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

    /** 当前班最后提交任务作为下一候选的连续生产参照。 */
    private Cd90MachineTailState latestTail(Cd90ShiftResourceState state) {
        if (state == null || state.getTasks() == null || state.getTasks().isEmpty()) {
            return null;
        }
        com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask task =
                state.getTasks().get(state.getTasks().size() - 1);
        return Cd90MachineTailState.builder().clothCode(task.getClothCode())
                .bigRollCode(task.getBigRollCode()).build();
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
