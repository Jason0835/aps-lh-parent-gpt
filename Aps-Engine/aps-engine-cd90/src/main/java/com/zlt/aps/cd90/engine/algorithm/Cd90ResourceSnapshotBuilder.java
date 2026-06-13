package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import com.zlt.aps.cd90.engine.model.Cd90ResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneConsumptionResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 从6点原始资源快照重建当前班次库排和工装状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90ResourceSnapshotBuilder {

    private final Cd90StorageLaneConsumptionCalculator consumptionCalculator;
    private final Cd90InboundResolver inboundResolver;

    /**
     * 按“6点快照 - 累计成型消耗 + 有效直裁入库”重建资源。
     *
     * @param originalLanes 6点库排原始快照
     * @param cumulativeConsumption 累计成型消耗
     * @param coilMeter 一车卷曲米数
     * @param inboundRecords 班次开始前实际或计划入库记录
     * @return 当前班次资源快照
     */
    public Cd90ResourceSnapshot build(List<Cd90StorageLaneState> originalLanes,
                                      BigDecimal cumulativeConsumption,
                                      BigDecimal coilMeter,
                                      List<Cd90InboundRecord> inboundRecords) {
        Cd90StorageLaneConsumptionResult consumption = consumptionCalculator.consume(
                cumulativeConsumption, coilMeter, originalLanes);
        List<Cd90StorageLaneState> lanes = new ArrayList<>(consumption.getLanes());
        Map<String, Cd90StorageLaneState> laneMap = lanes.stream()
                .collect(Collectors.toMap(Cd90StorageLaneState::getLaneCode, Function.identity()));

        inboundResolver.resolve(inboundRecords).forEach(inbound -> {
            Cd90StorageLaneState lane = laneMap.get(inbound.getLaneCode());
            if (lane == null) {
                lane = Cd90StorageLaneState.builder()
                        .laneCode(inbound.getLaneCode()).vehicleCount(0)
                        .maxVehicleCount(inbound.getVehicleCount()).build();
                lanes.add(lane);
                laneMap.put(lane.getLaneCode(), lane);
            }
            if (lane.getVehicleCount() > 0 && lane.getClothCode() != null
                    && !lane.getClothCode().equals(inbound.getClothCode())) {
                throw new IllegalArgumentException("同一库排不能恢复不同帘布入库");
            }
            lane.setClothCode(inbound.getClothCode());
            lane.setVehicleCount(lane.getVehicleCount() + inbound.getVehicleCount());
            lane.setMaxVehicleCount(Math.max(lane.getMaxVehicleCount(), lane.getVehicleCount()));
        });
        int occupied = lanes.stream().mapToInt(Cd90StorageLaneState::getVehicleCount).sum();
        log.debug("[直裁自动排程] 当前班次资源快照重建完成, releasedVehicles={}, occupiedVehicles={}, remainder={}",
                consumption.getReleasedVehicleCount(), occupied, consumption.getRemainderQuantity());
        return Cd90ResourceSnapshot.builder()
                .lanes(lanes)
                .occupiedVehicleCount(occupied)
                .releasedVehicleCount(consumption.getReleasedVehicleCount())
                .consumptionRemainderQuantity(consumption.getRemainderQuantity())
                .build();
    }
}
