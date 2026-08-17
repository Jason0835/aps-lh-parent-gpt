package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingAllocation;
import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingStock;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import com.zlt.aps.cd90.engine.model.Cd90ShiftCommitRequest;
import com.zlt.aps.cd90.engine.model.Cd90ShiftCommitResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocationResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
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
public class Cd90ShiftResourceCommitter {

    private final Cd90StorageLaneAllocator laneAllocator;
    private final Cd90MachineTrialSelector trialSelector;
    private final Cd90BigRollAgingAllocator agingAllocator;

    /**
     * 按试算优先级逐方案尝试，并原子提交当前班次内存资源。
     *
     * @param request 单规格提交请求
     * @param originalState 原资源状态
     * @return 成功后的新状态和任务，或失败原因及未修改原状态
     */
    public Cd90ShiftCommitResult commit(Cd90ShiftCommitRequest request,
                                        Cd90ShiftResourceState originalState) {
        if (request == null || request.getTrialPlan() == null || originalState == null) {
            throw new IllegalArgumentException("班次提交请求、试算方案和资源状态不能为空");
        }
        List<Cd90MachineTrial> remainingTrials = request.getTrialPlan().getTrials() == null
                ? new ArrayList<>() : new ArrayList<>(request.getTrialPlan().getTrials());
        String lastFailureReason = request.getTrialPlan().getFailureReason() == null
                ? "NO_AVAILABLE_MACHINE" : request.getTrialPlan().getFailureReason();
        while (!remainingTrials.isEmpty()) {
            // 每次选择当前最优机台；该机台资源失败后移除并继续尝试下一候选机台。
            Cd90MachineTrial trial = trialSelector.select(remainingTrials);
            if (trial == null) {
                break;
            }
            remainingTrials.remove(trial);
            if (trial.getFinalSchedulableQuantity() == null
                    || trial.getFinalSchedulableQuantity().signum() <= 0) {
                String reason = trial.getLimitReason() == null ? lastFailureReason : trial.getLimitReason();
                log.warn("[直裁自动排程] 当前班次资源提交失败, classField={}, clothCode={}, reason={}",
                        request.getClassField(), request.getClothCode(), reason);
                return Cd90ShiftCommitResult.builder().success(false).failureReason(reason)
                        .state(originalState).build();
            }
            // 在原状态副本上试提交，任何失败都直接丢弃副本，保证资源修改原子性。
            Cd90ShiftResourceState working = copy(originalState);
            BigDecimal standardCurlLength = trial.getStandardCurlLength();
            Cd90StorageLaneAllocationResult allocation = laneAllocator.allocate(
                    request.getClothCode(), trial.getMachineCode(), trial.getFinalSchedulableQuantity(),
                    standardCurlLength, working.getLanes());
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
            BigDecimal committedQuantity = committedQuantity(trial, standardCurlLength, allocatedVehicles);
            int elapsedBefore = Math.max(0, fullShiftSeconds(request) - beforeSeconds);
            LocalDateTime originalStart = request.getShiftStart().plusSeconds(elapsedBefore);
            Cd90BigRollAgingAllocation agingAllocation = commitAgingAllocation(
                    working, request.getBigRollCode(), committedQuantity, originalStart);
            if (agingAllocation != null && !agingAllocation.isSuccess()) {
                lastFailureReason = Cd90BigRollAgingAllocator.AGING_PERIOD_LIMIT;
                continue;
            }
            int committedAgingDelaySeconds = agingAllocation == null ? 0 : agingAllocation.getDelaySeconds();
            int afterSeconds = this.adjustedRemainingSeconds(
                    request, trial, beforeSeconds, committedQuantity, committedAgingDelaySeconds);
            int productionDurationSeconds = Math.max(
                    0, beforeSeconds - afterSeconds - committedAgingDelaySeconds);
            LocalDateTime expectedStart = agingAllocation == null ? originalStart : agingAllocation.getTaskStartTime();
            int produceOrder = (int) working.getTasks().stream()
                    .filter(item -> trial.getMachineCode().equals(item.getMachineCode())).count() + 1;
            Cd90ShiftScheduleTask task = Cd90ShiftScheduleTask.builder()
                    .classField(request.getClassField()).clothCode(request.getClothCode())
                    .bigRollCode(request.getBigRollCode()).cordSpec(request.getCordSpec())
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
            working.getTailByMachine().put(trial.getMachineCode(), Cd90MachineTailState.builder()
                    .clothCode(request.getClothCode()).bigRollCode(request.getBigRollCode()).build());
            working.getTasks().add(task);
            log.info("[直裁自动排程] 当前班次资源提交成功, classField={}, clothCode={}, machineCode={}, "
                            + "planQuantity={}, vehicleCount={}, requiredVehicleCount={}, produceOrder={}",
                    request.getClassField(), request.getClothCode(), trial.getMachineCode(),
                    committedQuantity, allocatedVehicles, allocation.getRequiredVehicleCount(), produceOrder);
            return Cd90ShiftCommitResult.builder().success(true).partialReason(partialReason)
                    .state(working).task(task).build();
        }
        log.warn("[直裁自动排程] 当前班次资源提交失败, classField={}, clothCode={}, reason={}",
                request.getClassField(), request.getClothCode(), lastFailureReason);
        return Cd90ShiftCommitResult.builder().success(false).failureReason(lastFailureReason)
                .state(originalState).build();
    }

