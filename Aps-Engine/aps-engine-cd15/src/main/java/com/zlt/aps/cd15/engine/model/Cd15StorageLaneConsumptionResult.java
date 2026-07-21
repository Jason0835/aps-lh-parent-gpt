package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成型消耗扣减库排后的资源结果。
 */
@Data
@Builder
public class Cd15StorageLaneConsumptionResult {

    /** 实际释放车数及对应工装数。 */
    private int releasedVehicleCount;
    /** 未达到整车卷曲长度的消耗余量。 */
    private BigDecimal remainderQuantity;
    /** 扣减后的库排状态副本。 */
    private List<Cd15StorageLaneState> lanes;
}
