package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Cd15StorageLaneAllocation {

    private String laneCode;
    private int vehicleCount;
}