package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15CandidateMachineTrialInput;
import com.zlt.aps.cd15.engine.model.Cd15MachineCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialPlan;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialRequest;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15MachineCandidateResolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
public class Cd15MachineTrialPreparationService {

    private final Cd15MachineCandidateResolver candidateResolver;
    private final Cd15CandidateMachineTrialCalculator trialCalculator;
    private final Cd15MachineTrialSelector trialSelector;
    private final Cd15VehiclePlanQuantityCalculator vehiclePlanQuantityCalculator;
    private final Cd15MachineModeResolver machineModeResolver;

    public Cd15MachineTrialPlan prepare(Cd15MachineTrialRequest request,
                                        Cd15MachineResourceSnapshot snapshot) {
        if (request == null || request.getParameters() == null) {
            throw new IllegalArgumentException("机台试算请求及参数不能为空");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("机台资源快照不能为空");
        }
        Cd15AutoScheduleParameters parameters = request.getParameters();
        BigDecimal vehiclePlanQuantity = vehiclePlanQuantityCalculator.calculate(
                request.getUnitConsumeMillimeter(), request.getCraftWidth(),
                request.getCurlLength());
        // 先执行启用状态、大卷绑定、指定/禁止机台、检修和班次开放等硬约束过滤。
        BigDecimal machineMatchWidth = request.getMachineMatchWidth() == null
                ? request.getCraftWidth() : request.getMachineMatchWidth();
        Cd15MachineCandidateResolution resolution = candidateResolver.resolveDetailed(
                request.getSteelStripCode(), request.getBigRollCode(), machineMatchWidth,
                request.getCuttingAngle(), snapshot.getAngleWidthMaxByAngle(),
                request.getShiftCode(), request.getShiftStart(), request.getShiftEnd(), snapshot.getMachines(),
                snapshot.getBindings(), snapshot.getRestrictions(), parameters.getMachinePriority());
        List<Cd15MachineCandidate> rawCandidates = resolution.getCandidates() == null
                ? Collections.emptyList() : resolution.getCandidates();
        List<Cd15MachineCandidate> candidates = rawCandidates.stream()
                .filter(candidate -> this.machineModeMatched(
                        request, candidate.getMachineCode(), snapshot))
                .collect(Collectors.toList());
        String failureReason = resolution.getFailureReason();
        if (failureReason == null && !rawCandidates.isEmpty() && candidates.isEmpty()) {
            failureReason = "NO_AVAILABLE_MACHINE";
        }
        Map<String, Integer> seconds = request.getRemainingSecondsByMachine() == null
                ? Collections.emptyMap() : request.getRemainingSecondsByMachine();
        Map<String, String> previousSpecs = request.getPreviousSpecByMachine() == null
                ? Collections.emptyMap() : request.getPreviousSpecByMachine();
        Map<String, Cd15MachineTailState> previousTails = request.getPreviousTailByMachine() == null
                ? Collections.emptyMap() : request.getPreviousTailByMachine();
        Cd15MachineTailState currentTail = Cd15MachineTailState.builder()
                .materialKey(request.getMaterialKey())
                .steelStripCode(request.getSteelStripCode())
                .bigRollCode(request.getBigRollCode())
                .cuttingAngle(request.getCuttingAngle())
                .build();

        // 每台候选机独立试算需求补量、损耗、工装、班产和规格切换耗时，不修改资源快照。
        List<Cd15MachineTrial> trials = candidates.stream()
                .map(candidate -> trialCalculator.calculate(Cd15CandidateMachineTrialInput.builder()
                        // 钢带与机台标识
                        .bigRollCode(request.getBigRollCode())
                        .steelStripCode(request.getSteelStripCode())
                        .machineCode(candidate.getMachineCode())
                        // 本次排程净需求量（已扣除已排量）
                        .netDemandQuantity(request.getNetDemandQuantity())
                        // 是否为清尾：清尾时起排量门槛降低、允许跨机台合并
                        .closeOut(request.isCloseOut())
                        .singleSpecSplit(request.isSingleSpecSplit())
                        // 最小起排量、均分阈值
                        .minimumStartQuantity(parameters.getMinStartQty())
                        .equalShareThreshold(parameters.getEqualShareThreshold())
                        // 单车等价排程量，用于整车取整及工装数量试算
                        .vehiclePlanQuantity(vehiclePlanQuantity)
                        .craftWidth(request.getCraftWidth())
                        .unitConsumeMillimeter(request.getUnitConsumeMillimeter())
                        .cordWidth(request.getCordWidth())
                        // 工装总数（卷轴），决定单机同时可上多少卷
                        .totalToolingCount(parameters.getRollTotalCount())
                        // 已占用车数（前序班次已安排入库的部分）
                        .occupiedVehicleCount(request.getOccupiedVehicleCount())
                        // 当前裁断模式的满班产能
                        .shiftCapacity(this.shiftCapacity(
                                request, candidate.getMachineCode(), snapshot))
                        // 班次可用小时数
                        .shiftHours(request.getShiftHours())
                        // 机台班次剩余秒数（扣除前序任务后）
                        .remainingSeconds(this.remainingSeconds(
                                request, candidate.getMachineCode(), snapshot, seconds))
                        .originalStartTime(this.originalStartTime(
                                request, candidate.getMachineCode(), snapshot, seconds))
                        .bigRollAgingStocks(request.getBigRollAgingStocks())
                        // 机台上次规格（用于计算换规格耗时）
                        .previousSpec(previousSpecs.get(candidate.getMachineCode()))
                        .currentSpec(request.getCordSpec())
                        .specChangeMinutes(parameters.getSpecChangeMinutes())
                        // 机台上次尾匹（用于计算换尾耗时）
                        .previousTail(previousTails.get(candidate.getMachineCode()))
                        .currentTail(currentTail)
                        // 三种规格切换场景耗时（同卷异规格 / 异卷同规格 / 异卷异规格）
                        .sameRollDiffSpecChangeMinutes(changeMinutes(parameters,
                                parameters.getSameRollDiffSpecChangeMinutes()))
                        .diffRollSameSpecChangeMinutes(changeMinutes(parameters,
                                parameters.getDiffRollSameSpecChangeMinutes()))
                        .diffRollDiffSpecChangeMinutes(changeMinutes(parameters,
                                parameters.getDiffRollDiffSpecChangeMinutes()))
                        // 损耗率规则集（按规格匹配）
                        .lossRateRules(snapshot.getLossRateRules())
                        // 通用损耗率兜底（百分比），四层优先级均未命中时使用
                        .fallbackLossRatePercent(parameters.getFallbackLossRatePercent())
                        // 首选机台标志与优先级顺序
                        .preferredMachine(candidate.isPreferredMachine())
                        .historyMachine(candidate.getMachineCode() != null
                                && candidate.getMachineCode().equals(request.getPreferredHistoryMachineCode()))
                        .priorityOrder(candidate.getPriorityOrder())
                        .build()))
                .collect(Collectors.toList());
        // 选择器只给出当前最优方案；提交阶段仍会在库排失败时继续尝试其余方案。
        Cd15MachineTrial selected = trialSelector.select(trials);
        log.info("[斜裁自动排程] 规格机台试算准备完成, steelStripCode={}, bigRollCode={}, "
                        + "candidateCount={}, selectedMachine={}, preferredHistoryMachine={}",
                request.getSteelStripCode(), request.getBigRollCode(), trials.size(),
                selected == null ? null : selected.getMachineCode(), request.getPreferredHistoryMachineCode());
        if ("MACHINE_PROHIBITED".equals(resolution.getFailureReason())) {
            log.warn("[斜裁自动排程] 大卷绑定机台均不可作业, steelStripCode={}, bigRollCode={}, machines={}",
                    request.getSteelStripCode(), request.getBigRollCode(), resolution.getBoundMachineCodes());
        }
        return Cd15MachineTrialPlan.builder().trials(trials).selectedTrial(selected)
                .failureReason(failureReason)
                .boundMachineCodes(resolution.getBoundMachineCodes()).build();
    }

