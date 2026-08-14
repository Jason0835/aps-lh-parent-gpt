package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocationResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
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
public class Cd90StorageLaneAllocator {

    /**
     * 在库排状态副本上分配计划入库车辆。
     * <p>
     * 候选库排分两段:
     * 第一段:已有相同帘布代号且有富余库容的库排(车数多的优先,减少碎片);
     * 第二段:空库排(clothCode 为空 且 vehicleCount=0),按 laneCode 稳定排序兜底。
     * 空库排被分配后 clothCode 置为当前帘布,后续班次作为同规格库排候选。
     * </p>
     *
     * @param clothCode 帘布代码
     * @param machineCode 当前候选机台编码
     * @param planQuantity 计划量,单位米
     * @param standardCurlLength 标准卷曲长度，直接作为一车工装卷的排程容量
     * @param originalLanes 原库排状态
     * @return 分配结果
     */
    public Cd90StorageLaneAllocationResult allocate(String clothCode,
                                                      String machineCode,
                                                      BigDecimal planQuantity,
                                                      BigDecimal standardCurlLength,
                                                      List<Cd90StorageLaneState> originalLanes) {
        return allocate(clothCode, machineCode, planQuantity, standardCurlLength, originalLanes, false);
    }

    /**
     * 硬插单分配时 isHardInsert=true，可将已被消耗(vehicleCount=0)但残留旧帘布标签的库位视为空库位，
     * 让插单帘布优先获得这些库位资源。
     */
    public Cd90StorageLaneAllocationResult allocate(String clothCode,
                                                      String machineCode,
                                                      BigDecimal planQuantity,
                                                      BigDecimal standardCurlLength,
                                                      List<Cd90StorageLaneState> originalLanes,
                                                      boolean isHardInsert) {
        if (!StringUtils.hasText(machineCode)) {
            throw new IllegalArgumentException("库排分配机台编码不能为空");
        }
        if (planQuantity == null || planQuantity.signum() <= 0) {
            throw new IllegalArgumentException("库排分配计划量必须大于0");
        }
        if (standardCurlLength == null || standardCurlLength.signum() <= 0) {
            throw new IllegalArgumentException("标准卷曲长度必须大于0");
        }
        int required = planQuantity.divide(standardCurlLength, 0, RoundingMode.CEILING).intValueExact();
        List<Cd90StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());

        // 第一段:同规格且有富余(车数多的优先,然后 laneCode 稳定排序)
        List<Cd90StorageLaneState> sameSpec = lanes.stream()
                .filter(item -> machineCode.equals(item.getMachineCode()))
                .filter(item -> item.getMaxVehicleCount() > item.getVehicleCount())
                .filter(item -> clothCode.equals(item.getClothCode()))
                .sorted(Comparator.comparingInt(Cd90StorageLaneState::getVehicleCount)
                        .reversed()
                        .thenComparing(Cd90StorageLaneState::getLaneCode))
                .collect(Collectors.toList());

        // 第二段:空库排,硬插单时 vehicleCount=0 即视为空(不论是否残留旧帘布标签)
        List<Cd90StorageLaneState> emptyLanes = lanes.stream()
                .filter(item -> machineCode.equals(item.getMachineCode()))
                .filter(item -> item.getMaxVehicleCount() > 0)
                .filter(item -> isHardInsert
                        ? item.getVehicleCount() == 0
                        : !StringUtils.hasText(item.getClothCode()) && item.getVehicleCount() == 0)
                .sorted(Comparator.comparing(Cd90StorageLaneState::getLaneCode))
                .collect(Collectors.toList());

        // 合并候选:先消耗同规格,不足再消耗空库排
        List<Cd90StorageLaneState> candidates = new ArrayList<>(sameSpec.size() + emptyLanes.size());
        candidates.addAll(sameSpec);
        candidates.addAll(emptyLanes);

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
        int allocated = required - remaining;
        if (allocated <= 0) {
            return Cd90StorageLaneAllocationResult.builder().success(false)
                    .failureReason("STORAGE_LANE_LIMIT").requiredVehicleCount(required)
                    .allocatedVehicleCount(0).allocations(new ArrayList<>()).lanes(originalLanes).build();
        }
        return Cd90StorageLaneAllocationResult.builder().success(true)
                .requiredVehicleCount(required).allocatedVehicleCount(allocated)
                .allocations(allocations).lanes(lanes).build();
    }

    private Cd90StorageLaneState copy(Cd90StorageLaneState source) {
        return Cd90StorageLaneState.builder().laneCode(source.getLaneCode())
                .machineCode(source.getMachineCode())
                .clothCode(source.getClothCode()).vehicleCount(source.getVehicleCount())
                .maxVehicleCount(source.getMaxVehicleCount()).build();
    }
}
