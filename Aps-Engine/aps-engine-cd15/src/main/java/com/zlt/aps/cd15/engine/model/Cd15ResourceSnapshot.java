package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 当前斜裁排程班次资源快照。
 */
@Data
@Builder
public class Cd15ResourceSnapshot {

    /** 当前库排状态。 */
    private List<Cd15StorageLaneState> lanes;
    /** 当前库排占用总车数及工装占用数。 */
    private int occupiedVehicleCount;
    /** 本次从6点快照累计释放的车辆数。 */
    private int releasedVehicleCount;
    /** 未达到整车的累计消耗余量。 */
    private BigDecimal consumptionRemainderQuantity;
}
