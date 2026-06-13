package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneConsumptionResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 按累计成型消耗释放库排车辆和工装的纯计算器。
 */
@Component
public class Cd90StorageLaneConsumptionCalculator {

    /**
     * 从6点库排快照副本中释放已完整消耗的车辆。
     *
     * @param cumulativeConsumption 累计成型消耗量
     * @param coilMeter 一车卷曲米数
     * @param originalLanes 6点库排原始快照
     * @return 扣减结果
     */
    public Cd90StorageLaneConsumptionResult consume(BigDecimal cumulativeConsumption,
                                                    BigDecimal coilMeter,
                                                    List<Cd90StorageLaneState> originalLanes) {
        if (cumulativeConsumption == null || cumulativeConsumption.signum() < 0) {
            throw new IllegalArgumentException("累计成型消耗不能小于0");
        }
        if (coilMeter == null || coilMeter.signum() <= 0) {
            throw new IllegalArgumentException("工装卷曲米数必须大于0");
        }
        List<Cd90StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());
        int releasable = cumulativeConsumption.divide(coilMeter, 0, RoundingMode.FLOOR).intValueExact();
        BigDecimal remainder = cumulativeConsumption.remainder(coilMeter);

        List<Cd90StorageLaneState> occupied = lanes.stream()
                .filter(item -> item.getVehicleCount() > 0)
                .sorted(Comparator.comparingInt(Cd90StorageLaneState::getVehicleCount)
                        .thenComparing(Cd90StorageLaneState::getLaneCode,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        int released = 0;
        for (Cd90StorageLaneState lane : occupied) {
            if (released >= releasable) {
                break;
            }
            int count = Math.min(lane.getVehicleCount(), releasable - released);
            lane.setVehicleCount(lane.getVehicleCount() - count);
            released += count;
            if (lane.getVehicleCount() == 0) {
                lane.setClothCode(null);
            }
        }
        return Cd90StorageLaneConsumptionResult.builder()
                .releasedVehicleCount(released)
                .remainderQuantity(remainder)
                .lanes(lanes)
                .build();
    }

    private Cd90StorageLaneState copy(Cd90StorageLaneState source) {
        return Cd90StorageLaneState.builder()
                .laneCode(source.getLaneCode())
                .clothCode(source.getClothCode())
                .vehicleCount(source.getVehicleCount())
                .maxVehicleCount(source.getMaxVehicleCount())
                .build();
    }
}
