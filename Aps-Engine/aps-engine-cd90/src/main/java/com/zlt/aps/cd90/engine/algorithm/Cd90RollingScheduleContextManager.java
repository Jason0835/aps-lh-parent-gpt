package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import com.zlt.aps.cd90.engine.model.Cd90ResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理步骤14所需的多班累计状态，并为每个班次重新创建资源状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90RollingScheduleContextManager {

    private final Cd90ResourceSnapshotBuilder snapshotBuilder;

    /**
     * 使用6点库排快照初始化滚动上下文。
     *
     * @param storageLanesAtSix 6点库排快照
     * @return 新滚动上下文
     */
    public Cd90RollingScheduleContext initialize(List<Cd90StorageLaneState> storageLanesAtSix) {
        return Cd90RollingScheduleContext.builder()
                .storageLanesAtSix(copyLanes(storageLanesAtSix))
                .cumulativeConsumption(BigDecimal.ZERO)
                .actualInboundRecords(new ArrayList<>())
                .plannedInboundRecords(new ArrayList<>())
                .committedTasks(new ArrayList<>())
                .tailSpecByMachine(new HashMap<>())
                .build();
    }

    /**
     * 更新6点至下一班开始前的累计成型消耗量。
     *
     * @param context 滚动上下文
     * @param cumulativeConsumption 累计消耗量
     */
    public void updateCumulativeConsumption(Cd90RollingScheduleContext context,
                                            BigDecimal cumulativeConsumption) {
        context.setCumulativeConsumption(cumulativeConsumption == null
                ? BigDecimal.ZERO : cumulativeConsumption.max(BigDecimal.ZERO));
    }

    /**
     * 从6点原始快照、累计消耗和班次前有效入库重建当前班资源。
     *
     * @param context 滚动上下文
     * @param shift 当前班次
     * @param coilMeter 一车卷曲米数
     * @param totalToolingCount 工装总数
     * @param machineCodes 当前班启用机台编码
     * @return 当前班独立资源状态
     */
    public Cd90ShiftResourceState openShift(Cd90RollingScheduleContext context,
                                            Cd90ShiftDescriptor shift,
                                            BigDecimal coilMeter,
                                            int totalToolingCount,
                                            List<String> machineCodes) {
        List<Cd90InboundRecord> effectiveInbound = new ArrayList<>();
        effectiveInbound.addAll(beforeShift(context.getActualInboundRecords(), shift.getStartTime()));
        effectiveInbound.addAll(beforeShift(context.getPlannedInboundRecords(), shift.getStartTime()));
        Cd90ResourceSnapshot snapshot = snapshotBuilder.build(
                context.getStorageLanesAtSix(), context.getCumulativeConsumption(),
                coilMeter, effectiveInbound);

        Map<String, Integer> remainingSeconds = new HashMap<>();
        if (machineCodes != null) {
            machineCodes.forEach(code -> remainingSeconds.put(code, shift.getDurationSeconds()));
        }
        log.info("[直裁自动排程] 当前班次滚动资源已重建, classField={}, shiftCode={}, "
                        + "effectiveInboundCount={}, occupiedVehicleCount={}, cumulativeConsumption={}",
                shift.getClassField(), shift.getShiftCode(), effectiveInbound.size(),
                snapshot.getOccupiedVehicleCount(), context.getCumulativeConsumption());
        return Cd90ShiftResourceState.builder()
                .lanes(snapshot.getLanes()).totalToolingCount(totalToolingCount)
                .occupiedToolingCount(snapshot.getOccupiedVehicleCount())
                .remainingSecondsByMachine(remainingSeconds)
                .tailSpecByMachine(new HashMap<>(context.getTailSpecByMachine()))
                .tasks(new ArrayList<>()).build();
    }

    /**
     * 完成当前班次，将任务、机尾规格和库排计划入库写入滚动上下文。
     *
     * @param context 滚动上下文
     * @param completedState 当前班最终资源状态
     */
    public void completeShift(Cd90RollingScheduleContext context,
                              Cd90ShiftResourceState completedState) {
        if (completedState == null || completedState.getTasks() == null) {
            return;
        }
        int taskOffset = context.getCommittedTasks().size();
        for (int index = 0; index < completedState.getTasks().size(); index++) {
            Cd90ShiftScheduleTask task = completedState.getTasks().get(index);
            context.getCommittedTasks().add(task);
            appendPlannedInbound(context, task, taskOffset + index + 1);
        }
        context.setTailSpecByMachine(completedState.getTailSpecByMachine() == null
                ? new HashMap<>() : new HashMap<>(completedState.getTailSpecByMachine()));
        log.info("[直裁自动排程] 当前班次滚动上下文已保存, taskCount={}, "
                        + "plannedInboundCount={}, machineTailCount={}",
                completedState.getTasks().size(), context.getPlannedInboundRecords().size(),
                context.getTailSpecByMachine().size());
    }

    private void appendPlannedInbound(Cd90RollingScheduleContext context,
                                      Cd90ShiftScheduleTask task, int taskSequence) {
        if (task.getLaneAllocations() == null) {
            return;
        }
        String taskKey = task.getClassField() + "-" + task.getMachineCode() + "-" + taskSequence;
        int totalVehicles = task.getLaneAllocations().stream()
                .mapToInt(Cd90StorageLaneAllocation::getVehicleCount).sum();
        BigDecimal remainingQuantity = task.getPlanQuantity() == null
                ? BigDecimal.ZERO : task.getPlanQuantity();
        for (int index = 0; index < task.getLaneAllocations().size(); index++) {
            Cd90StorageLaneAllocation allocation = task.getLaneAllocations().get(index);
            BigDecimal allocationQuantity = index == task.getLaneAllocations().size() - 1
                    ? remainingQuantity : proportionalQuantity(
                            task.getPlanQuantity(), allocation.getVehicleCount(), totalVehicles);
            remainingQuantity = remainingQuantity.subtract(allocationQuantity);
            context.getPlannedInboundRecords().add(Cd90InboundRecord.builder()
                    .taskKey(taskKey).actual(false).clothCode(task.getClothCode())
                    .laneCode(allocation.getLaneCode()).vehicleCount(allocation.getVehicleCount())
                    .inboundQuantity(allocationQuantity)
                    .inboundTime(task.getExpectedEndTime()).build());
        }
    }

    /**
     * 多库排拆分时按车数比例分配精确计划量，最后一个库排承接舍入余量。
     */
    private BigDecimal proportionalQuantity(BigDecimal totalQuantity,
                                            int allocationVehicles,
                                            int totalVehicles) {
        if (totalQuantity == null || totalVehicles <= 0 || allocationVehicles <= 0) {
            return BigDecimal.ZERO;
        }
        return totalQuantity.multiply(BigDecimal.valueOf(allocationVehicles))
                .divide(BigDecimal.valueOf(totalVehicles), 10, RoundingMode.HALF_UP);
    }

    private List<Cd90InboundRecord> beforeShift(List<Cd90InboundRecord> records,
                                                LocalDateTime shiftStart) {
        if (records == null) {
            return new ArrayList<>();
        }
        return records.stream()
                .filter(record -> record.getInboundTime() == null
                        || !record.getInboundTime().isAfter(shiftStart))
                .collect(Collectors.toList());
    }

    private List<Cd90StorageLaneState> copyLanes(List<Cd90StorageLaneState> lanes) {
        if (lanes == null) {
            return new ArrayList<>();
        }
        return lanes.stream().map(item -> Cd90StorageLaneState.builder()
                .laneCode(item.getLaneCode()).clothCode(item.getClothCode())
                .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                .build()).collect(Collectors.toList());
    }
}