    /**
     * 机台模式完全由机台主数据决定；指定机台约束由候选机台解析器统一处理。
     */
    private boolean machineModeMatched(
            Cd15MachineTrialRequest request,
            String machineCode,
            Cd15MachineResourceSnapshot snapshot) {
        com.zlt.aps.cd15.engine.model.Cd15MachineResource machine =
                this.findMachine(machineCode, snapshot);
        if (machine == null) {
            return false;
        }
        return this.machineModeResolver.matches(machine, request.isSplitCut());
    }

    /** 按本次裁断模式选择班产能力。 */
    private BigDecimal shiftCapacity(
            Cd15MachineTrialRequest request,
            String machineCode,
            Cd15MachineResourceSnapshot snapshot) {
        com.zlt.aps.cd15.engine.model.Cd15MachineResource machine =
                this.findMachine(machineCode, snapshot);
        if (machine == null) {
            return BigDecimal.ZERO;
        }
        return this.machineModeResolver.capacity(machine, request.isSplitCut());
    }

    /** 检修只扣除与当前班次重叠的秒数，不再整班排除机台。 */
    private int remainingSeconds(
            Cd15MachineTrialRequest request,
            String machineCode,
            Cd15MachineResourceSnapshot snapshot,
            Map<String, Integer> configuredSeconds) {
        int fullSeconds = this.fullShiftSeconds(request);
        com.zlt.aps.cd15.engine.model.Cd15MachineResource machine =
                this.findMachine(machineCode, snapshot);
        int maintenanceSeconds = machine == null ? 0 : machine.getMaintenanceSeconds();
        int availableSeconds = Math.max(0, fullSeconds - maintenanceSeconds);
        int currentRemaining = configuredSeconds == null
                ? fullSeconds : configuredSeconds.getOrDefault(machineCode, fullSeconds);
        return Math.min(currentRemaining, availableSeconds);
    }

