package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Cd15LaneAllocationDraft {

    private String classField;
    private String laneCode;
    private BigDecimal allocationQuantity;
    private int vehicleCount;
}