package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CD15 逐班待排候选，按施工材料层位与成型自然需求组合生成。
 */
@Data
@Builder
public class Cd15ScheduleCandidate {

    private Cd15NaturalDemand demand;
    private Cd15ConstructionMaterial material;
    private Cd15ShiftDescriptor shift;
    private int classIndex;
    private String cuttingAngle;
    private String steelStripCode;
    private String bigRollCode;
    private BigDecimal stockMetersAtSix;
    private boolean shortageInCurrentShift;
    private boolean continueFromPreviousShift;
}
