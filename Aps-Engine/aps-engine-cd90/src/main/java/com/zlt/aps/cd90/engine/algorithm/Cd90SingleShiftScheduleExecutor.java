package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90CloseOutDecision;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
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
        List<Cd90ScheduleCandidate> candidates = candidatePreparationService.prepare(
                context, input, shift.getClassField());
        Cd90MachineResourceSnapshot machineSnapshot = machineResourceService.load(
                context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
        Cd90ShiftResourceState state = initialState;
        Map<String, String> failures = new LinkedHashMap<>();
        List<Cd90ScheduleAttemptTrace> attemptTraces = new ArrayList<>();

        log.info("[直裁自动排程] 当前班次执行开始, classField={}, shiftCode={}, candidateCount={}",
                shift.getClassField(), shift.getShiftCode(), candidates.size());
        for (Cd90ScheduleCandidate candidate : candidates) {
            String clothCode = candidate == null ? null : candidate.getClothCode();
            if (!StringUtils.hasText(clothCode)) {
                continue;
            }
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
                String reason = "CONSTRUCTION_MISSING";
                recordFailure(failures, shift, clothCode, reason);
                attemptTraces.add(trace(shift, clothCode, null, netDemand,
                        BigDecimal.ZERO, reason, attemptTraces.size() + 1));
                continue;
            }
            Cd90CloseOutDecision closeOut = closeOutCalculator.decide(
                    demand.getPlanSurplusQuantity(), netDemand);
            if (closeOut.isMissingPlanSurplusWarning()) {
                log.warn("[直裁自动排程] 月计划剩余量缺失, classField={}, clothCode={}",
                        shift.getClassField(), clothCode);
            }
            Cd90MachineTrialPlan trialPlan = trialPreparationService.prepare(
                    trialRequest(context, shift, state, construction, netDemand, closeOut.isCloseOut()),
                    machineSnapshot);
            Cd90ShiftCommitResult commit = resourceCommitter.commit(
                    commitRequest(context, shift, construction, trialPlan), state);
            if (!commit.isSuccess()) {
                recordFailure(failures, shift, clothCode, commit.getFailureReason());
                attemptTraces.add(trace(shift, clothCode, construction.getBigRollCode(),
                        netDemand, BigDecimal.ZERO, commit.getFailureReason(),
                        attemptTraces.size() + 1));
                continue;
            }
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
                .shiftCode(shift.getShiftCode())
                .shiftStart(shift.getStartTime()).shiftEnd(shift.getEndTime())
                .netDemandQuantity(netDemand).closeOut(closeOut)
                .occupiedVehicleCount(occupiedVehicles(state.getLanes()))
                .shiftHours(Math.max(1, shift.getDurationSeconds() / 3600))
                .remainingSecondsByMachine(state.getRemainingSecondsByMachine())
                .previousSpecByMachine(state.getTailSpecByMachine())
                .parameters(context.getParameters()).build();
    }

    private Cd90ShiftCommitRequest commitRequest(Cd90AutoScheduleContext context,
                                                  Cd90ShiftDescriptor shift,
                                                  Cd90ConstructionMaterial construction,
                                                  Cd90MachineTrialPlan trialPlan) {
        return Cd90ShiftCommitRequest.builder()
                .clothCode(construction.getClothCode())
                .bigRollCode(construction.getBigRollCode())
                .cordSpec(construction.getCordSpec())
                .classField(shift.getClassField())
                .shiftStart(shift.getStartTime()).shiftEnd(shift.getEndTime())
                .coilMeter(context.getParameters().getRollCoilMeter())
                .trialPlan(trialPlan).build();
    }

    private Cd90ConstructionMaterial findConstruction(List<Cd90ConstructionMaterial> materials,
                                                       String clothCode) {
        return safe(materials).stream()
                .filter(item -> item != null && clothCode.equals(item.getClothCode()))
                .filter(item -> StringUtils.hasText(item.getBigRollCode())
                        && StringUtils.hasText(item.getCordSpec()))
                .findFirst().orElse(null);
    }

    private int occupiedVehicles(List<Cd90StorageLaneState> lanes) {
        return safe(lanes).stream().mapToInt(Cd90StorageLaneState::getVehicleCount).sum();
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
