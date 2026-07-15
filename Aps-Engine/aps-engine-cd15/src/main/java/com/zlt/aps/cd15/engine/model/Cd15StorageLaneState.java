package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Cd15StorageLaneState {

    private String laneCode;
    private String steelStripCode;
    private int vehicleCount;
    private int maxVehicleCount;
}