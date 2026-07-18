package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 可直接供后续未排结果落库转换使用的内存模型。 */
@Data
@Builder
public class Cd15UnscheduledResultModel {

    private String steelStripCode;
    private String bigRollCode;
    private String cuttingAngle;
    private BigDecimal demandQuantity;
    private BigDecimal scheduledQuantity;
    private BigDecimal unscheduledQuantity;
    private String failStage;
    private String reasonCode;
    private int reasonOrder;
    private boolean primaryReason;
    private String reasonDescription;
    private String candidateMachineCodes;
}
