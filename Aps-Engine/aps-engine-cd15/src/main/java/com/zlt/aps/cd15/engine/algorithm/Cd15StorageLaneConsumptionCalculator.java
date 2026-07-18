package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15StorageLaneConsumptionResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 按累计成型消耗释放库排车辆和工装的纯计算器。
 */
@Component
public class Cd15StorageLaneConsumptionCalculator {

    /**
     * 从6点库排快照副本中释放已经被成型消耗整车消耗掉的车辆。
     *
     * <p>累计消耗必须按钢带代号分组扣减：C01的成型消耗只能释放C01占用的库排，不能把C02的库排
     * 释放成空库排后再分配给C01。库排号仍按物理库排唯一处理，不使用“钢带代号+库排号”拆成多条资源。</p>
     *
     * @param cumulativeConsumptionBySteelStrip 按钢带代号汇总的累计成型消耗量，单位米
     * @param coilMeter 一车卷曲米数
     * @param originalLanes 6点库排原始快照
     * @return 扣减结果
     */
    public Cd15StorageLaneConsumptionResult consume(Map<String, BigDecimal> cumulativeConsumptionBySteelStrip,
                                                    Map<String, BigDecimal> curlLengthBySteelStrip,
                                                    BigDecimal fallbackCoilMeter,
                                                    List<Cd15StorageLaneState> originalLanes) {
        if (fallbackCoilMeter == null || fallbackCoilMeter.signum() <= 0) {
            throw new IllegalArgumentException("工装卷曲米数必须大于0");
        }
        List<Cd15StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());
        Map<String, BigDecimal> consumptionBySteelStrip = cumulativeConsumptionBySteelStrip == null
                ? Collections.emptyMap() : cumulativeConsumptionBySteelStrip;
        if (consumptionBySteelStrip.values().stream()
                .filter(Objects::nonNull).anyMatch(item -> item.signum() < 0)) {
            throw new IllegalArgumentException("累计成型消耗不能小于0");
        }

        int released = consumptionBySteelStrip.entrySet().stream()
                .mapToInt(entry -> releaseSameSteelStrip(lanes, entry.getKey(), value(entry.getValue()),
                        curlLength(curlLengthBySteelStrip, entry.getKey(), fallbackCoilMeter)))
                .sum();
        BigDecimal remainder = consumptionBySteelStrip.entrySet().stream()
                .map(entry -> value(entry.getValue()).remainder(
                        curlLength(curlLengthBySteelStrip, entry.getKey(), fallbackCoilMeter)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Cd15StorageLaneConsumptionResult.builder()
                .releasedVehicleCount(released)
                .remainderQuantity(remainder)
                .lanes(lanes)
                .build();
    }

    private int releaseSameSteelStrip(List<Cd15StorageLaneState> lanes, String steelStripCode,
                                 BigDecimal cumulativeConsumption, BigDecimal coilMeter) {
        if (steelStripCode == null || cumulativeConsumption.signum() <= 0) {
            return 0;
        }
        int releasable = cumulativeConsumption.divide(coilMeter, 0, RoundingMode.FLOOR).intValueExact();
        if (releasable <= 0) {
            return 0;
        }
        List<Cd15StorageLaneState> occupied = lanes.stream()
                .filter(item -> steelStripCode.equals(item.getSteelStripCode()))
                .filter(item -> item.getVehicleCount() > 0)
                .sorted(Comparator.comparingInt(Cd15StorageLaneState::getVehicleCount)
                        .thenComparing(Cd15StorageLaneState::getLaneCode,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        int released = 0;
        for (Cd15StorageLaneState lane : occupied) {
            if (released >= releasable) {
                break;
            }
            int count = Math.min(lane.getVehicleCount(), releasable - released);
            lane.setVehicleCount(lane.getVehicleCount() - count);
            released += count;
            // 库排释放到0车后仍保留原钢带代码：0车代表该钢带的定向空位，不能变成任意钢带可抢占的公共空库排。
        }
        return released;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal curlLength(Map<String, BigDecimal> curlLengthBySteelStrip,
                                  String steelStripCode,
                                  BigDecimal fallbackCoilMeter) {
        if (curlLengthBySteelStrip != null) {
            BigDecimal curlLength = curlLengthBySteelStrip.get(steelStripCode);
            if (curlLength != null && curlLength.signum() > 0) {
                return curlLength;
            }
        }
        return fallbackCoilMeter;
    }

    private Cd15StorageLaneState copy(Cd15StorageLaneState source) {
        return Cd15StorageLaneState.builder()
                .laneCode(source.getLaneCode())
                .steelStripCode(source.getSteelStripCode())
                .vehicleCount(source.getVehicleCount())
                .maxVehicleCount(source.getMaxVehicleCount())
                .build();
    }
}
