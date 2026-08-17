package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneConsumptionResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
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
public class Cd90StorageLaneConsumptionCalculator {

    /**
     * 从6点库排快照副本中释放已经被成型消耗整车消耗掉的车辆。
     *
     * <p>累计消耗必须按帘布代号分组扣减：C01的成型消耗只能释放C01占用的库排，不能把C02的库排
     * 释放成空库排后再分配给C01。库排号仍按物理库排唯一处理，不使用“帘布代号+库排号”拆成多条资源。</p>
     *
     * @param cumulativeConsumptionByCloth 按帘布代号汇总的累计成型消耗量，单位米
     * @param coilMeter 一车卷曲米数
     * @param originalLanes 6点库排原始快照
     * @return 扣减结果
     */
    public Cd90StorageLaneConsumptionResult consume(Map<String, BigDecimal> cumulativeConsumptionByCloth,
                                                    Map<String, BigDecimal> curlLengthByCloth,
                                                    BigDecimal fallbackCoilMeter,
                                                    List<Cd90StorageLaneState> originalLanes) {
        if (fallbackCoilMeter == null || fallbackCoilMeter.signum() <= 0) {
            throw new IllegalArgumentException("工装卷曲米数必须大于0");
        }
        List<Cd90StorageLaneState> lanes = originalLanes == null ? new ArrayList<>()
                : originalLanes.stream().map(this::copy).collect(Collectors.toList());
        Map<String, BigDecimal> consumptionByCloth = cumulativeConsumptionByCloth == null
                ? Collections.emptyMap() : cumulativeConsumptionByCloth;
        if (consumptionByCloth.values().stream()
                .filter(Objects::nonNull).anyMatch(item -> item.signum() < 0)) {
            throw new IllegalArgumentException("累计成型消耗不能小于0");
        }

        int released = consumptionByCloth.entrySet().stream()
                .mapToInt(entry -> releaseSameCloth(lanes, entry.getKey(), value(entry.getValue()),
                        curlLength(curlLengthByCloth, entry.getKey(), fallbackCoilMeter)))
                .sum();
        BigDecimal remainder = consumptionByCloth.entrySet().stream()
                .map(entry -> value(entry.getValue()).remainder(
                        curlLength(curlLengthByCloth, entry.getKey(), fallbackCoilMeter)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Cd90StorageLaneConsumptionResult.builder()
                .releasedVehicleCount(released)
                .remainderQuantity(remainder)
                .lanes(lanes)
                .build();
    }

    private int releaseSameCloth(List<Cd90StorageLaneState> lanes, String clothCode,
                                 BigDecimal cumulativeConsumption, BigDecimal coilMeter) {
        if (clothCode == null || cumulativeConsumption.signum() <= 0) {
            return 0;
        }
        int releasable = cumulativeConsumption.divide(coilMeter, 0, RoundingMode.FLOOR).intValueExact();
        if (releasable <= 0) {
            return 0;
        }
        List<Cd90StorageLaneState> occupied = lanes.stream()
                .filter(item -> clothCode.equals(item.getClothCode()))
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
            // 库排释放到0车后仍保留原帘布代码：0车代表该帘布的定向空位，不能变成任意帘布可抢占的公共空库排。
        }
        return released;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal curlLength(Map<String, BigDecimal> curlLengthByCloth,
                                  String clothCode,
                                  BigDecimal fallbackCoilMeter) {
        if (curlLengthByCloth != null) {
            BigDecimal curlLength = curlLengthByCloth.get(clothCode);
            if (curlLength != null && curlLength.signum() > 0) {
                return curlLength;
            }
        }
        return fallbackCoilMeter;
    }

    private Cd90StorageLaneState copy(Cd90StorageLaneState source) {
        return Cd90StorageLaneState.builder()
                .laneCode(source.getLaneCode())
                .machineCode(source.getMachineCode())
                .clothCode(source.getClothCode())
                .vehicleCount(source.getVehicleCount())
                .maxVehicleCount(source.getMaxVehicleCount())
                .build();
    }
}
