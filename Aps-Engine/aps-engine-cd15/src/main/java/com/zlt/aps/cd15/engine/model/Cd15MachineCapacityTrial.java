package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 机台班产能增量试算结果。
 */
@Data
@Builder
public class Cd15MachineCapacityTrial {

    /** 满班定额推导的机台速度，单位米/秒。 */
    private BigDecimal machineSpeed;
    /** 本次规格切换耗时秒数。 */
    private int changeSeconds;
    /** 当前任务实际生产耗时秒数。 */
    private int productionSeconds;
    /** 机台产能可排量。 */
    private BigDecimal capacityQuantity;
    /** 排入后剩余秒数。 */
    private int remainingSeconds;
    /** 是否能完整容纳请求数量。 */
    private boolean fullyAccommodated;
}
