package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15InboundRecord;
import com.zlt.aps.cd15.engine.model.Cd15ResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneConsumptionResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
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
public class Cd15ResourceSnapshotBuilder {

    private final Cd15StorageLaneConsumptionCalculator consumptionCalculator;
    private final Cd15InboundResolver inboundResolver;

    /**
     * 按“基线库排 + 当前班前有效入库 - 累计成型消耗”重建当前班资源。
     *
     * @param originalLanes 资源基线库排快照
     * @param cumulativeConsumptionBySteelStrip 按钢带代码汇总的累计成型消耗量，单位米
     * @param curlLengthBySteelStrip 按钢带代码维护的单车卷曲长度，单位米
     * @param fallbackCoilMeter 缺少钢带卷曲长度时使用的兜底单车米数
     * @param inboundRecords 当前班次开班前有效的实际/计划斜裁入库记录
     * @return 当前班次资源快照
     */
    public Cd15ResourceSnapshot build(List<Cd15StorageLaneState> originalLanes,
                                      Map<String, BigDecimal> cumulativeConsumptionBySteelStrip,
                                      Map<String, BigDecimal> curlLengthBySteelStrip,
                                      BigDecimal fallbackCoilMeter,
                                      List<Cd15InboundRecord> inboundRecords) {
        // 前序斜裁计划入库已经进入当前班可见资源，也可能在当前班开班前被成型消耗扣减。
        // 所以这里不能先扣原始库排再全量加回入库，否则后续班次会把库排越滚越满。
        List<Cd15StorageLaneState> baseLanes = mergeInbound(copyLanes(originalLanes), inboundRecords);
        Cd15StorageLaneConsumptionResult consumption = consumptionCalculator.consume(
                cumulativeConsumptionBySteelStrip, curlLengthBySteelStrip, fallbackCoilMeter, baseLanes);
        List<Cd15StorageLaneState> lanes = new ArrayList<>(consumption.getLanes());
        int occupied = lanes.stream().mapToInt(Cd15StorageLaneState::getVehicleCount).sum();
        log.debug("[斜裁自动排程] 当前班次资源快照重建完成, releasedVehicles={}, occupiedVehicles={}, "
                        + "remainder={}, cumulativeConsumptionBySteelStrip={}",
                consumption.getReleasedVehicleCount(), occupied, consumption.getRemainderQuantity(),
                cumulativeConsumptionBySteelStrip);
        return Cd15ResourceSnapshot.builder()
                .lanes(lanes)
                .occupiedVehicleCount(occupied)
                .releasedVehicleCount(consumption.getReleasedVehicleCount())
                .consumptionRemainderQuantity(consumption.getRemainderQuantity())
                .build();
    }

    /**
     * 将当前班次开班前已经有效的斜裁入库并入库排基线，同一库排不允许混入不同钢带。
     */
    private List<Cd15StorageLaneState> mergeInbound(List<Cd15StorageLaneState> lanes,
                                                     List<Cd15InboundRecord> inboundRecords) {
        Map<String, Cd15StorageLaneState> laneMap = lanes.stream()
                .collect(Collectors.toMap(Cd15StorageLaneState::getLaneCode, Function.identity()));
        inboundResolver.resolve(inboundRecords).forEach(inbound -> {
            Cd15StorageLaneState lane = laneMap.get(inbound.getLaneCode());
            if (lane == null) {
                lane = Cd15StorageLaneState.builder()
                        .laneCode(inbound.getLaneCode()).vehicleCount(0)
                        .maxVehicleCount(inbound.getVehicleCount()).build();
                lanes.add(lane);
                laneMap.put(lane.getLaneCode(), lane);
            }
            if (lane.getVehicleCount() > 0 && lane.getSteelStripCode() != null
                    && !lane.getSteelStripCode().equals(inbound.getSteelStripCode())) {
                log.warn("[斜裁库排] 入库库位 {} 已被钢带 {} 占用(原计划入库钢带 {}),跳过此条入库记录",
                        inbound.getLaneCode(), lane.getSteelStripCode(), inbound.getSteelStripCode());
                return;
            }
            lane.setSteelStripCode(inbound.getSteelStripCode());
            lane.setVehicleCount(lane.getVehicleCount() + inbound.getVehicleCount());
            lane.setMaxVehicleCount(Math.max(lane.getMaxVehicleCount(), lane.getVehicleCount()));
        });
        return lanes;
    }

    /**
     * 复制库排状态，避免快照重建过程修改外部传入的资源基线。
     */
    private List<Cd15StorageLaneState> copyLanes(List<Cd15StorageLaneState> lanes) {
        if (lanes == null) {
            return new ArrayList<>();
        }
        return lanes.stream().map(item -> Cd15StorageLaneState.builder()
                .laneCode(item.getLaneCode()).steelStripCode(item.getSteelStripCode())
                .vehicleCount(item.getVehicleCount()).maxVehicleCount(item.getMaxVehicleCount())
                .build()).collect(Collectors.toList());
    }
}