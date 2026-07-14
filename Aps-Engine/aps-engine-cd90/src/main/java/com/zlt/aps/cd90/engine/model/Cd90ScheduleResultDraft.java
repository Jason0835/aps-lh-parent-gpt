package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 按帘布、大卷和机台归并的排程主结果草稿。 */
@Data
@Builder
public class Cd90ScheduleResultDraft {

    private String resultKey;
    private String clothCode;
    private String bigRollCode;
    private String cordSpec;
    private String machineCode;
    private String primaryLaneCode;
    private String dataSource;
    private String cxBatchNo;
    private String cxMachineCodes;
    private BigDecimal planSurplusQty;
    private List<Cd90ScheduleShiftSlotDraft> shiftSlots;
}
