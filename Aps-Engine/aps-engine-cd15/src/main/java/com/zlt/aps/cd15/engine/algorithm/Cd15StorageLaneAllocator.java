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

/**
 * 当前班次库排纯内存试分配器。
 */
@Component
public class Cd15StorageLaneAllocator {

    /**
     * 在库排状态副本上分配计划入库车辆。
     * <p>
     * 候选库排分两段:
     * 第一段:已有相同钢带代号且有富余库容的库排(车数多的优先,减少碎片);
     * 第二段:空库排(steelStripCode 为空 且 vehicleCount=0),按 laneCode 稳定排序兜底。
     * 空库排被分配后 steelStripCode 置为当前钢带,后续班次作为同规格库排候选。
     * </p>
     *
     * @param steelStripCode 钢带代码
     * @param planQuantity 计划量,单位米
     * @param vehiclePlanQuantity 单车对应的斜裁排程米数
     * @param originalLanes 原库排状态
     * @return 分配结果
     */
    public Cd15StorageLaneAllocationResult allocate(String steelStripCode,
                                                      BigDecimal planQuantity,
                                                      BigDecimal vehiclePlanQuantity,
                                                      List<Cd15StorageLaneState> originalLanes) {
        return allocate(steelStripCode, planQuantity, vehiclePlanQuantity, originalLanes, false);
    }

    /**
     * 硬插单分配时 isHardInsert=true，可将已被消耗(vehicleCount=0)但残留旧钢带标签的库位视为空库位，
     * 让插单钢带优先获得这些库位资源。
     */
    public Cd15StorageLaneAllocationResult allocate(String steelStripCode,
                                                      BigDecimal planQuantity,
                                                      BigDecimal vehiclePlanQuantity,
                                                      List<Cd15StorageLaneState> originalLanes,
                                                      boolean isHardInsert) {
        if (!StringUtils.hasText(steelStripCode)) {
            throw new IllegalArgumentException("库排分配钢带代码不能为空");
        }
        if (planQuantity == null || planQuantity.signum() <= 0) {
            throw new IllegalArgumentException("库排分配计划量必须大于0");
        }
        if (vehiclePlanQuantity == null || vehiclePlanQuantity.signum() <= 0) {
            throw new IllegalArgumentException("单车斜裁排程米数必须大于0");
        }
        int required = planQuantity.divide(vehiclePlanQuantity, 0, RoundingMode.CEILING).intValueExact();
        List<Cd15StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());

        // 第一段:同规格且有富余(车数多的优先,然后 laneCode 稳定排序)
        List<Cd15StorageLaneState> sameSpec = lanes.stream()
                .filter(item -> item.getMaxVehicleCount() > item.getVehicleCount())
                .filter(item -> steelStripCode.equals(item.getSteelStripCode()))
                .sorted(Comparator.comparingInt(Cd15StorageLaneState::getVehicleCount)
                        .reversed()
                        .thenComparing(Cd15StorageLaneState::getLaneCode))
                .collect(Collectors.toList());

        // 第二段:空库排,硬插单时 vehicleCount=0 即视为空(不论是否残留旧钢带标签)
        List<Cd15StorageLaneState> emptyLanes = lanes.stream()
                .filter(item -> item.getMaxVehicleCount() > 0)
                .filter(item -> isHardInsert
                        ? item.getVehicleCount() == 0
                        : !StringUtils.hasText(item.getSteelStripCode()) && item.getVehicleCount() == 0)
                .sorted(Comparator.comparing(Cd15StorageLaneState::getLaneCode))
                .collect(Collectors.toList());

        // 合并候选:先消耗同规格,不足再消耗空库排
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
        if (allocated <= 0) {
            return Cd15StorageLaneAllocationResult.builder().success(false)
                    .failureReason("STORAGE_LANE_LIMIT").requiredVehicleCount(required)
                    .allocatedVehicleCount(0).allocations(new ArrayList<>()).lanes(originalLanes).build();
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
