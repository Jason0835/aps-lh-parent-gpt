package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90CandidateMachineTrialInput;
import com.zlt.aps.cd90.engine.model.Cd90MachineCandidate;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialPlan;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialRequest;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import com.zlt.aps.cd90.engine.model.Cd90MachineCandidateResolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单规格候选机台试算编排器，只生成内存方案，不提交资源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90MachineTrialPreparationService {

    private final Cd90MachineCandidateResolver candidateResolver;
    private final Cd90CandidateMachineTrialCalculator trialCalculator;
    private final Cd90MachineTrialSelector trialSelector;

    public Cd90MachineTrialPlan prepare(Cd90MachineTrialRequest request,
                                        Cd90MachineResourceSnapshot snapshot) {
        if (request == null || request.getParameters() == null) {
            throw new IllegalArgumentException("机台试算请求及参数不能为空");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("机台资源快照不能为空");
        }
        Cd90AutoScheduleParameters parameters = request.getParameters();
        // 先执行启用状态、大卷绑定、指定/禁止机台、检修和班次开放等硬约束过滤。
        Cd90MachineCandidateResolution resolution = candidateResolver.resolveDetailed(
                request.getClothCode(), request.getBigRollCode(), request.getCraftWidth(),
                request.getShiftCode(), request.getShiftStart(), request.getShiftEnd(), snapshot.getMachines(),
                snapshot.getBindings(), snapshot.getRestrictions(), parameters.getMachinePriority());
        List<Cd90MachineCandidate> candidates = resolution.getCandidates();
        Map<String, Integer> seconds = request.getRemainingSecondsByMachine() == null
                ? Collections.emptyMap() : request.getRemainingSecondsByMachine();
        Map<String, String> previousSpecs = request.getPreviousSpecByMachine() == null
                ? Collections.emptyMap() : request.getPreviousSpecByMachine();
        Map<String, Cd90MachineTailState> previousTails = request.getPreviousTailByMachine() == null
                ? Collections.emptyMap() : request.getPreviousTailByMachine();
        Cd90MachineTailState currentTail = Cd90MachineTailState.builder()
                .clothCode(request.getClothCode()).bigRollCode(request.getBigRollCode()).build();

        // 每台候选机独立试算需求补量、损耗、工装、班产和规格切换耗时，不修改资源快照。
        List<Cd90MachineTrial> trials = candidates.stream()
                .map(candidate -> trialCalculator.calculate(Cd90CandidateMachineTrialInput.builder()
                        .clothCode(request.getClothCode())
                        .machineCode(candidate.getMachineCode())
                        .netDemandQuantity(request.getNetDemandQuantity())
                        .closeOut(request.isCloseOut())
                        .minimumStartQuantity(parameters.getMinStartQty())
                        .coilMeter(request.getCurlLength())
                        .totalToolingCount(parameters.getRollTotalCount())
                        .occupiedVehicleCount(request.getOccupiedVehicleCount())
                        .quota(candidate.getQuota())
                        .shiftHours(request.getShiftHours())
                        .remainingSeconds(seconds.getOrDefault(candidate.getMachineCode(),
                                request.getShiftHours() * 3600))
                        .previousSpec(previousSpecs.get(candidate.getMachineCode()))
                        .currentSpec(request.getCordSpec())
                        .specChangeMinutes(parameters.getSpecChangeMinutes())
                        .previousTail(previousTails.get(candidate.getMachineCode()))
                        .currentTail(currentTail)
                        .sameRollDiffSpecChangeMinutes(changeMinutes(parameters,
                                parameters.getSameRollDiffSpecChangeMinutes()))
                        .diffRollSameSpecChangeMinutes(changeMinutes(parameters,
                                parameters.getDiffRollSameSpecChangeMinutes()))
                        .diffRollDiffSpecChangeMinutes(changeMinutes(parameters,
                                parameters.getDiffRollDiffSpecChangeMinutes()))
                        .lossRateRules(snapshot.getLossRateRules())
                        .preferredMachine(candidate.isPreferredMachine())
                        .priorityOrder(candidate.getPriorityOrder())
                        .build()))
                .collect(Collectors.toList());
        // 选择器只给出当前最优方案；提交阶段仍会在库排失败时继续尝试其余方案。
        Cd90MachineTrial selected = trialSelector.select(trials);
        log.info("[直裁自动排程] 规格机台试算准备完成, clothCode={}, bigRollCode={}, "
                        + "candidateCount={}, selectedMachine={}",
                request.getClothCode(), request.getBigRollCode(), trials.size(),
                selected == null ? null : selected.getMachineCode());
        if ("MACHINE_PROHIBITED".equals(resolution.getFailureReason())) {
            log.warn("[直裁自动排程] 大卷绑定机台均不可作业, clothCode={}, bigRollCode={}, machines={}",
                    request.getClothCode(), request.getBigRollCode(), resolution.getBoundMachineCodes());
        }
        return Cd90MachineTrialPlan.builder().trials(trials).selectedTrial(selected)
                .failureReason(resolution.getFailureReason())
                .boundMachineCodes(resolution.getBoundMachineCodes()).build();
    }

    /** 兼容未补充三类参数的旧测试构造器，正式参数快照始终使用新字段。 */
    private int changeMinutes(Cd90AutoScheduleParameters parameters, int configuredMinutes) {
        return configuredMinutes == 0 && parameters.getSourceValues() == null
                ? parameters.getSpecChangeMinutes() : configuredMinutes;
    }
}
