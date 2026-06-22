package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 库排试分配结果。 */
@Data
@Builder
public class Cd90StorageLaneAllocationResult {
    /** 是否分配成功。 */
    private boolean success;
    /** 失败原因编码。 */
    private String failureReason;
    /** 计划量换算后的所需车数。 */
    private int requiredVehicleCount;
    /** 本次实际分配到的车数。 */
    private int allocatedVehicleCount;
    /** 库排分配明细。 */
    private List<Cd90StorageLaneAllocation> allocations;
    /** 分配成功后的库排状态副本。 */
    private List<Cd90StorageLaneState> lanes;
}
