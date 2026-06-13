package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocationResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 当前班次库排纯内存试分配器。
 */
@Component
public class Cd90StorageLaneAllocator {

    /**
     * 在库排状态副本上分配计划入库车辆。
     *
     * @param clothCode 帘布代码
     * @param planQuantity 计划量，单位米
     * @param coilMeter 单车卷曲米数
     * @param originalLanes 原库排状态
     * @return 分配结果
     */
    public Cd90StorageLaneAllocationResult allocate(String clothCode,
                                                     BigDecimal planQuantity,
                                                     BigDecimal coilMeter,
                                                     List<Cd90StorageLaneState> originalLanes) {
        if (planQuantity == null || planQuantity.signum() <= 0) {
            throw new IllegalArgumentException("库排分配计划量必须大于0");
        }
        if (coilMeter == null || coilMeter.signum() <= 0) {
            throw new IllegalArgumentException("工装卷曲米数必须大于0");
        }
        int required = planQuantity.divide(coilMeter, 0, RoundingMode.CEILING).intValueExact();
        List<Cd90StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());
        List<Cd90StorageLaneState> candidates = lanes.stream()
                .filter(item -> item.getMaxVehicleCount() > item.getVehicleCount())
                .filter(item -> clothCode.equals(item.getClothCode())
                        || item.getVehicleCount() == 0)
                .sorted(Comparator
                        .comparing((Cd90StorageLaneState item) -> !clothCode.equals(item.getClothCode()))
                        .thenComparingInt(Cd90StorageLaneState::getVehicleCount)
                        .thenComparing(Cd90StorageLaneState::getLaneCode))
                .collect(Collectors.toList());
        int remaining = required;
        List<Cd90StorageLaneAllocation> allocations = new ArrayList<>();
        for (Cd90StorageLaneState lane : candidates) {
            if (remaining == 0) {
                break;
            }
            int capacity = lane.getMaxVehicleCount() - lane.getVehicleCount();
            int assigned = Math.min(capacity, remaining);
            if (assigned <= 0) {
                continue;
            }
            lane.setClothCode(clothCode);
            lane.setVehicleCount(lane.getVehicleCount() + assigned);
            allocations.add(Cd90StorageLaneAllocation.builder()
                    .laneCode(lane.getLaneCode()).vehicleCount(assigned).build());
            remaining -= assigned;
        }
        if (remaining > 0) {
            return Cd90StorageLaneAllocationResult.builder().success(false)
                    .failureReason("STORAGE_LANE_LIMIT").requiredVehicleCount(required)
                    .allocations(new ArrayList<>()).lanes(originalLanes).build();
        }
        return Cd90StorageLaneAllocationResult.builder().success(true)
                .requiredVehicleCount(required).allocations(allocations).lanes(lanes).build();
    }

    private Cd90StorageLaneState copy(Cd90StorageLaneState source) {
        return Cd90StorageLaneState.builder().laneCode(source.getLaneCode())
                .clothCode(source.getClothCode()).vehicleCount(source.getVehicleCount())
                .maxVehicleCount(source.getMaxVehicleCount()).build();
    }
}
