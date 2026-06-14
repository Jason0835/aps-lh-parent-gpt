package com.zlt.aps.cd90.engine.algorithm;

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
        String lastFailureReason = "NO_AVAILABLE_MACHINE";
        while (!remainingTrials.isEmpty()) {
            // 每次选择当前最优机台；该机台资源失败后移除并继续尝试下一候选机台。
            Cd90MachineTrial trial = trialSelector.select(remainingTrials);
            if (trial == null) {
                break;
            }
            remainingTrials.remove(trial);
            // 在原状态副本上试提交，任何失败都直接丢弃副本，保证资源修改原子性。
            Cd90ShiftResourceState working = copy(originalState);
            Cd90StorageLaneAllocationResult allocation = laneAllocator.allocate(
                    request.getClothCode(), trial.getFinalSchedulableQuantity(),
                    request.getCoilMeter(), working.getLanes());
            if (!allocation.isSuccess()) {
                // 库排容量不足属于当前机台方案失败，不提前修改工装和机台剩余时间。
                lastFailureReason = allocation.getFailureReason();
                continue;
            }
            int availableTooling = working.getTotalToolingCount() - working.getOccupiedToolingCount();
            if (allocation.getRequiredVehicleCount() > availableTooling) {
                // 工装数按入库车数占用；不足时继续尝试其他方案但仍保留稳定失败原因。
                lastFailureReason = "ROLL_TOOL_LIMIT";
                continue;
            }

            // 计划起止时间从机台本班已消耗秒数推导，同机台任务按提交顺序连续排列。
            int beforeSeconds = working.getRemainingSecondsByMachine().getOrDefault(
                    trial.getMachineCode(), fullShiftSeconds(request));
            int afterSeconds = trial.getRemainingSeconds();
            int elapsedBefore = Math.max(0, fullShiftSeconds(request) - beforeSeconds);
            int occupiedSeconds = Math.max(0, beforeSeconds - afterSeconds);
            LocalDateTime expectedStart = request.getShiftStart().plusSeconds(elapsedBefore);
            int produceOrder = (int) working.getTasks().stream()
                    .filter(item -> trial.getMachineCode().equals(item.getMachineCode())).count() + 1;
            Cd90ShiftScheduleTask task = Cd90ShiftScheduleTask.builder()
                    .classField(request.getClassField()).clothCode(request.getClothCode())
                    .bigRollCode(request.getBigRollCode()).cordSpec(request.getCordSpec())
                    .machineCode(trial.getMachineCode())
                    .planQuantity(trial.getFinalSchedulableQuantity())
                    .vehicleCount(allocation.getRequiredVehicleCount())
                    .produceOrder(produceOrder).expectedStartTime(expectedStart)
                    .expectedEndTime(expectedStart.plusSeconds(occupiedSeconds))
                    .laneAllocations(allocation.getAllocations()).build();

            // 以下资源变更必须与任务一起提交，不能只扣减部分资源。
            working.setLanes(allocation.getLanes());
            working.setOccupiedToolingCount(working.getOccupiedToolingCount()
                    + allocation.getRequiredVehicleCount());
            working.getRemainingSecondsByMachine().put(trial.getMachineCode(), afterSeconds);
            working.getTailSpecByMachine().put(trial.getMachineCode(), request.getCordSpec());
            working.getTasks().add(task);
            log.info("[直裁自动排程] 当前班次资源提交成功, classField={}, clothCode={}, "
                            + "machineCode={}, planQuantity={}, vehicleCount={}, produceOrder={}",
                    request.getClassField(), request.getClothCode(), trial.getMachineCode(),
                    trial.getFinalSchedulableQuantity(), allocation.getRequiredVehicleCount(), produceOrder);
            return Cd90ShiftCommitResult.builder().success(true).state(working).task(task).build();
        }
        log.warn("[直裁自动排程] 当前班次资源提交失败, classField={}, clothCode={}, reason={}",
                request.getClassField(), request.getClothCode(), lastFailureReason);
        // 全部方案失败时明确返回原对象，调用方可安全继续处理下一个规格。
        return Cd90ShiftCommitResult.builder().success(false).failureReason(lastFailureReason)
                .state(originalState).build();
    }

    private int fullShiftSeconds(Cd90ShiftCommitRequest request) {
        return Math.toIntExact(ChronoUnit.SECONDS.between(request.getShiftStart(), request.getShiftEnd()));
    }

    private Cd90ShiftResourceState copy(Cd90ShiftResourceState source) {
        List<Cd90StorageLaneState> lanes = source.getLanes() == null ? new ArrayList<>()
                : source.getLanes().stream().map(item -> Cd90StorageLaneState.builder()
                        .laneCode(item.getLaneCode()).clothCode(item.getClothCode())
                        .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                        .build()).collect(Collectors.toList());
        return Cd90ShiftResourceState.builder().lanes(lanes)
                .totalToolingCount(source.getTotalToolingCount())
                .occupiedToolingCount(source.getOccupiedToolingCount())
                .remainingSecondsByMachine(copyMap(source.getRemainingSecondsByMachine()))
                .tailSpecByMachine(copyMap(source.getTailSpecByMachine()))
                .tasks(source.getTasks() == null ? new ArrayList<>() : new ArrayList<>(source.getTasks()))
                .build();
    }

    private <K, V> Map<K, V> copyMap(Map<K, V> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }
}
