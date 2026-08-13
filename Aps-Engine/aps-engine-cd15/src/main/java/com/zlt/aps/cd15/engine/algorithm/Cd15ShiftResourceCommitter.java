package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitRequest;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocationResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 当前班次机台、库排、工装和任务链的原子内存提交器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15ShiftResourceCommitter {

    private final Cd15StorageLaneAllocator laneAllocator;
    private final Cd15MachineTrialSelector trialSelector;
    private final Cd15BigRollAgingAllocator agingAllocator;
    private final Cd15BigRollMeterCalculator bigRollMeterCalculator;

    /**
     * 按试算优先级逐方案尝试，并原子提交当前班次内存资源。
     *
     * @param request 单规格提交请求
     * @param originalState 原资源状态
     * @return 成功后的新状态和任务，或失败原因及未修改原状态
     */
    public Cd15ShiftCommitResult commit(Cd15ShiftCommitRequest request,
                                        Cd15ShiftResourceState originalState) {
        if (request == null || request.getTrialPlan() == null || originalState == null) {
            throw new IllegalArgumentException("班次提交请求、试算方案和资源状态不能为空");
        }
        List<Cd15MachineTrial> remainingTrials = request.getTrialPlan().getTrials() == null
                ? new ArrayList<>() : new ArrayList<>(request.getTrialPlan().getTrials());
        String lastFailureReason = request.getTrialPlan().getFailureReason() == null
                ? "NO_AVAILABLE_MACHINE" : request.getTrialPlan().getFailureReason();
        while (!remainingTrials.isEmpty()) {
            // 每次选择当前最优机台；该机台资源失败后移除并继续尝试下一候选机台。
            Cd15MachineTrial trial = trialSelector.select(remainingTrials);
            if (trial == null) {
                break;
            }
            remainingTrials.remove(trial);
            if (trial.getFinalSchedulableQuantity() == null
                    || trial.getFinalSchedulableQuantity().signum() <= 0) {
                String reason = trial.getLimitReason() == null ? lastFailureReason : trial.getLimitReason();
                log.warn("[斜裁自动排程] 当前班次资源提交失败, classField={}, steelStripCode={}, reason={}",
                        request.getClassField(), request.getSteelStripCode(), reason);
                return Cd15ShiftCommitResult.builder().success(false).failureReason(reason)
                        .state(originalState).build();
            }
            // 在原状态副本上试提交，任何失败都直接丢弃副本，保证资源修改原子性。
            Cd15ShiftResourceState working = copy(originalState);
            BigDecimal vehiclePlanQuantity = trial.getVehiclePlanQuantity();
            Cd15StorageLaneAllocationResult allocation = laneAllocator.allocate(
                    request.getSteelStripCode(), trial.getFinalSchedulableQuantity(),
                    vehiclePlanQuantity, working.getLanes());
            if (!allocation.isSuccess() || !acceptPartialAllocation(allocation, request)) {
                // 库排容量不足或部分排比例太小，均不提前修改工装和机台剩余时间。
                lastFailureReason = "STORAGE_LANE_LIMIT";
                continue;
            }
            int allocatedVehicles = allocation.getAllocatedVehicleCount();
            String partialReason = trial.isEqualShareApplied()
                    ? "EQUAL_SHARE"
                    : allocatedVehicles < allocation.getRequiredVehicleCount()
                            ? "STORAGE_LANE_LIMIT" : trial.getLimitReason();
            int availableTooling = working.getTotalToolingCount() - working.getOccupiedToolingCount();
            if (allocatedVehicles > availableTooling) {
                // 工装数按实际入库车数占用；不足时继续尝试其他方案但仍保留稳定失败原因。
                lastFailureReason = "ROLL_TOOL_LIMIT";
                continue;
            }

            int beforeSeconds = working.getRemainingSecondsByMachine().getOrDefault(
                    trial.getMachineCode(), fullShiftSeconds(request));
            BigDecimal committedQuantity = committedQuantity(trial, vehiclePlanQuantity, allocatedVehicles);
            if (trial.getRemainingSpecShiftQuantity() != null) {
                committedQuantity = committedQuantity.min(
                        trial.getRemainingSpecShiftQuantity());
            }
            BigDecimal bigRollConsumeQuantity = bigRollMeterCalculator.calculateForPlanQuantity(
                    committedQuantity, request.getUnitConsumeMillimeter(),
                    request.getCraftWidth(), request.getCordWidth(),
                    request.getSteelStripCode(), request.getBigRollCode());
            int elapsedBefore = Math.max(0, fullShiftSeconds(request) - beforeSeconds);
            LocalDateTime originalStart = request.getShiftStart().plusSeconds(elapsedBefore);
            Cd15BigRollAgingAllocation agingAllocation = commitAgingAllocation(
                    working, request.getBigRollCode(), bigRollConsumeQuantity, originalStart);
            if (agingAllocation != null && !agingAllocation.isSuccess()) {
                lastFailureReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
                continue;
            }
            int committedAgingDelaySeconds = agingAllocation == null
                    ? 0 : agingAllocation.getDelaySeconds();
            int afterSeconds = this.adjustedRemainingSeconds(
                    request, trial, beforeSeconds, committedQuantity,
                    committedAgingDelaySeconds);
            int productionDurationSeconds = Math.max(0,
                    beforeSeconds - afterSeconds - committedAgingDelaySeconds);
            LocalDateTime expectedStart = agingAllocation == null ? originalStart : agingAllocation.getTaskStartTime();
            int produceOrder = working.getTasks().stream()
                    .filter(item -> trial.getMachineCode().equals(item.getMachineCode()))
                    .map(Cd15ShiftScheduleTask::getProduceOrder)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            Cd15ShiftScheduleTask task = Cd15ShiftScheduleTask.builder()
                    .classField(request.getClassField()).materialKey(request.getMaterialKey())
                    .steelStripCode(request.getSteelStripCode())
                    .bigRollCode(request.getBigRollCode()).cordSpec(request.getCordSpec())
                    .cuttingAngle(request.getCuttingAngle())
                    .craftWidth(request.getCraftWidth())
                    .unitConsumeMillimeter(request.getUnitConsumeMillimeter())
                    .cordWidth(request.getCordWidth())
                    .curlLength(request.getCurlLength())
                    .bigRollConsumeQuantity(bigRollConsumeQuantity)
                    .cutMode(request.getCutMode()).splitGroupKey(request.getSplitGroupKey())
                    .machineCode(trial.getMachineCode())
                    .planQuantity(committedQuantity)
                    .vehicleCount(allocatedVehicles)
                    .produceOrder(produceOrder).expectedStartTime(expectedStart)
                    .expectedEndTime(expectedStart.plusSeconds(productionDurationSeconds))
                    .laneAllocations(allocation.getAllocations()).build();

            // 以下资源变更必须与任务一起提交，不能只扣减部分资源。
            working.setLanes(allocation.getLanes());
            working.setOccupiedToolingCount(working.getOccupiedToolingCount() + allocatedVehicles);
            working.getRemainingSecondsByMachine().put(trial.getMachineCode(), afterSeconds);
            working.getTailSpecByMachine().put(trial.getMachineCode(), request.getCordSpec());
            working.getTailByMachine().put(trial.getMachineCode(), Cd15MachineTailState.builder()
                    .materialKey(request.getMaterialKey())
                    .steelStripCode(request.getSteelStripCode())
                    .bigRollCode(request.getBigRollCode())
                    .cuttingAngle(request.getCuttingAngle())
                    .build());
            working.getTasks().add(task);
            log.info("[斜裁自动排程] 当前班次资源提交成功, classField={}, steelStripCode={}, machineCode={}, "
                            + "planQuantity={}, vehicleCount={}, requiredVehicleCount={}, produceOrder={}",
                    request.getClassField(), request.getSteelStripCode(), trial.getMachineCode(),
                    committedQuantity, allocatedVehicles, allocation.getRequiredVehicleCount(), produceOrder);
            BigDecimal equalShareRemainderQuantity = null;
            if (trial.isEqualShareApplied()) {
                BigDecimal plannedRemainder = trial.getEqualShareRemainderQuantity() == null
                        ? BigDecimal.ZERO : trial.getEqualShareRemainderQuantity();
                BigDecimal currentShiftShortage = trial.getActualQuantity()
                        .subtract(committedQuantity).max(BigDecimal.ZERO);
                equalShareRemainderQuantity = this.normalize(
                        plannedRemainder.add(currentShiftShortage));
            }
            return Cd15ShiftCommitResult.builder().success(true)
                    .partialReason(partialReason)
                    .equalShareRemainderQuantity(equalShareRemainderQuantity)
                    .state(working).task(task).build();
        }
        log.warn("[斜裁自动排程] 当前班次资源提交失败, classField={}, steelStripCode={}, reason={}",
                request.getClassField(), request.getSteelStripCode(), lastFailureReason);
        return Cd15ShiftCommitResult.builder().success(false).failureReason(lastFailureReason)
                .state(originalState).build();
    }

    /**
     * 将一个钢带规格按一出二方式原子提交为一条任务。
     * 两路使用相同规格并分别占用小车和工装，任一路失败时不修改原资源状态。
     */
    public Cd15ShiftCommitResult commitSingleSpecSplit(
            Cd15ShiftCommitRequest request,
            Cd15ShiftResourceState originalState) {
        if (request == null || request.getTrialPlan() == null
                || originalState == null) {
            throw new IllegalArgumentException(
                    "单规格分裁请求、试算方案和资源状态不能为空");
        }
        List<Cd15MachineTrial> remainingTrials =
                request.getTrialPlan().getTrials() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(request.getTrialPlan().getTrials());
        String lastFailureReason = request.getTrialPlan().getFailureReason() == null
                ? "NO_AVAILABLE_MACHINE"
                : request.getTrialPlan().getFailureReason();
        while (!remainingTrials.isEmpty()) {
            Cd15MachineTrial trial = this.trialSelector.select(remainingTrials);
            if (trial == null) {
                break;
            }
            remainingTrials.remove(trial);
            BigDecimal totalTrialQuantity = trial.getFinalSchedulableQuantity();
            if (totalTrialQuantity == null || totalTrialQuantity.signum() <= 0) {
                lastFailureReason = trial.getLimitReason() == null
                        ? lastFailureReason : trial.getLimitReason();
                continue;
            }
            BigDecimal branchTrialQuantity = totalTrialQuantity.divide(
                    new BigDecimal("2"), 10, RoundingMode.UNNECESSARY);
            BigDecimal vehiclePlanQuantity = trial.getVehiclePlanQuantity();
            Cd15ShiftResourceState preview = this.copy(originalState);
            Cd15StorageLaneAllocationResult firstPreview = this.laneAllocator.allocate(
                    request.getSteelStripCode(), branchTrialQuantity,
                    vehiclePlanQuantity, preview.getLanes());
            if (!firstPreview.isSuccess()) {
                lastFailureReason = "STORAGE_LANE_LIMIT";
                continue;
            }
            preview.setLanes(firstPreview.getLanes());
            Cd15StorageLaneAllocationResult secondPreview = this.laneAllocator.allocate(
                    request.getSteelStripCode(), branchTrialQuantity,
                    vehiclePlanQuantity, preview.getLanes());
            if (!secondPreview.isSuccess()) {
                lastFailureReason = "STORAGE_LANE_LIMIT";
                continue;
            }
            int pairVehicleCount = Math.min(
                    firstPreview.getAllocatedVehicleCount(),
                    secondPreview.getAllocatedVehicleCount());
            int requiredPairVehicleCount = Math.max(
                    firstPreview.getRequiredVehicleCount(),
                    secondPreview.getRequiredVehicleCount());
            int totalVehicleCount = pairVehicleCount * 2;
            if (pairVehicleCount <= 0
                    || totalVehicleCount > originalState.getTotalToolingCount()
                    - originalState.getOccupiedToolingCount()
                    || pairVehicleCount < requiredPairVehicleCount
                    && !request.isCloseOut()
                    && totalVehicleCount < Math.max(
                            1, request.getPartialMinVehicleCount())) {
                lastFailureReason = pairVehicleCount <= 0
                        || pairVehicleCount < requiredPairVehicleCount
                        ? "STORAGE_LANE_LIMIT" : "ROLL_TOOL_LIMIT";
                continue;
            }

            BigDecimal branchCommittedQuantity = branchTrialQuantity.min(
                    vehiclePlanQuantity.multiply(
                            BigDecimal.valueOf(pairVehicleCount)));
            BigDecimal committedQuantity = this.normalizeCommittedQuantity(
                    branchCommittedQuantity.multiply(new BigDecimal("2")));
            Cd15ShiftResourceState working = this.copy(originalState);
            Cd15StorageLaneAllocationResult firstAllocation =
                    this.laneAllocator.allocate(
                            request.getSteelStripCode(), branchCommittedQuantity,
                            vehiclePlanQuantity, working.getLanes());
            working.setLanes(firstAllocation.getLanes());
            Cd15StorageLaneAllocationResult secondAllocation =
                    this.laneAllocator.allocate(
                            request.getSteelStripCode(), branchCommittedQuantity,
                            vehiclePlanQuantity, working.getLanes());
            if (!firstAllocation.isSuccess() || !secondAllocation.isSuccess()) {
                lastFailureReason = "STORAGE_LANE_LIMIT";
                continue;
            }

            BigDecimal bigRollConsumeQuantity =
                    this.bigRollMeterCalculator.calculateForPlanQuantity(
                            committedQuantity,
                            request.getUnitConsumeMillimeter(),
                            request.getCraftWidth(), request.getCordWidth(),
                            request.getSteelStripCode(), request.getBigRollCode());
            int beforeSeconds = working.getRemainingSecondsByMachine()
                    .getOrDefault(trial.getMachineCode(),
                            this.fullShiftSeconds(request));
            int elapsedBefore = Math.max(0,
                    this.fullShiftSeconds(request) - beforeSeconds);
            LocalDateTime originalStart = request.getShiftStart()
                    .plusSeconds(elapsedBefore);
            Cd15BigRollAgingAllocation agingAllocation =
                    this.commitAgingAllocation(
                            working, request.getBigRollCode(),
                            bigRollConsumeQuantity, originalStart);
            if (agingAllocation != null && !agingAllocation.isSuccess()) {
                lastFailureReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
                continue;
            }
            int committedAgingDelaySeconds = agingAllocation == null
                    ? 0 : agingAllocation.getDelaySeconds();
            int afterSeconds = this.adjustedRemainingSeconds(
                    request, trial, beforeSeconds, committedQuantity,
                    committedAgingDelaySeconds);
            LocalDateTime expectedStart = agingAllocation == null
                    ? originalStart : agingAllocation.getTaskStartTime();
            int productionDurationSeconds = Math.max(0,
                    beforeSeconds - afterSeconds - committedAgingDelaySeconds);
            int produceOrder = working.getTasks().stream()
                    .filter(item -> trial.getMachineCode().equals(
                            item.getMachineCode()))
                    .map(Cd15ShiftScheduleTask::getProduceOrder)
                    .max(Integer::compareTo).orElse(0) + 1;
            List<Cd15StorageLaneAllocation> laneAllocations =
                    this.mergeLaneAllocations(
                            firstAllocation.getAllocations(),
                            secondAllocation.getAllocations());
            Cd15ShiftScheduleTask task = Cd15ShiftScheduleTask.builder()
                    .classField(request.getClassField())
                    .materialKey(request.getMaterialKey())
                    .steelStripCode(request.getSteelStripCode())
                    .bigRollCode(request.getBigRollCode())
                    .cordSpec(request.getCordSpec())
                    .cuttingAngle(request.getCuttingAngle())
                    .craftWidth(request.getCraftWidth())
                    .unitConsumeMillimeter(request.getUnitConsumeMillimeter())
                    .cordWidth(request.getCordWidth())
                    .curlLength(request.getCurlLength())
                    .bigRollConsumeQuantity(bigRollConsumeQuantity)
                    .cutMode("SPLIT")
                    .splitGroupKey(null)
                    .machineCode(trial.getMachineCode())
                    .planQuantity(committedQuantity)
                    .vehicleCount(totalVehicleCount)
                    .produceOrder(produceOrder)
                    .expectedStartTime(expectedStart)
                    .expectedEndTime(expectedStart.plusSeconds(
                            productionDurationSeconds))
                    .laneAllocations(laneAllocations).build();
            working.setLanes(secondAllocation.getLanes());
            working.setOccupiedToolingCount(
                    working.getOccupiedToolingCount() + totalVehicleCount);
            working.getRemainingSecondsByMachine().put(
                    trial.getMachineCode(), afterSeconds);
            working.getTailSpecByMachine().put(
                    trial.getMachineCode(), request.getCordSpec());
            working.getTailByMachine().put(
                    trial.getMachineCode(), Cd15MachineTailState.builder()
                            .materialKey(request.getMaterialKey())
                            .steelStripCode(request.getSteelStripCode())
                            .bigRollCode(request.getBigRollCode())
                            .cuttingAngle(request.getCuttingAngle()).build());
            working.getTasks().add(task);
            String partialReason = trial.isEqualShareApplied()
                    ? "EQUAL_SHARE"
                    : pairVehicleCount < requiredPairVehicleCount
                            ? "STORAGE_LANE_LIMIT" : trial.getLimitReason();
            BigDecimal equalShareRemainderQuantity = null;
            if (trial.isEqualShareApplied()) {
                BigDecimal plannedRemainder = trial.getEqualShareRemainderQuantity() == null
                        ? BigDecimal.ZERO : trial.getEqualShareRemainderQuantity();
                BigDecimal currentShiftShortage = trial.getActualQuantity()
                        .subtract(committedQuantity).max(BigDecimal.ZERO);
                equalShareRemainderQuantity = this.normalize(
                        plannedRemainder.add(currentShiftShortage));
            }
            log.info("[斜裁自动排程] 单规格分裁资源提交成功, classField={}, "
                            + "steelStripCode={}, machineCode={}, planQuantity={}, "
                            + "branchQuantity={}, vehicleCount={}",
                    request.getClassField(), request.getSteelStripCode(),
                    trial.getMachineCode(), committedQuantity,
                    branchCommittedQuantity, totalVehicleCount);
            return Cd15ShiftCommitResult.builder().success(true)
                    .partialReason(partialReason)
                    .equalShareRemainderQuantity(
                            equalShareRemainderQuantity)
                    .state(working).task(task).build();
        }
        return Cd15ShiftCommitResult.builder().success(false)
                .failureReason(lastFailureReason).state(originalState).build();
    }

    /**
     * 将两条分裁钢带作为同一作业单元提交。
     *
     * <p>库排和工装按钢带分别占用；机台切换、生产时长和大卷成熟等待按组合只扣一次。
     * 任一钢带资源不足时丢弃工作副本，保证组合不会拆开。</p>
     *
     * @param firstRequest 第一条钢带请求
     * @param secondRequest 第二条钢带请求
     * @param originalState 原资源状态
     * @return 分裁组合提交结果
     */
    public com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult commitSplit(
            Cd15ShiftCommitRequest firstRequest,
            Cd15ShiftCommitRequest secondRequest,
            Cd15ShiftResourceState originalState) {
        if (firstRequest == null || secondRequest == null || originalState == null
                || firstRequest.getTrialPlan() == null
                || secondRequest.getTrialPlan() == null) {
            throw new IllegalArgumentException("分裁组合请求、试算方案和资源状态不能为空");
        }
        if (!java.util.Objects.equals(firstRequest.getClassField(), secondRequest.getClassField())
                || !java.util.Objects.equals(firstRequest.getBigRollCode(), secondRequest.getBigRollCode())
                || !java.util.Objects.equals(firstRequest.getSplitGroupKey(), secondRequest.getSplitGroupKey())) {
            throw new IllegalArgumentException("分裁组合必须同班次、同大卷并共用分组键");
        }

        List<Cd15MachineTrial> remainingFirstTrials =
                firstRequest.getTrialPlan().getTrials() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(firstRequest.getTrialPlan().getTrials());
        String lastFailureReason = "NO_AVAILABLE_MACHINE";
        while (!remainingFirstTrials.isEmpty()) {
            Cd15MachineTrial firstTrial = trialSelector.select(remainingFirstTrials);
            if (firstTrial == null) {
                break;
            }
            remainingFirstTrials.remove(firstTrial);
            Cd15MachineTrial secondTrial = secondRequest.getTrialPlan().getTrials() == null
                    ? null : secondRequest.getTrialPlan().getTrials().stream()
                            .filter(item -> item != null
                                    && java.util.Objects.equals(
                                            firstTrial.getMachineCode(), item.getMachineCode()))
                            .findFirst().orElse(null);
            if (secondTrial == null
                    || firstTrial.getFinalSchedulableQuantity() == null
                    || secondTrial.getFinalSchedulableQuantity() == null
                    || firstTrial.getFinalSchedulableQuantity().signum() <= 0
                    || secondTrial.getFinalSchedulableQuantity().signum() <= 0) {
                lastFailureReason = secondTrial == null
                        ? "NO_AVAILABLE_MACHINE"
                        : java.util.Objects.toString(secondTrial.getLimitReason(),
                                java.util.Objects.toString(firstTrial.getLimitReason(),
                                        lastFailureReason));
                continue;
            }

            Cd15ShiftResourceState working = this.copy(originalState);
            Cd15StorageLaneAllocationResult firstAllocation = laneAllocator.allocate(
                    firstRequest.getSteelStripCode(),
                    firstTrial.getFinalSchedulableQuantity(),
                    firstTrial.getVehiclePlanQuantity(), working.getLanes());
            if (!firstAllocation.isSuccess()
                    || !this.acceptPartialAllocation(firstAllocation, firstRequest)) {
                lastFailureReason = "STORAGE_LANE_LIMIT";
                continue;
            }
            working.setLanes(firstAllocation.getLanes());
            Cd15StorageLaneAllocationResult secondAllocation = laneAllocator.allocate(
                    secondRequest.getSteelStripCode(),
                    secondTrial.getFinalSchedulableQuantity(),
                    secondTrial.getVehiclePlanQuantity(), working.getLanes());
            if (!secondAllocation.isSuccess()
                    || !this.acceptPartialAllocation(secondAllocation, secondRequest)) {
                lastFailureReason = "STORAGE_LANE_LIMIT";
                continue;
            }

            int firstVehicles = firstAllocation.getAllocatedVehicleCount();
            int secondVehicles = secondAllocation.getAllocatedVehicleCount();
            int toolingRequired = firstVehicles + secondVehicles;
            int toolingAvailable = working.getTotalToolingCount()
                    - working.getOccupiedToolingCount();
            if (toolingRequired > toolingAvailable) {
                lastFailureReason = "ROLL_TOOL_LIMIT";
                continue;
            }

            BigDecimal firstQuantity = this.committedQuantity(
                    firstTrial, firstTrial.getVehiclePlanQuantity(), firstVehicles);
            BigDecimal secondQuantity = this.committedQuantity(
                    secondTrial, secondTrial.getVehiclePlanQuantity(), secondVehicles);
            BigDecimal firstBigRollConsume = bigRollMeterCalculator.calculateForPlanQuantity(
                    firstQuantity, firstRequest.getUnitConsumeMillimeter(),
                    firstRequest.getCraftWidth(), firstRequest.getCordWidth(),
                    firstRequest.getSteelStripCode(), firstRequest.getBigRollCode());
            BigDecimal secondBigRollConsume = bigRollMeterCalculator.calculateForPlanQuantity(
                    secondQuantity, secondRequest.getUnitConsumeMillimeter(),
                    secondRequest.getCraftWidth(), secondRequest.getCordWidth(),
                    secondRequest.getSteelStripCode(), secondRequest.getBigRollCode());
            BigDecimal combinedBigRollConsume =
                    firstBigRollConsume.add(secondBigRollConsume);

            int beforeSeconds = working.getRemainingSecondsByMachine().getOrDefault(
                    firstTrial.getMachineCode(), this.fullShiftSeconds(firstRequest));
            int elapsedBefore = Math.max(
                    0, this.fullShiftSeconds(firstRequest) - beforeSeconds);
            LocalDateTime originalStart =
                    firstRequest.getShiftStart().plusSeconds(elapsedBefore);
            Cd15BigRollAgingAllocation agingAllocation = this.commitAgingAllocation(
                    working, firstRequest.getBigRollCode(),
                    combinedBigRollConsume, originalStart);
            if (agingAllocation != null && !agingAllocation.isSuccess()) {
                lastFailureReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
                continue;
            }

            int agingDelaySeconds = agingAllocation == null
                    ? 0 : agingAllocation.getDelaySeconds();
            int changeSeconds = Math.max(
                    firstTrial.getChangeSeconds(), secondTrial.getChangeSeconds());
            int productionSeconds = Math.max(
                    this.scaledProductionSeconds(firstTrial, firstQuantity),
                    this.scaledProductionSeconds(secondTrial, secondQuantity));
            int occupiedSeconds = agingDelaySeconds + changeSeconds + productionSeconds;
            if (occupiedSeconds > beforeSeconds) {
                lastFailureReason = "CAPACITY_LIMIT";
                continue;
            }
            int afterSeconds = beforeSeconds - occupiedSeconds;
            LocalDateTime expectedStart = agingAllocation == null
                    ? originalStart : agingAllocation.getTaskStartTime();
            LocalDateTime expectedEnd =
                    expectedStart.plusSeconds(changeSeconds + productionSeconds);
            int produceOrder = working.getTasks().stream()
                    .filter(item -> firstTrial.getMachineCode().equals(
                            item.getMachineCode()))
                    .map(Cd15ShiftScheduleTask::getProduceOrder)
                    .max(Integer::compareTo).orElse(0) + 1;

            Cd15ShiftScheduleTask firstTask = this.splitTask(
                    firstRequest, firstTrial.getMachineCode(),
                    firstQuantity, firstBigRollConsume, firstVehicles,
                    firstAllocation, produceOrder, expectedStart, expectedEnd);
            Cd15ShiftScheduleTask secondTask = this.splitTask(
                    secondRequest, firstTrial.getMachineCode(),
                    secondQuantity, secondBigRollConsume, secondVehicles,
                    secondAllocation, produceOrder, expectedStart, expectedEnd);

            working.setLanes(secondAllocation.getLanes());
            working.setOccupiedToolingCount(
                    working.getOccupiedToolingCount() + toolingRequired);
            working.getRemainingSecondsByMachine().put(
                    firstTrial.getMachineCode(), afterSeconds);
            working.getTailSpecByMachine().put(
                    firstTrial.getMachineCode(), secondRequest.getCordSpec());
            working.getTailByMachine().put(
                    firstTrial.getMachineCode(), Cd15MachineTailState.builder()
                            .materialKey(firstRequest.getSplitGroupKey())
                            .steelStripCode(firstRequest.getSteelStripCode()
                                    + "+" + secondRequest.getSteelStripCode())
                            .bigRollCode(firstRequest.getBigRollCode())
                            .cuttingAngle(firstRequest.getCuttingAngle())
                            .build());
            working.getTasks().add(firstTask);
            working.getTasks().add(secondTask);

            log.info("[斜裁自动排程] 分裁组合资源提交成功, classField={}, groupKey={}, "
                            + "machineCode={}, firstSteelStrip={}, secondSteelStrip={}, "
                            + "firstQuantity={}, secondQuantity={}, bigRollConsume={}",
                    firstRequest.getClassField(), firstRequest.getSplitGroupKey(),
                    firstTrial.getMachineCode(), firstRequest.getSteelStripCode(),
                    secondRequest.getSteelStripCode(), firstQuantity,
                    secondQuantity, combinedBigRollConsume);
            return com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult.builder()
                    .success(true).state(working)
                    .firstTask(firstTask).secondTask(secondTask)
                    .firstPartialReason(this.partialReason(
                            firstAllocation, firstTrial))
                    .secondPartialReason(this.partialReason(
                            secondAllocation, secondTrial))
                    .build();
        }
        log.warn("[斜裁自动排程] 分裁组合资源提交失败, classField={}, groupKey={}, reason={}",
                firstRequest.getClassField(), firstRequest.getSplitGroupKey(),
                lastFailureReason);
        return com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult.builder()
                .success(false).state(originalState)
                .failureReason(lastFailureReason).build();
    }

    private Cd15ShiftScheduleTask splitTask(
            Cd15ShiftCommitRequest request,
            String machineCode,
            BigDecimal quantity,
            BigDecimal bigRollConsumeQuantity,
            int vehicles,
            Cd15StorageLaneAllocationResult allocation,
            int produceOrder,
            LocalDateTime expectedStart,
            LocalDateTime expectedEnd) {
        return Cd15ShiftScheduleTask.builder()
                .classField(request.getClassField())
                .materialKey(request.getMaterialKey())
                .steelStripCode(request.getSteelStripCode())
                .bigRollCode(request.getBigRollCode())
                .cordSpec(request.getCordSpec())
                .cuttingAngle(request.getCuttingAngle())
                .craftWidth(request.getCraftWidth())
                .unitConsumeMillimeter(request.getUnitConsumeMillimeter())
                .cordWidth(request.getCordWidth())
                .curlLength(request.getCurlLength())
                .bigRollConsumeQuantity(bigRollConsumeQuantity)
                .cutMode(request.getCutMode())
                .splitGroupKey(request.getSplitGroupKey())
                .machineCode(machineCode)
                .planQuantity(quantity)
                .vehicleCount(vehicles)
                .produceOrder(produceOrder)
                .expectedStartTime(expectedStart)
                .expectedEndTime(expectedEnd)
                .laneAllocations(allocation.getAllocations())
                .build();
    }

    private int scaledProductionSeconds(
            Cd15MachineTrial trial, BigDecimal committedQuantity) {
        if (trial.getProductionSeconds() <= 0
                || trial.getFinalSchedulableQuantity() == null
                || trial.getFinalSchedulableQuantity().signum() <= 0) {
            return 0;
        }
        return committedQuantity.multiply(
                        BigDecimal.valueOf(trial.getProductionSeconds()))
                .divide(trial.getFinalSchedulableQuantity(),
                        0, RoundingMode.CEILING)
                .intValueExact();
    }

    private String partialReason(
            Cd15StorageLaneAllocationResult allocation,
            Cd15MachineTrial trial) {
        return allocation.getAllocatedVehicleCount()
                < allocation.getRequiredVehicleCount()
                ? "STORAGE_LANE_LIMIT" : trial.getLimitReason();
    }

    private boolean acceptPartialAllocation(Cd15StorageLaneAllocationResult allocation,
                                            Cd15ShiftCommitRequest request) {
        int assigned = allocation.getAllocatedVehicleCount();
        int required = allocation.getRequiredVehicleCount();
        if (assigned <= 0) {
            return false;
        }
        if (assigned >= required || request.isCloseOut()) {
            return true;
        }
        // 非收尾部分排不再按比例判断，而按参数配置的最小车数判断，避免大需求场景被比例阈值长期卡死。
        return assigned >= Math.max(1, request.getPartialMinVehicleCount());
    }

    private BigDecimal committedQuantity(Cd15MachineTrial trial, BigDecimal vehiclePlanQuantity,
                                         int allocatedVehicles) {
        BigDecimal laneQuantity = vehiclePlanQuantity.multiply(BigDecimal.valueOf(allocatedVehicles));
        BigDecimal trialQuantity = trial.getFinalSchedulableQuantity() == null
                ? BigDecimal.ZERO : trial.getFinalSchedulableQuantity();
        BigDecimal result = trialQuantity.signum() > 0 ? laneQuantity.min(trialQuantity) : laneQuantity;
        return normalizeCommittedQuantity(result);
    }

    /** 合并两路同规格库排分配，同一库排累计车数。 */
    private List<Cd15StorageLaneAllocation> mergeLaneAllocations(
            List<Cd15StorageLaneAllocation> first,
            List<Cd15StorageLaneAllocation> second) {
        Map<String, Integer> vehicleCountByLane = new LinkedHashMap<>();
        List<Cd15StorageLaneAllocation> source = new ArrayList<>();
        if (first != null) {
            source.addAll(first);
        }
        if (second != null) {
            source.addAll(second);
        }
        source.forEach(item -> vehicleCountByLane.merge(
                item.getLaneCode(), item.getVehicleCount(), Integer::sum));
        return vehicleCountByLane.entrySet().stream()
                .map(entry -> Cd15StorageLaneAllocation.builder()
                        .laneCode(entry.getKey())
                        .vehicleCount(entry.getValue()).build())
                .collect(Collectors.toList());
    }

    private int adjustedRemainingSeconds(Cd15ShiftCommitRequest request, Cd15MachineTrial trial,
                                         int beforeSeconds, BigDecimal committedQuantity,
                                         int committedAgingDelaySeconds) {
        BigDecimal trialQuantity = trial.getFinalSchedulableQuantity();
        if (trialQuantity == null || (committedQuantity.compareTo(trialQuantity) >= 0
                && committedAgingDelaySeconds == trial.getAgingDelaySeconds())
                || trial.getProductionSeconds() <= 0) {
            return trial.getRemainingSeconds();
        }
        int productionSeconds = committedQuantity.multiply(BigDecimal.valueOf(trial.getProductionSeconds()))
                .divide(trialQuantity, 0, RoundingMode.CEILING).intValueExact();
        return Math.max(0, beforeSeconds - committedAgingDelaySeconds
                - trial.getChangeSeconds() - productionSeconds);
    }

    /**
     * 归一化最终提交计划量。当前过渡规则按整数米向上取整，后续改为整卷/整车/整条时只替换本方法。
     */
    private BigDecimal normalizeCommittedQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return quantity.setScale(0, RoundingMode.CEILING);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }

    private int fullShiftSeconds(Cd15ShiftCommitRequest request) {
        return Math.toIntExact(ChronoUnit.SECONDS.between(request.getShiftStart(), request.getShiftEnd()));
    }

    private Cd15ShiftResourceState copy(Cd15ShiftResourceState source) {
        List<Cd15StorageLaneState> lanes = source.getLanes() == null ? new ArrayList<>()
                : source.getLanes().stream().map(item -> Cd15StorageLaneState.builder()
                        .laneCode(item.getLaneCode()).steelStripCode(item.getSteelStripCode())
                        .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                        .build()).collect(Collectors.toList());
        return Cd15ShiftResourceState.builder().lanes(lanes)
                .totalToolingCount(source.getTotalToolingCount())
                .occupiedToolingCount(source.getOccupiedToolingCount())
                .remainingSecondsByMachine(copyMap(source.getRemainingSecondsByMachine()))
                .tailSpecByMachine(copyMap(source.getTailSpecByMachine()))
                .tailByMachine(copyTailMap(source.getTailByMachine()))
                .tasks(source.getTasks() == null ? new ArrayList<>() : new ArrayList<>(source.getTasks()))
                .bigRollAgingStocks(copyAgingStocks(source.getBigRollAgingStocks()))
                .build();
    }

    private Map<String, Cd15MachineTailState> copyTailMap(Map<String, Cd15MachineTailState> source) {
        Map<String, Cd15MachineTailState> result = new HashMap<>();
        if (source != null) {
            source.forEach((machineCode, tail) -> result.put(machineCode, tail == null ? null
                    : Cd15MachineTailState.builder()
                            .materialKey(tail.getMaterialKey())
                            .steelStripCode(tail.getSteelStripCode())
                            .bigRollCode(tail.getBigRollCode())
                            .cuttingAngle(tail.getCuttingAngle())
                            .build()));
        }
        return result;
    }

    private Cd15BigRollAgingAllocation commitAgingAllocation(Cd15ShiftResourceState working,
                                                             String bigRollCode,
                                                             BigDecimal committedQuantity,
                                                             LocalDateTime originalStart) {
        return agingAllocator.allocate(working.getBigRollAgingStocks(), bigRollCode, committedQuantity, originalStart);
    }

    private List<Cd15BigRollAgingStock> copyAgingStocks(List<Cd15BigRollAgingStock> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().map(item -> Cd15BigRollAgingStock.builder()
                .sourceType(item.getSourceType())
                .sourceId(item.getSourceId())
                .bigRollCode(item.getBigRollCode())
                .bigRollBarcode(item.getBigRollBarcode())
                .availableQuantity(item.getAvailableQuantity())
                .allocatedQuantity(item.getAllocatedQuantity())
                .stockInTime(item.getStockInTime())
                .releaseTime(item.getReleaseTime())
                .build()).collect(Collectors.toList());
    }
    private <K, V> Map<K, V> copyMap(Map<K, V> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }
}
