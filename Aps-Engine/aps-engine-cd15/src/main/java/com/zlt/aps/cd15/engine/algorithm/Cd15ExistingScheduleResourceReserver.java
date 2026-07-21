package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.constant.Cd15CutMode;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15MachineCapacityTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15MachineTailState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.service.Cd15MachineResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 恢复当前班锁定排程段对机台、库排和工装的真实资源占用。 */
@Component
@RequiredArgsConstructor
public class Cd15ExistingScheduleResourceReserver {

    private final Cd15MachineResourceService machineResourceService;
    private final Cd15MachineCapacityCalculator capacityCalculator;
    private final Cd15MachineModeResolver machineModeResolver;
    private final Cd15BigRollMeterCalculator bigRollMeterCalculator;
    private final Cd15BigRollAgingAllocator bigRollAgingAllocator;

    /** 将锁定任务按原机台和原顺序写入当前班资源快照。 */
    public void reserve(Cd15AutoScheduleContext context,
                        Cd15ShiftDescriptor shift,
                        Cd15ShiftResourceState state,
                        List<Cd15ScheduleResult> lockedResults,
                        Map<Long, List<Cd15ScheduleLaneAllocation>> sourceLanes,
                        Cd15AutoScheduleInput input) {
        if (lockedResults == null || lockedResults.isEmpty()) {
            return;
        }
        validate(context, shift, state);
        Cd15MachineResourceSnapshot snapshot = machineResourceService.load(
                context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
        Map<String, Cd15MachineResource> machineByCode = safeMachines(snapshot).stream()
                .filter(item -> item.getMachineCode() != null)
                .collect(Collectors.toMap(Cd15MachineResource::getMachineCode,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<Cd15ScheduleLaneAllocation>> lanesByResult = sourceLanes == null
                ? Collections.emptyMap() : sourceLanes;
        safeMachines(snapshot).stream()
                .filter(item -> item.getMachineCode() != null)
                .forEach(machine -> state.getRemainingSecondsByMachine().putIfAbsent(
                        machine.getMachineCode(), Math.max(0,
                                shift.getDurationSeconds()
                                        - machine.getMaintenanceSeconds())));
        List<Cd15ScheduleResult> orderedResults = lockedResults.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Cd15ScheduleResult::getMachineCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(item -> readOrder(item, shift.getClassField()),
                                Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        java.util.Set<String> processedGroups = new java.util.HashSet<>();
        orderedResults.forEach(result -> {
            if (!Cd15CutMode.SPLIT.equals(this.cutMode(result))) {
                this.reserveOne(context, shift, state, result,
                        machineByCode, lanesByResult, input);
                return;
            }
            String groupKey = result.getGroupNo();
            if (groupKey == null || groupKey.trim().isEmpty()) {
                throw new IllegalStateException("锁定分裁任务缺少组号");
            }
            if (!processedGroups.add(groupKey)) {
                return;
            }
            List<Cd15ScheduleResult> splitResults = orderedResults.stream()
                    .filter(item -> groupKey.equals(item.getGroupNo()))
                    .collect(Collectors.toList());
            this.reserveSplit(context, shift, state, splitResults,
                    machineByCode, lanesByResult, input);
        });
    }

    private void reserveOne(Cd15AutoScheduleContext context,
                            Cd15ShiftDescriptor shift,
                            Cd15ShiftResourceState state,
                            Cd15ScheduleResult result,
                            Map<String, Cd15MachineResource> machineByCode,
                            Map<Long, List<Cd15ScheduleLaneAllocation>> lanesByResult,
                            Cd15AutoScheduleInput input) {
        BigDecimal quantity = readPlan(result, shift.getClassField());
        if (quantity.signum() <= 0) {
            return;
        }
        this.validateMaterial(result);
        Cd15MachineResource machine = machineByCode.get(result.getMachineCode());
        if (machine == null || !machineModeResolver.matches(machine, false)) {
            throw new IllegalStateException("锁定任务原机台不可用: " + result.getMachineCode());
        }
        BigDecimal shiftCapacity = machineModeResolver.capacity(machine, false);
        if (shiftCapacity == null || shiftCapacity.signum() <= 0) {
            throw new IllegalStateException("锁定任务原机台单裁能力未维护: " + result.getMachineCode());
        }
        int remainingSeconds = state.getRemainingSecondsByMachine()
                .getOrDefault(result.getMachineCode(), 0);
        Cd15MachineTailState currentTail = Cd15MachineTailState.builder()
                .materialKey(result.getMaterialKey())
                .steelStripCode(result.getSteelStripCode())
                .bigRollCode(result.getBigRollCode())
                .cuttingAngle(result.getCuttingAngle()).build();
        BigDecimal bigRollConsume = this.bigRollConsume(result, quantity);
        Cd15BigRollAgingAllocation agingAllocation = this.reserveBigRoll(
                state, result, bigRollConsume, shift, remainingSeconds, input);
        int availableSeconds = Math.max(0,
                remainingSeconds - agingAllocation.getDelaySeconds());
        Cd15MachineCapacityTrial trial = capacityCalculator.calculateWithRemainingSeconds(
                shiftCapacity, Math.max(1, shift.getDurationSeconds() / 3600),
                availableSeconds, state.getTailByMachine().get(result.getMachineCode()),
                currentTail, context.getParameters().getSameRollDiffSpecChangeMinutes(),
                context.getParameters().getDiffRollSameSpecChangeMinutes(),
                context.getParameters().getDiffRollDiffSpecChangeMinutes(), quantity);
        if (!trial.isFullyAccommodated()) {
            throw new IllegalStateException("锁定任务超过原机台剩余产能: " + result.getMachineCode());
        }
        List<Cd15StorageLaneAllocation> allocations = reserveLanes(
                shift, state, result, lanesByResult.getOrDefault(
                        result.getId(), Collections.emptyList()));
        int vehicleCount = allocations.stream()
                .mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        if (state.getOccupiedToolingCount() + vehicleCount > state.getTotalToolingCount()) {
            throw new IllegalStateException("锁定任务占用工装超过当前可用数量");
        }
        state.setOccupiedToolingCount(state.getOccupiedToolingCount() + vehicleCount);
        state.getRemainingSecondsByMachine().put(
                result.getMachineCode(), trial.getRemainingSeconds());
        state.getTailByMachine().put(result.getMachineCode(), currentTail);
        state.getTailSpecByMachine().put(result.getMachineCode(), result.getSteelStripCode());
        LocalDateTime expectedStart = agingAllocation.getTaskStartTime();
        LocalDateTime expectedEnd = expectedStart.plusSeconds(
                trial.getChangeSeconds() + trial.getProductionSeconds());
        state.getTasks().add(Cd15ShiftScheduleTask.builder()
                .classField(shift.getClassField())
                .sourceTaskKey(result.getId() + ":" + shift.getClassField())
                .sourceResultId(result.getId())
                .materialKey(result.getMaterialKey())
                .steelStripCode(result.getSteelStripCode())
                .bigRollCode(result.getBigRollCode())
                .cuttingAngle(result.getCuttingAngle())
                .craftWidth(result.getCraftWidth())
                .unitConsumeMillimeter(result.getUnitConsumeMillimeter())
                .cordWidth(result.getCordWidth()).curlLength(result.getCurlLength())
                .bigRollConsumeQuantity(bigRollConsume)
                .cutMode(Cd15CutMode.SINGLE)
                .cordSpec(result.getSteelStripCode()).machineCode(result.getMachineCode())
                .planQuantity(quantity).vehicleCount(vehicleCount)
                .produceOrder(defaultOrder(readOrder(result, shift.getClassField())))
                .expectedStartTime(expectedStart).expectedEndTime(expectedEnd)
                .laneAllocations(allocations).build());
    }

    /** 锁定分裁组按一次机台作业恢复，两条库排和大卷占用一起提交。 */
    private void reserveSplit(
            Cd15AutoScheduleContext context,
            Cd15ShiftDescriptor shift,
            Cd15ShiftResourceState state,
            List<Cd15ScheduleResult> splitResults,
            Map<String, Cd15MachineResource> machineByCode,
            Map<Long, List<Cd15ScheduleLaneAllocation>> lanesByResult,
            Cd15AutoScheduleInput input) {
        if (splitResults.size() != 2) {
            throw new IllegalStateException("锁定分裁组必须包含两条排程结果");
        }
        Cd15ScheduleResult first = splitResults.get(0);
        Cd15ScheduleResult second = splitResults.get(1);
        BigDecimal firstQuantity = this.readPlan(first, shift.getClassField());
        BigDecimal secondQuantity = this.readPlan(second, shift.getClassField());
        if (firstQuantity.signum() <= 0 || secondQuantity.signum() <= 0
                || Objects.equals(first.getSteelStripCode(), second.getSteelStripCode())
                || !Objects.equals(first.getMachineCode(), second.getMachineCode())
                || !Objects.equals(first.getBigRollCode(), second.getBigRollCode())
                || !Objects.equals(first.getCuttingAngle(), second.getCuttingAngle())) {
            throw new IllegalStateException("锁定分裁组必须是同机台、同大卷、同角度的两条不同钢带计划");
        }
        Integer firstOrder = this.readOrder(first, shift.getClassField());
        Integer secondOrder = this.readOrder(second, shift.getClassField());
        if (firstOrder == null || firstOrder <= 0
                || !Objects.equals(firstOrder, secondOrder)) {
            throw new IllegalStateException("锁定分裁组两条结果必须共用生产顺序");
        }
        this.validateMaterial(first);
        this.validateMaterial(second);
        Cd15MachineResource machine = machineByCode.get(first.getMachineCode());
        if (machine == null || !machineModeResolver.matches(machine, true)) {
            throw new IllegalStateException("锁定分裁组原机台当前模式不可用: "
                    + first.getMachineCode());
        }
        BigDecimal firstShiftCapacity = machineModeResolver.capacity(machine, true);
        BigDecimal secondShiftCapacity = machineModeResolver.capacity(machine, true);
        if (firstShiftCapacity == null || firstShiftCapacity.signum() <= 0
                || secondShiftCapacity == null || secondShiftCapacity.signum() <= 0) {
            throw new IllegalStateException("锁定分裁组原机台分裁能力未维护");
        }
        int remainingSeconds = state.getRemainingSecondsByMachine()
                .getOrDefault(first.getMachineCode(), 0);
        BigDecimal firstConsume = this.bigRollConsume(first, firstQuantity);
        BigDecimal secondConsume = this.bigRollConsume(second, secondQuantity);
        Cd15BigRollAgingAllocation agingAllocation = this.reserveBigRoll(
                state, first, firstConsume.add(secondConsume), shift,
                remainingSeconds, input);
        int availableSeconds = Math.max(0,
                remainingSeconds - agingAllocation.getDelaySeconds());
        Cd15MachineTailState splitTail = Cd15MachineTailState.builder()
                .materialKey(first.getGroupNo())
                .steelStripCode(first.getSteelStripCode()
                        + "+" + second.getSteelStripCode())
                .bigRollCode(first.getBigRollCode())
                .cuttingAngle(first.getCuttingAngle()).build();
        Cd15MachineTailState previousTail = state.getTailByMachine()
                .get(first.getMachineCode());
        int shiftHours = Math.max(1, shift.getDurationSeconds() / 3600);
        Cd15MachineCapacityTrial firstTrial = capacityCalculator
                .calculateWithRemainingSeconds(firstShiftCapacity, shiftHours,
                        availableSeconds, previousTail, splitTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        firstQuantity);
        Cd15MachineCapacityTrial secondTrial = capacityCalculator
                .calculateWithRemainingSeconds(secondShiftCapacity, shiftHours,
                        availableSeconds, previousTail, splitTail,
                        context.getParameters().getSameRollDiffSpecChangeMinutes(),
                        context.getParameters().getDiffRollSameSpecChangeMinutes(),
                        context.getParameters().getDiffRollDiffSpecChangeMinutes(),
                        secondQuantity);
        int changeSeconds = Math.max(firstTrial.getChangeSeconds(),
                secondTrial.getChangeSeconds());
        int productionSeconds = Math.max(firstTrial.getProductionSeconds(),
                secondTrial.getProductionSeconds());
        if (!firstTrial.isFullyAccommodated() || !secondTrial.isFullyAccommodated()
                || changeSeconds + productionSeconds > availableSeconds) {
            throw new IllegalStateException("锁定分裁组超过原机台剩余产能");
        }
        List<Cd15StorageLaneAllocation> firstAllocations = this.reserveLanes(
                shift, state, first, lanesByResult.getOrDefault(
                        first.getId(), Collections.emptyList()));
        List<Cd15StorageLaneAllocation> secondAllocations = this.reserveLanes(
                shift, state, second, lanesByResult.getOrDefault(
                        second.getId(), Collections.emptyList()));
        int firstVehicles = firstAllocations.stream()
                .mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        int secondVehicles = secondAllocations.stream()
                .mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        if (state.getOccupiedToolingCount() + firstVehicles + secondVehicles
                > state.getTotalToolingCount()) {
            throw new IllegalStateException("锁定分裁组占用工装超过当前可用数量");
        }
        state.setOccupiedToolingCount(state.getOccupiedToolingCount()
                + firstVehicles + secondVehicles);
        int afterSeconds = availableSeconds - changeSeconds - productionSeconds;
        state.getRemainingSecondsByMachine().put(first.getMachineCode(), afterSeconds);
        state.getTailByMachine().put(first.getMachineCode(), splitTail);
        state.getTailSpecByMachine().put(first.getMachineCode(),
                first.getSteelStripCode() + "+" + second.getSteelStripCode());
        LocalDateTime expectedStart = agingAllocation.getTaskStartTime();
        LocalDateTime expectedEnd = expectedStart.plusSeconds(
                changeSeconds + productionSeconds);
        state.getTasks().add(this.lockedSplitTask(first, shift, firstQuantity,
                firstConsume, firstVehicles, firstAllocations,
                firstOrder, expectedStart, expectedEnd));
        state.getTasks().add(this.lockedSplitTask(second, shift, secondQuantity,
                secondConsume, secondVehicles, secondAllocations,
                firstOrder, expectedStart, expectedEnd));
    }

    private Cd15ShiftScheduleTask lockedSplitTask(
            Cd15ScheduleResult result,
            Cd15ShiftDescriptor shift,
            BigDecimal quantity,
            BigDecimal bigRollConsume,
            int vehicleCount,
            List<Cd15StorageLaneAllocation> allocations,
            int produceOrder,
            LocalDateTime expectedStart,
            LocalDateTime expectedEnd) {
        return Cd15ShiftScheduleTask.builder()
                .classField(shift.getClassField())
                .sourceTaskKey(result.getId() + ":" + shift.getClassField())
                .sourceResultId(result.getId()).materialKey(result.getMaterialKey())
                .steelStripCode(result.getSteelStripCode())
                .bigRollCode(result.getBigRollCode())
                .cuttingAngle(result.getCuttingAngle())
                .craftWidth(result.getCraftWidth())
                .unitConsumeMillimeter(result.getUnitConsumeMillimeter())
                .cordWidth(result.getCordWidth()).curlLength(result.getCurlLength())
                .bigRollConsumeQuantity(bigRollConsume)
                .cutMode(Cd15CutMode.SPLIT).splitGroupKey(result.getGroupNo())
                .cordSpec(result.getSteelStripCode()).machineCode(result.getMachineCode())
                .planQuantity(quantity).vehicleCount(vehicleCount)
                .produceOrder(produceOrder).expectedStartTime(expectedStart)
                .expectedEndTime(expectedEnd).laneAllocations(allocations).build();
    }

    private List<Cd15StorageLaneAllocation> reserveLanes(
            Cd15ShiftDescriptor shift, Cd15ShiftResourceState state,
            Cd15ScheduleResult result, List<Cd15ScheduleLaneAllocation> sourceLanes) {
        List<Cd15ScheduleLaneAllocation> rows = sourceLanes.stream()
                .filter(item -> shift.getClassField().equals(item.getClassField()))
                .sorted(Comparator.comparing(Cd15ScheduleLaneAllocation::getAllocationOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            throw new IllegalStateException("锁定任务缺少原库排明细: " + result.getId());
        }
        List<Cd15StorageLaneAllocation> allocations = new ArrayList<>();
        rows.forEach(row -> {
            Cd15StorageLaneState lane = state.getLanes().stream()
                    .filter(item -> Objects.equals(row.getStorageLaneCode(), item.getLaneCode()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "锁定任务原库排不存在: " + row.getStorageLaneCode()));
            int vehicles = row.getAllocatedCartCount() == null
                    ? 0 : row.getAllocatedCartCount();
            if (vehicles <= 0 || lane.getVehicleCount() + vehicles > lane.getMaxVehicleCount()) {
                throw new IllegalStateException("锁定任务原库排容量不足: " + row.getStorageLaneCode());
            }
            if (lane.getVehicleCount() > 0
                    && !Objects.equals(result.getSteelStripCode(), lane.getSteelStripCode())) {
                throw new IllegalStateException("锁定任务原库排已被其他钢带占用: "
                        + row.getStorageLaneCode());
            }
            lane.setSteelStripCode(result.getSteelStripCode());
            lane.setVehicleCount(lane.getVehicleCount() + vehicles);
            allocations.add(Cd15StorageLaneAllocation.builder()
                    .laneCode(row.getStorageLaneCode()).vehicleCount(vehicles).build());
        });
        return allocations;
    }

    private String cutMode(Cd15ScheduleResult result) {
        String mode = result == null || result.getCutMode() == null
                ? "" : result.getCutMode().trim().toUpperCase();
        if (!Cd15CutMode.SINGLE.equals(mode)
                && !Cd15CutMode.SPLIT.equals(mode)) {
            throw new IllegalStateException("锁定排程结果裁断模式必须为SINGLE或SPLIT");
        }
        return mode;
    }

    private void validateMaterial(Cd15ScheduleResult result) {
        if (result == null || result.getMaterialKey() == null
                || result.getMaterialKey().trim().isEmpty()
                || result.getBigRollCode() == null
                || result.getBigRollCode().trim().isEmpty()
                || result.getCuttingAngle() == null
                || result.getCuttingAngle().trim().isEmpty()
                || result.getCraftWidth() == null
                || result.getCraftWidth().signum() <= 0
                || result.getUnitConsumeMillimeter() == null
                || result.getUnitConsumeMillimeter().signum() <= 0
                || result.getCordWidth() == null
                || result.getCordWidth().signum() <= 0) {
            throw new IllegalStateException("锁定排程结果缺少完整施工尺寸: "
                    + (result == null ? null : result.getId()));
        }
    }

    private BigDecimal bigRollConsume(Cd15ScheduleResult result,
                                      BigDecimal quantity) {
        return bigRollMeterCalculator.calculateForPlanQuantity(
                quantity, result.getUnitConsumeMillimeter(),
                result.getCraftWidth(), result.getCordWidth());
    }

    private Cd15BigRollAgingAllocation reserveBigRoll(
            Cd15ShiftResourceState state,
            Cd15ScheduleResult result,
            BigDecimal consumption,
            Cd15ShiftDescriptor shift,
            int remainingSeconds,
            Cd15AutoScheduleInput input) {
        if (input != null && input.getBigRollAgingDataMissingCodes() != null
                && input.getBigRollAgingDataMissingCodes().contains(
                        result.getBigRollCode())) {
            throw new IllegalStateException("锁定任务GDYY大卷资料缺失: "
                    + result.getBigRollCode());
        }
        if (state.getBigRollAgingStocks() == null
                || state.getBigRollAgingStocks().isEmpty()) {
            throw new IllegalStateException("锁定任务没有可用GDYY成熟大卷: "
                    + result.getBigRollCode());
        }
        int fullSeconds = Math.max(1, shift.getDurationSeconds());
        LocalDateTime originalStart = shift.getStartTime().plusSeconds(
                Math.max(0, fullSeconds - remainingSeconds));
        Cd15BigRollAgingAllocation allocation = bigRollAgingAllocator.allocate(
                state.getBigRollAgingStocks(), result.getBigRollCode(),
                consumption, originalStart);
        if (!allocation.isSuccess()) {
            throw new IllegalStateException("锁定任务GDYY大卷库存或成熟时间不足: "
                    + result.getBigRollCode());
        }
        return allocation;
    }

    private BigDecimal readPlan(Cd15ScheduleResult result, String classField) {
        Object value = wrapper(result).getPropertyValue(property(classField, "PlanQty"));
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private Integer readOrder(Cd15ScheduleResult result, String classField) {
        Object value = wrapper(result).getPropertyValue(property(classField, "ProduceOrder"));
        return value == null ? null : ((Number) value).intValue();
    }

    private BeanWrapper wrapper(Cd15ScheduleResult result) {
        return PropertyAccessorFactory.forBeanPropertyAccess(result);
    }

    private String property(String classField, String suffix) {
        return classField.toLowerCase() + suffix;
    }

    private int defaultOrder(Integer value) {
        return value == null || value <= 0 ? 1 : value;
    }

    private List<Cd15MachineResource> safeMachines(Cd15MachineResourceSnapshot snapshot) {
        return snapshot == null || snapshot.getMachines() == null
                ? Collections.emptyList() : snapshot.getMachines();
    }

    private void validate(Cd15AutoScheduleContext context, Cd15ShiftDescriptor shift,
                          Cd15ShiftResourceState state) {
        if (context == null || context.getParameters() == null || shift == null || state == null
                || state.getRemainingSecondsByMachine() == null || state.getTailByMachine() == null
                || state.getTailSpecByMachine() == null || state.getTasks() == null
                || state.getLanes() == null) {
            throw new IllegalArgumentException("锁定任务资源恢复上下文不完整");
        }
    }
}
