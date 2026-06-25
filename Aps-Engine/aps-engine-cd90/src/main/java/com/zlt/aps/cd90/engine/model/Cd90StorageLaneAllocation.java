package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/** 库排车数分配明细。 */
@Data
@Builder
public class Cd90StorageLaneAllocation {
    /** 库排编码。 */
    private String laneCode;
    /** 本任务分配车数。 */
    private int vehicleCount;
}
