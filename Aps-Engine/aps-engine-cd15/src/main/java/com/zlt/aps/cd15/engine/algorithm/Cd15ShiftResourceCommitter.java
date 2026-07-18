package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitRequest;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
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
            String partialReason = allocatedVehicles < allocation.getRequiredVehicleCount()
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
            int afterSeconds = adjustedRemainingSeconds(request, trial, beforeSeconds, committedQuantity);
            int elapsedBefore = Math.max(0, fullShiftSeconds(request) - beforeSeconds);
            int productionDurationSeconds = Math.max(0, beforeSeconds - afterSeconds - trial.getAgingDelaySeconds());
            LocalDateTime originalStart = request.getShiftStart().plusSeconds(elapsedBefore);
            Cd15BigRollAgingAllocation agingAllocation = commitAgingAllocation(
                    working, request.getBigRollCode(), committedQuantity, originalStart);
            if (agingAllocation != null && !agingAllocation.isSuccess()) {
                lastFailureReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
                continue;
            }
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
                    .reinforcement(request.isReinforcement())
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
            return Cd15ShiftCommitResult.builder().success(true).partialReason(partialReason)
                    .state(working).task(task).build();
        }
        log.warn("[斜裁自动排程] 当前班次资源提交失败, classField={}, steelStripCode={}, reason={}",
                request.getClassField(), request.getSteelStripCode(), lastFailureReason);
        return Cd15ShiftCommitResult.builder().success(false).failureReason(lastFailureReason)
                .state(originalState).build();
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

    private int adjustedRemainingSeconds(Cd15ShiftCommitRequest request, Cd15MachineTrial trial,
                                         int beforeSeconds, BigDecimal committedQuantity) {
        BigDecimal trialQuantity = trial.getFinalSchedulableQuantity();
        if (trialQuantity == null || committedQuantity.compareTo(trialQuantity) >= 0
                || trial.getProductionSeconds() <= 0) {
            return trial.getRemainingSeconds();
        }
        int productionSeconds = committedQuantity.multiply(BigDecimal.valueOf(trial.getProductionSeconds()))
                .divide(trialQuantity, 0, RoundingMode.CEILING).intValueExact();
        return Math.max(0, beforeSeconds - trial.getAgingDelaySeconds()
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
        if (working.getBigRollAgingStocks() == null || working.getBigRollAgingStocks().isEmpty()) {
            return null;
        }
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