    private java.time.LocalDateTime originalStartTime(
            Cd15MachineTrialRequest request,
            String machineCode,
            Cd15MachineResourceSnapshot snapshot,
            Map<String, Integer> configuredSeconds) {
        if (request.getShiftStart() == null) {
            return null;
        }
        int fullSeconds = this.fullShiftSeconds(request);
        int remainingSeconds = this.remainingSeconds(
                request, machineCode, snapshot, configuredSeconds);
        return request.getShiftStart().plusSeconds(
                Math.max(0, fullSeconds - remainingSeconds));
    }

    private int fullShiftSeconds(Cd15MachineTrialRequest request) {
        if (request.getShiftStart() != null && request.getShiftEnd() != null) {
            return Math.toIntExact(java.time.Duration.between(
                    request.getShiftStart(), request.getShiftEnd()).getSeconds());
        }
        return Math.max(1, request.getShiftHours()) * 3600;
    }

    private com.zlt.aps.cd15.engine.model.Cd15MachineResource findMachine(
            String machineCode, Cd15MachineResourceSnapshot snapshot) {
        return snapshot == null || snapshot.getMachines() == null
                ? null : snapshot.getMachines().stream()
                        .filter(item -> item != null
                                && java.util.Objects.equals(machineCode, item.getMachineCode()))
                        .findFirst().orElse(null);
    }
    private int changeMinutes(Cd15AutoScheduleParameters parameters, int configuredMinutes) {
        return configuredMinutes == 0 && parameters.getSourceValues() == null
                ? parameters.getSpecChangeMinutes() : configuredMinutes;
    }
}
