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
 * 从资源基线、前序入库和累计成型消耗重建当前班次的库排/工装占用快照。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90ResourceSnapshotBuilder {

    private final Cd90StorageLaneConsumptionCalculator consumptionCalculator;
    private final Cd90InboundResolver inboundResolver;

    /**
     * 按“基线库排 + 当前班前有效入库 - 累计成型消耗”重建当前班资源。
     *
     * @param originalLanes 资源基线库排快照
     * @param cumulativeConsumptionByCloth 按帘布代码汇总的累计成型消耗量，单位米
     * @param curlLengthByCloth 按帘布代码维护的单车卷曲长度，单位米
     * @param fallbackCoilMeter 缺少帘布卷曲长度时使用的兜底单车米数
     * @param inboundRecords 当前班次开班前有效的实际/计划直裁入库记录
     * @return 当前班次资源快照
     */
    public Cd90ResourceSnapshot build(List<Cd90StorageLaneState> originalLanes,
                                      Map<String, BigDecimal> cumulativeConsumptionByCloth,
                                      Map<String, BigDecimal> curlLengthByCloth,
                                      BigDecimal fallbackCoilMeter,
                                      List<Cd90InboundRecord> inboundRecords) {
        // 前序直裁计划入库已经进入当前班可见资源，也可能在当前班开班前被成型消耗扣减。
        // 所以这里不能先扣原始库排再全量加回入库，否则后续班次会把库排越滚越满。
        List<Cd90StorageLaneState> baseLanes = mergeInbound(copyLanes(originalLanes), inboundRecords);
        Cd90StorageLaneConsumptionResult consumption = consumptionCalculator.consume(
                cumulativeConsumptionByCloth, curlLengthByCloth, fallbackCoilMeter, baseLanes);
        List<Cd90StorageLaneState> lanes = new ArrayList<>(consumption.getLanes());
        int occupied = lanes.stream().mapToInt(Cd90StorageLaneState::getVehicleCount).sum();
        log.debug("[直裁自动排程] 当前班次资源快照重建完成, releasedVehicles={}, occupiedVehicles={}, "
                        + "remainder={}, cumulativeConsumptionByCloth={}",
                consumption.getReleasedVehicleCount(), occupied, consumption.getRemainderQuantity(),
                cumulativeConsumptionByCloth);
        return Cd90ResourceSnapshot.builder()
                .lanes(lanes)
                .occupiedVehicleCount(occupied)
                .releasedVehicleCount(consumption.getReleasedVehicleCount())
                .consumptionRemainderQuantity(consumption.getRemainderQuantity())
                .build();
    }

    /**
     * 将当前班次开班前已经有效的直裁入库并入库排基线，同一库排不允许混入不同帘布。
     */
    private List<Cd90StorageLaneState> mergeInbound(List<Cd90StorageLaneState> lanes,
                                                     List<Cd90InboundRecord> inboundRecords) {
        Map<String, Cd90StorageLaneState> laneMap = lanes.stream()
                .collect(Collectors.toMap(Cd90StorageLaneState::getLaneCode, Function.identity()));
        inboundResolver.resolve(inboundRecords).forEach(inbound -> {
            Cd90StorageLaneState lane = laneMap.get(inbound.getLaneCode());
            if (lane == null) {
                lane = Cd90StorageLaneState.builder()
                        .laneCode(inbound.getLaneCode()).machineCode(inbound.getMachineCode()).vehicleCount(0)
                        .maxVehicleCount(inbound.getVehicleCount()).build();
                lanes.add(lane);
                laneMap.put(lane.getLaneCode(), lane);
            }
            if (lane.getVehicleCount() > 0 && lane.getClothCode() != null
                    && !lane.getClothCode().equals(inbound.getClothCode())) {
                log.warn("[直裁库排] 入库库位 {} 已被帘布 {} 占用(原计划入库帘布 {}),跳过此条入库记录",
                        inbound.getLaneCode(), lane.getClothCode(), inbound.getClothCode());
                return;
            }
            lane.setClothCode(inbound.getClothCode());
            lane.setVehicleCount(lane.getVehicleCount() + inbound.getVehicleCount());
            lane.setMaxVehicleCount(Math.max(lane.getMaxVehicleCount(), lane.getVehicleCount()));
        });
        return lanes;
    }

    /**
     * 复制库排状态，避免快照重建过程修改外部传入的资源基线。
     */
    private List<Cd90StorageLaneState> copyLanes(List<Cd90StorageLaneState> lanes) {
        if (lanes == null) {
            return new ArrayList<>();
        }
        return lanes.stream().map(item -> Cd90StorageLaneState.builder()
                .laneCode(item.getLaneCode()).machineCode(item.getMachineCode())
                .clothCode(item.getClothCode())
                .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                .build()).collect(Collectors.toList());
    }
}
