package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 可直接供后续未排结果落库转换使用的内存模型。 */
@Data
@Builder
public class Cd90UnscheduledResultModel {

    private String clothCode;
    private String bigRollCode;
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
