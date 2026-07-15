package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Cd15StorageLaneAllocationResult {

    private boolean success;
    private String failureReason;
    private int requiredVehicleCount;
    private int allocatedVehicleCount;
    private List<Cd15StorageLaneAllocation> allocations;
    private List<Cd15StorageLaneState> lanes;
}