    private boolean acceptPartialAllocation(Cd90StorageLaneAllocationResult allocation,
                                            Cd90ShiftCommitRequest request) {
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

    private BigDecimal committedQuantity(Cd90MachineTrial trial, BigDecimal standardCurlLength,
                                          int allocatedVehicles) {
        BigDecimal laneQuantity = standardCurlLength.multiply(BigDecimal.valueOf(allocatedVehicles));
        BigDecimal trialQuantity = trial.getFinalSchedulableQuantity() == null
                ? BigDecimal.ZERO : trial.getFinalSchedulableQuantity();
        BigDecimal result = trialQuantity.signum() > 0 ? laneQuantity.min(trialQuantity) : laneQuantity;
        return normalizeCommittedQuantity(result);
    }

    private int adjustedRemainingSeconds(Cd90ShiftCommitRequest request, Cd90MachineTrial trial,
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

    private int fullShiftSeconds(Cd90ShiftCommitRequest request) {
        return Math.toIntExact(ChronoUnit.SECONDS.between(request.getShiftStart(), request.getShiftEnd()));
    }

    private Cd90ShiftResourceState copy(Cd90ShiftResourceState source) {
        List<Cd90StorageLaneState> lanes = source.getLanes() == null ? new ArrayList<>()
                : source.getLanes().stream().map(item -> Cd90StorageLaneState.builder()
                        .laneCode(item.getLaneCode()).machineCode(item.getMachineCode())
                        .clothCode(item.getClothCode())
                        .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                        .build()).collect(Collectors.toList());
        return Cd90ShiftResourceState.builder().lanes(lanes)
                .totalToolingCount(source.getTotalToolingCount())
                .occupiedToolingCount(source.getOccupiedToolingCount())
                .remainingSecondsByMachine(copyMap(source.getRemainingSecondsByMachine()))
                .tailSpecByMachine(copyMap(source.getTailSpecByMachine()))
                .tailByMachine(copyTailMap(source.getTailByMachine()))
                .tasks(source.getTasks() == null ? new ArrayList<>() : new ArrayList<>(source.getTasks()))
                .bigRollAgingStocks(copyAgingStocks(source.getBigRollAgingStocks()))
                .build();
    }

    private Map<String, Cd90MachineTailState> copyTailMap(Map<String, Cd90MachineTailState> source) {
        Map<String, Cd90MachineTailState> result = new HashMap<>();
        if (source != null) {
            source.forEach((machineCode, tail) -> result.put(machineCode, tail == null ? null
                    : Cd90MachineTailState.builder().clothCode(tail.getClothCode())
                            .bigRollCode(tail.getBigRollCode()).build()));
        }
        return result;
    }

    private Cd90BigRollAgingAllocation commitAgingAllocation(Cd90ShiftResourceState working,
                                                             String bigRollCode,
                                                             BigDecimal committedQuantity,
                                                             LocalDateTime originalStart) {
        if (working.getBigRollAgingStocks() == null || working.getBigRollAgingStocks().isEmpty()) {
            return null;
        }
        return agingAllocator.allocate(working.getBigRollAgingStocks(), bigRollCode, committedQuantity, originalStart);
    }

    private List<Cd90BigRollAgingStock> copyAgingStocks(List<Cd90BigRollAgingStock> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().map(item -> Cd90BigRollAgingStock.builder()
                .sourceType(item.getSourceType())
                .sourceId(item.getSourceId())
                .clothCode(item.getClothCode())
                .bigRollCode(item.getBigRollCode())
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
