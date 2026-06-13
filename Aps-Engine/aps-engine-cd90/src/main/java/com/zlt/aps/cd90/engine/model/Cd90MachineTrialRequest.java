package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 单个帘布规格的候选机台试算请求。
 */
@Data
@Builder
public class Cd90MachineTrialRequest {

    private String clothCode;
    private String bigRollCode;
    private String cordSpec;
    private String shiftCode;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;
    private BigDecimal netDemandQuantity;
    private boolean closeOut;
    private int occupiedVehicleCount;
    private int shiftHours;
    private Map<String, Integer> remainingSecondsByMachine;
    private Map<String, String> previousSpecByMachine;
    private Cd90AutoScheduleParameters parameters;
}
