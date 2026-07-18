package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 排程结果的班次库排分配明细草稿。 */
@Data
@Builder
public class Cd15LaneAllocationDraft {

    private String resultKey;
    private String classField;
    private String laneCode;
    private BigDecimal allocationQuantity;
    private int vehicleCount;
}
