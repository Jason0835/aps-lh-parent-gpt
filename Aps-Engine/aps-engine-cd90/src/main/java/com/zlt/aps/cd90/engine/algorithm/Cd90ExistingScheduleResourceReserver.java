package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90MachineCapacityTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineResource;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90MachineResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
public class Cd90ExistingScheduleResourceReserver {

    private final Cd90MachineResourceService machineResourceService;
    private final Cd90MachineCapacityCalculator capacityCalculator;

    /** 将锁定任务按原机台和原顺序写入当前班资源快照。 */
    public void reserve(Cd90AutoScheduleContext context,
                        Cd90ShiftDescriptor shift,
                        Cd90ShiftResourceState state,
                        List<Cd90ScheduleResult> lockedResults,
                        Map<Long, List<Cd90ScheduleLaneAllocation>> sourceLanes) {
        if (lockedResults == null || lockedResults.isEmpty()) {
            return;
        }
        validate(context, shift, state);
        Cd90MachineResourceSnapshot snapshot = machineResourceService.load(
                context.getFactoryCode(), shift.getStartTime(), shift.getEndTime());
        Map<String, Cd90MachineResource> machineByCode = safeMachines(snapshot).stream()
                .filter(item -> item.getMachineCode() != null)
                .collect(Collectors.toMap(Cd90MachineResource::getMachineCode,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<Cd90ScheduleLaneAllocation>> lanesByResult = sourceLanes == null
                ? Collections.emptyMap() : sourceLanes;

        lockedResults.stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(Cd90ScheduleResult::getMachineCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(item -> readOrder(item, shift.getClassField()),
                                Comparator.nullsLast(Integer::compareTo)))
                .forEach(result -> reserveOne(context, shift, state, result,
                        machineByCode, lanesByResult));
    }

    private void reserveOne(Cd90AutoScheduleContext context,
                            Cd90ShiftDescriptor shift,
                            Cd90ShiftResourceState state,
                            Cd90ScheduleResult result,
                            Map<String, Cd90MachineResource> machineByCode,
                            Map<Long, List<Cd90ScheduleLaneAllocation>> lanesByResult) {
        BigDecimal quantity = readPlan(result, shift.getClassField());
        if (quantity.signum() <= 0) {
            return;
        }
        Cd90MachineResource machine = machineByCode.get(result.getMachineCode());
        if (machine == null || machine.getQuota() == null || machine.getQuota().signum() <= 0) {
            throw new IllegalStateException("锁定任务原机台不可用: " + result.getMachineCode());
        }
        int remainingSeconds = state.getRemainingSecondsByMachine()
                .getOrDefault(result.getMachineCode(), 0);
        Cd90MachineTailState currentTail = Cd90MachineTailState.builder()
                .clothCode(result.getClothCode()).bigRollCode(result.getBigRollCode()).build();
        Cd90MachineCapacityTrial trial = capacityCalculator.calculateWithRemainingSeconds(
                machine.getQuota(), Math.max(1, shift.getDurationSeconds() / 3600),
                remainingSeconds, state.getTailByMachine().get(result.getMachineCode()),
                currentTail, context.getParameters().getSameRollDiffSpecChangeMinutes(),
                context.getParameters().getDiffRollSameSpecChangeMinutes(),
                context.getParameters().getDiffRollDiffSpecChangeMinutes(), quantity);
        if (!trial.isFullyAccommodated()) {
            throw new IllegalStateException("锁定任务超过原机台剩余产能: " + result.getMachineCode());
        }
        List<Cd90StorageLaneAllocation> allocations = reserveLanes(
                shift, state, result, lanesByResult.getOrDefault(
                        result.getId(), Collections.emptyList()));
        int vehicleCount = allocations.stream()
                .mapToInt(Cd90StorageLaneAllocation::getVehicleCount).sum();
        if (state.getOccupiedToolingCount() + vehicleCount > state.getTotalToolingCount()) {
            throw new IllegalStateException("锁定任务占用工装超过当前可用数量");
        }
        state.setOccupiedToolingCount(state.getOccupiedToolingCount() + vehicleCount);
        state.getRemainingSecondsByMachine().put(
                result.getMachineCode(), trial.getRemainingSeconds());
        state.getTailByMachine().put(result.getMachineCode(), currentTail);
        state.getTailSpecByMachine().put(result.getMachineCode(), result.getClothCode());
        state.getTasks().add(Cd90ShiftScheduleTask.builder()
                .classField(shift.getClassField())
                .sourceTaskKey(result.getId() + ":" + shift.getClassField())
                .sourceResultId(result.getId())
                .clothCode(result.getClothCode()).bigRollCode(result.getBigRollCode())
                .cordSpec(result.getClothCode()).machineCode(result.getMachineCode())
                .planQuantity(quantity).vehicleCount(vehicleCount)
                .produceOrder(defaultOrder(readOrder(result, shift.getClassField())))
                .expectedStartTime(shift.getStartTime()).expectedEndTime(shift.getEndTime())
                .laneAllocations(allocations).build());
    }

    private List<Cd90StorageLaneAllocation> reserveLanes(
            Cd90ShiftDescriptor shift, Cd90ShiftResourceState state,
            Cd90ScheduleResult result, List<Cd90ScheduleLaneAllocation> sourceLanes) {
        List<Cd90ScheduleLaneAllocation> rows = sourceLanes.stream()
                .filter(item -> shift.getClassField().equals(item.getClassField()))
                .sorted(Comparator.comparing(Cd90ScheduleLaneAllocation::getAllocationOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            throw new IllegalStateException("锁定任务缺少原库排明细: " + result.getId());
        }
        List<Cd90StorageLaneAllocation> allocations = new ArrayList<>();
        rows.forEach(row -> {
            Cd90StorageLaneState lane = state.getLanes().stream()
                    .filter(item -> Objects.equals(row.getStorageLaneCode(), item.getLaneCode()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "锁定任务原库排不存在: " + row.getStorageLaneCode()));
            int vehicles = row.getAllocatedCartCount() == null
                    ? 0 : row.getAllocatedCartCount();
            if (!Objects.equals(result.getMachineCode(), lane.getMachineCode())) {
                throw new IllegalStateException("锁定任务原库排未绑定当前机台: "
                        + row.getStorageLaneCode());
            }
            if (vehicles <= 0 || lane.getVehicleCount() + vehicles > lane.getMaxVehicleCount()) {
                throw new IllegalStateException("锁定任务原库排容量不足: " + row.getStorageLaneCode());
            }
            if (lane.getVehicleCount() > 0
                    && !Objects.equals(result.getClothCode(), lane.getClothCode())) {
                throw new IllegalStateException("锁定任务原库排已被其他帘布占用: "
                        + row.getStorageLaneCode());
            }
            lane.setClothCode(result.getClothCode());
            lane.setVehicleCount(lane.getVehicleCount() + vehicles);
            allocations.add(Cd90StorageLaneAllocation.builder()
                    .laneCode(row.getStorageLaneCode()).vehicleCount(vehicles).build());
        });
        return allocations;
    }

    private BigDecimal readPlan(Cd90ScheduleResult result, String classField) {
        Object value = wrapper(result).getPropertyValue(property(classField, "PlanQty"));
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private Integer readOrder(Cd90ScheduleResult result, String classField) {
        Object value = wrapper(result).getPropertyValue(property(classField, "ProduceOrder"));
        return value == null ? null : ((Number) value).intValue();
    }

    private BeanWrapper wrapper(Cd90ScheduleResult result) {
        return PropertyAccessorFactory.forBeanPropertyAccess(result);
    }

    private String property(String classField, String suffix) {
        return classField.toLowerCase() + suffix;
    }

    private int defaultOrder(Integer value) {
        return value == null || value <= 0 ? 1 : value;
    }

    private List<Cd90MachineResource> safeMachines(Cd90MachineResourceSnapshot snapshot) {
        return snapshot == null || snapshot.getMachines() == null
                ? Collections.emptyList() : snapshot.getMachines();
    }

    private void validate(Cd90AutoScheduleContext context, Cd90ShiftDescriptor shift,
                          Cd90ShiftResourceState state) {
        if (context == null || context.getParameters() == null || shift == null || state == null
                || state.getRemainingSecondsByMachine() == null || state.getTailByMachine() == null
                || state.getTailSpecByMachine() == null || state.getTasks() == null
                || state.getLanes() == null) {
            throw new IllegalArgumentException("锁定任务资源恢复上下文不完整");
        }
    }
}
