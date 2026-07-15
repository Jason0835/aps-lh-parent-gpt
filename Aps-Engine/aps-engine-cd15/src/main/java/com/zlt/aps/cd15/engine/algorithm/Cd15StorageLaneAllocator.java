package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocationResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Cd15StorageLaneAllocator {

    public static final String STORAGE_LANE_LIMIT = "STORAGE_LANE_LIMIT";

    public Cd15StorageLaneAllocationResult allocate(String steelStripCode,
                                                    BigDecimal planQuantity,
                                                    BigDecimal vehiclePlanQuantity,
                                                    List<Cd15StorageLaneState> originalLanes) {
        if (planQuantity == null || planQuantity.signum() <= 0) {
            throw new IllegalArgumentException("库排分配计划量必须大于0");
        }
        if (vehiclePlanQuantity == null || vehiclePlanQuantity.signum() <= 0) {
            throw new IllegalArgumentException("单车斜裁排程米数必须大于0");
        }
        int required = planQuantity.divide(vehiclePlanQuantity, 0, RoundingMode.CEILING).intValueExact();
        List<Cd15StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());
        List<Cd15StorageLaneState> sameSpec = lanes.stream()
                .filter(item -> item.getMaxVehicleCount() > item.getVehicleCount())
                .filter(item -> steelStripCode.equals(item.getSteelStripCode()))
                .sorted(Comparator.comparingInt(Cd15StorageLaneState::getVehicleCount)
                        .reversed()
                        .thenComparing(Cd15StorageLaneState::getLaneCode))
                .collect(Collectors.toList());
        List<Cd15StorageLaneState> emptyLanes = lanes.stream()
                .filter(item -> item.getMaxVehicleCount() > 0)
                .filter(item -> !StringUtils.hasText(item.getSteelStripCode()) && item.getVehicleCount() == 0)
                .sorted(Comparator.comparing(Cd15StorageLaneState::getLaneCode))
                .collect(Collectors.toList());
        List<Cd15StorageLaneState> candidates = new ArrayList<>(sameSpec.size() + emptyLanes.size());
        candidates.addAll(sameSpec);
        candidates.addAll(emptyLanes);

        int remaining = required;
        List<Cd15StorageLaneAllocation> allocations = new ArrayList<>();
        for (Cd15StorageLaneState lane : candidates) {
            if (remaining == 0) {
                break;
            }
            int capacity = lane.getMaxVehicleCount() - lane.getVehicleCount();
            int assigned = Math.min(capacity, remaining);
            if (assigned <= 0) {
                continue;
            }
            lane.setSteelStripCode(steelStripCode);
            lane.setVehicleCount(lane.getVehicleCount() + assigned);
            allocations.add(Cd15StorageLaneAllocation.builder()
                    .laneCode(lane.getLaneCode()).vehicleCount(assigned).build());
            remaining -= assigned;
        }
        int allocated = required - remaining;
        if (allocated < required) {
            return Cd15StorageLaneAllocationResult.builder().success(false)
                    .failureReason(STORAGE_LANE_LIMIT).requiredVehicleCount(required)
                    .allocatedVehicleCount(allocated).allocations(allocations).lanes(originalLanes).build();
        }
        return Cd15StorageLaneAllocationResult.builder().success(true)
                .requiredVehicleCount(required).allocatedVehicleCount(allocated)
                .allocations(allocations).lanes(lanes).build();
    }

    private Cd15StorageLaneState copy(Cd15StorageLaneState source) {
        return Cd15StorageLaneState.builder().laneCode(source.getLaneCode())
                .steelStripCode(source.getSteelStripCode()).vehicleCount(source.getVehicleCount())
                .maxVehicleCount(source.getMaxVehicleCount()).build();
    }
}