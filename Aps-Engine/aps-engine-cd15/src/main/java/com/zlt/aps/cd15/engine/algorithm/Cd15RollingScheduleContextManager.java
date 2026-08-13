package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15InboundRecord;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15ResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理步骤14所需的多班累计状态，并为每个班次重新创建资源状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15RollingScheduleContextManager {

    private final Cd15ResourceSnapshotBuilder snapshotBuilder;

    /**
     * 使用6点库排快照初始化滚动上下文。
     *
     * @param storageLanesAtSix 6点库排快照
     * @return 新滚动上下文
     */
    public Cd15RollingScheduleContext initialize(List<Cd15StorageLaneState> storageLanesAtSix) {
        return this.initialize(storageLanesAtSix, new HashMap<>());
    }

    /**
     * 使用6点库排和首班新增规格证据初始化滚动上下文。
     *
     * @param storageLanesAtSix 6点库排快照
     * @param newSpecAdvanceInfoBySteelStrip 新增规格提前生产证据
     * @return 新滚动上下文
     */
    public Cd15RollingScheduleContext initialize(
            List<Cd15StorageLaneState> storageLanesAtSix,
            Map<String, Cd15NewSpecAdvanceInfo> newSpecAdvanceInfoBySteelStrip) {
        Map<String, Cd15NewSpecAdvanceInfo> copiedInfo =
                this.copyNewSpecAdvanceInfo(newSpecAdvanceInfoBySteelStrip);
        Map<String, BigDecimal> remainingBySteelStrip = copiedInfo.values().stream()
                .filter(item -> item.getSteelStripCode() != null)
                .filter(item -> item.getAdvanceDemandQuantity() != null
                        && item.getAdvanceDemandQuantity().signum() > 0)
                .collect(Collectors.toMap(Cd15NewSpecAdvanceInfo::getSteelStripCode,
                        Cd15NewSpecAdvanceInfo::getAdvanceDemandQuantity,
                        (first, second) -> first, HashMap::new));
        return Cd15RollingScheduleContext.builder()
                .storageLanesAtSix(this.copyLanes(storageLanesAtSix))
                .cumulativeConsumptionBySteelStrip(new HashMap<>())
                .actualInboundRecords(new ArrayList<>())
                .plannedInboundRecords(new ArrayList<>())
                .bigRollAgingStocks(new ArrayList<>())
                .committedTasks(new ArrayList<>())
                .continueDemandBySteelStrip(new HashMap<>())
                .pendingTasks(new ArrayList<>())
                .newSpecAdvanceInfoBySteelStrip(copiedInfo)
                .newSpecAdvanceRemainingBySteelStrip(remainingBySteelStrip)
                .normalizedNewSpecAdvanceSteelStripCodes(new HashSet<>())
                .equalSharePendingMaterialKeys(new HashSet<>())
                .singleSpecSplitMaterialKeys(new HashSet<>())
                .tailSpecByMachine(new HashMap<>())
                .lastMachineBySteelStrip(new HashMap<>())
                .tailByMachine(new HashMap<>())
                .build();
    }

    /**
     * 更新6点至下一班开始前的累计成型消耗量，按钢带代号分组保存。
     *
     * @param context 滚动上下文
     * @param cumulativeConsumptionBySteelStrip 按钢带代号汇总的累计消耗量
     */
    public void updateCumulativeConsumption(Cd15RollingScheduleContext context,
                                            Map<String, BigDecimal> cumulativeConsumptionBySteelStrip) {
        Map<String, BigDecimal> safeConsumption = cumulativeConsumptionBySteelStrip == null
                ? new HashMap<>() : cumulativeConsumptionBySteelStrip.entrySet().stream()
                        .filter(entry -> entry.getKey() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> entry.getValue() == null
                                        ? BigDecimal.ZERO : entry.getValue().max(BigDecimal.ZERO)));
        context.setCumulativeConsumptionBySteelStrip(safeConsumption);
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
    public Cd15ShiftResourceState openShift(Cd15RollingScheduleContext context,
                                            Cd15ShiftDescriptor shift,
                                            Map<String, BigDecimal> curlLengthBySteelStrip,
                                            BigDecimal fallbackCoilMeter,
                                            int totalToolingCount,
                                            List<String> machineCodes) {
        // 只允许预计入库时间不晚于班次开始的记录参与当前班资源重建，防止提前消费未来产量。
        List<Cd15InboundRecord> effectiveInbound = new ArrayList<>();
        effectiveInbound.addAll(beforeShift(context.getActualInboundRecords(), shift.getStartTime()));
        effectiveInbound.addAll(beforeShift(context.getPlannedInboundRecords(), shift.getStartTime()));
        // 每班都从6点基线重算：基线库排 - 累计消耗 + 当前班前有效入库。
        Cd15ResourceSnapshot snapshot = snapshotBuilder.build(
                context.getStorageLanesAtSix(), context.getCumulativeConsumptionBySteelStrip(),
                curlLengthBySteelStrip, fallbackCoilMeter, effectiveInbound);

        // 机台产能属于班次资源，每班从完整班次秒数重新初始化。
        Map<String, Integer> remainingSeconds = new HashMap<>();
        if (machineCodes != null) {
            machineCodes.forEach(code -> remainingSeconds.put(code, shift.getDurationSeconds()));
        }
        log.info("[斜裁自动排程] 当前班次滚动资源已重建, classField={}, shiftCode={}, "
                        + "effectiveInboundCount={}, releasedVehicleCount={}, occupiedVehicleCount={}, consumptionRemainder={}, cumulativeConsumptionBySteelStrip={}",
                shift.getClassField(), shift.getShiftCode(), effectiveInbound.size(),
                snapshot.getReleasedVehicleCount(), snapshot.getOccupiedVehicleCount(),
                snapshot.getConsumptionRemainderQuantity(), context.getCumulativeConsumptionBySteelStrip());
        return Cd15ShiftResourceState.builder()
                .lanes(snapshot.getLanes()).totalToolingCount(totalToolingCount)
                .occupiedToolingCount(snapshot.getOccupiedVehicleCount())
                .remainingSecondsByMachine(remainingSeconds)
                .tailSpecByMachine(new HashMap<>(context.getTailSpecByMachine()))
                .tailByMachine(copyTails(context.getTailByMachine()))
                .tasks(new ArrayList<>()).build();
    }

    /**
     * 完成当前班次，将任务、机尾规格和库排计划入库写入滚动上下文。
     *
     * @param context 滚动上下文
     * @param completedState 当前班最终资源状态
     */
    public void completeShift(Cd15RollingScheduleContext context,
                              Cd15ShiftResourceState completedState) {
        if (completedState == null || completedState.getTasks() == null) {
            return;
        }
        if (context.getLastMachineBySteelStrip() == null) {
            context.setLastMachineBySteelStrip(new HashMap<>());
        }
        int taskOffset = context.getCommittedTasks().size();
        for (int index = 0; index < completedState.getTasks().size(); index++) {
            Cd15ShiftScheduleTask task = completedState.getTasks().get(index);
            context.getCommittedTasks().add(task);
            if (task.getMaterialKey() != null && task.getMachineCode() != null) {
                context.getLastMachineBySteelStrip().put(task.getMaterialKey(), task.getMachineCode());
            }
            // 当前班计划任务按预计结束时间转为计划入库，供后续班次净需求和库排重建使用。
            appendPlannedInbound(context, task, taskOffset + index + 1);
        }
        // 机尾规格跨班继承，用于下一班判断是否发生规格切换和相应耗时。
        context.setBigRollAgingStocks(copyAgingStocks(completedState.getBigRollAgingStocks()));
        context.setTailSpecByMachine(completedState.getTailSpecByMachine() == null
                ? new HashMap<>() : new HashMap<>(completedState.getTailSpecByMachine()));
        context.setTailByMachine(copyTails(completedState.getTailByMachine()));
        log.info("[斜裁自动排程] 当前班次滚动上下文已保存, taskCount={}, "
                        + "plannedInboundCount={}, machineTailCount={}, lastMachineBySteelStrip={}",
                completedState.getTasks().size(), context.getPlannedInboundRecords().size(),
                context.getTailSpecByMachine().size(), context.getLastMachineBySteelStrip());
    }

    private void appendPlannedInbound(Cd15RollingScheduleContext context,
                                      Cd15ShiftScheduleTask task, int taskSequence) {
        if (task.getLaneAllocations() == null) {
            return;
        }
        String taskKey = task.getClassField() + "-" + task.getMachineCode() + "-" + taskSequence;
        int totalVehicles = task.getLaneAllocations().stream()
                .mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        BigDecimal remainingQuantity = task.getPlanQuantity() == null
                ? BigDecimal.ZERO : task.getPlanQuantity();
        for (int index = 0; index < task.getLaneAllocations().size(); index++) {
            Cd15StorageLaneAllocation allocation = task.getLaneAllocations().get(index);
            // 前序库排按车数比例分摊，最后一条承接小数舍入余量，保证分配量之和等于任务量。
            BigDecimal allocationQuantity = index == task.getLaneAllocations().size() - 1
                    ? remainingQuantity : proportionalQuantity(
                            task.getPlanQuantity(), allocation.getVehicleCount(), totalVehicles);
            remainingQuantity = remainingQuantity.subtract(allocationQuantity);
            context.getPlannedInboundRecords().add(Cd15InboundRecord.builder()
                    .taskKey(taskKey).actual(false).steelStripCode(task.getSteelStripCode())
                    .laneCode(allocation.getLaneCode()).machineCode(task.getMachineCode())
                    .vehicleCount(allocation.getVehicleCount())
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

    private List<Cd15InboundRecord> beforeShift(List<Cd15InboundRecord> records,
                                                LocalDateTime shiftStart) {
        if (records == null) {
            return new ArrayList<>();
        }
        return records.stream()
                .filter(record -> record.getInboundTime() == null
                        || !record.getInboundTime().isAfter(shiftStart))
                .collect(Collectors.toList());
    }

    /**
     * 将前序班次已分配米数恢复到本班重新加载的GDYY成熟流水。
     *
     * @param context 滚动上下文
     * @param currentStocks 当前班重新加载的成熟流水
     * @return 带跨班已分配水位的成熟流水副本
     */
    public List<Cd15BigRollAgingStock> restoreBigRollAllocations(
            Cd15RollingScheduleContext context, List<Cd15BigRollAgingStock> currentStocks) {
        Map<String, BigDecimal> allocatedBySource = copyAgingStocks(
                context == null ? null : context.getBigRollAgingStocks()).stream()
                .collect(Collectors.toMap(this::agingSourceKey,
                        this::allocatedQuantity, BigDecimal::max));
        return copyAgingStocks(currentStocks).stream()
                .peek(item -> item.setAllocatedQuantity(allocatedQuantity(item)
                        .max(allocatedBySource.getOrDefault(agingSourceKey(item), BigDecimal.ZERO))))
                .collect(Collectors.toList());
    }

    private List<Cd15BigRollAgingStock> copyAgingStocks(List<Cd15BigRollAgingStock> stocks) {
        if (stocks == null) {
            return new ArrayList<>();
        }
        return stocks.stream().map(item -> Cd15BigRollAgingStock.builder()
                .sourceType(item.getSourceType()).sourceId(item.getSourceId())
                .bigRollCode(item.getBigRollCode()).bigRollBarcode(item.getBigRollBarcode())
                .availableQuantity(item.getAvailableQuantity())
                .allocatedQuantity(item.getAllocatedQuantity())
                .stockInTime(item.getStockInTime()).releaseTime(item.getReleaseTime())
                .build()).collect(Collectors.toList());
    }

    private BigDecimal allocatedQuantity(Cd15BigRollAgingStock stock) {
        return stock.getAllocatedQuantity() == null ? BigDecimal.ZERO : stock.getAllocatedQuantity();
    }

    private String agingSourceKey(Cd15BigRollAgingStock stock) {
        return (stock.getSourceType() == null ? "" : stock.getSourceType()) + "#"
                + (stock.getSourceId() == null ? "" : stock.getSourceId());
    }

    private List<Cd15StorageLaneState> copyLanes(List<Cd15StorageLaneState> lanes) {
        if (lanes == null) {
            return new ArrayList<>();
        }
        return lanes.stream().map(item -> Cd15StorageLaneState.builder()
                .laneCode(item.getLaneCode()).machineCode(item.getMachineCode())
                .steelStripCode(item.getSteelStripCode())
                .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                .build()).collect(Collectors.toList());
    }

    /** 深拷新增规格提前生产证据，避免跨班共享可变集合。 */
    private Map<String, Cd15NewSpecAdvanceInfo> copyNewSpecAdvanceInfo(
            Map<String, Cd15NewSpecAdvanceInfo> source) {
        Map<String, Cd15NewSpecAdvanceInfo> result = new HashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((steelStripCode, info) -> {
            if (info != null) {
                result.put(steelStripCode, Cd15NewSpecAdvanceInfo.builder()
                        .steelStripCode(info.getSteelStripCode())
                        .historyStartDate(info.getHistoryStartDate())
                        .historyEndDate(info.getHistoryEndDate())
                        .sourceDemandDates(info.getSourceDemandDates() == null
                                ? new ArrayList<>() : new ArrayList<>(info.getSourceDemandDates()))
                        .sourceDemandKeys(info.getSourceDemandKeys() == null
                                ? new ArrayList<>() : new ArrayList<>(info.getSourceDemandKeys()))
                        .advanceDemandQuantity(info.getAdvanceDemandQuantity())
                        .targetProductionDate(info.getTargetProductionDate())
                        .analysis(info.getAnalysis())
                        .build());
            }
        });
        return result;
    }
    /** 复制机台链尾状态，避免跨班共享可变对象。 */
    private Map<String, com.zlt.aps.cd15.engine.model.Cd15MachineTailState> copyTails(
            Map<String, com.zlt.aps.cd15.engine.model.Cd15MachineTailState> source) {
        Map<String, com.zlt.aps.cd15.engine.model.Cd15MachineTailState> result = new HashMap<>();
        if (source != null) {
            source.forEach((machineCode, tail) -> result.put(machineCode, tail == null ? null
                    : com.zlt.aps.cd15.engine.model.Cd15MachineTailState.builder()
                            .materialKey(tail.getMaterialKey())
                            .steelStripCode(tail.getSteelStripCode())
                            .bigRollCode(tail.getBigRollCode())
                            .cuttingAngle(tail.getCuttingAngle())
                            .build()));
        }
        return result;
    }
}
