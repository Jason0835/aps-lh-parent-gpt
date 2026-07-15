package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * CD15 排程结果草稿，落库前在 Engine 内部流转。
 */
@Data
@Builder
public class Cd15ScheduleResultDraft {

    /** ORDER_NO/GROUP_NO：分裁两条结果共用，单裁时 GROUP_NO 可为空。 */
    private String orderNo;
    /** ORDER_NO/GROUP_NO：分裁组合号。 */
    private String groupNo;
    private String factoryCode;
    private Date scheduleDate;
    private String cxBatchNo;
    private String cxMachineCodes;
    private BigDecimal planSurplusQty;
    private String bigRollCode;
    private String bigRollBarcode;
    private String steelStripCode;
    private String cuttingAngle;
    private String machineCode;
    private String machineName;
    private String classField;
    private String shiftDisplayName;
    private int classIndex;
    private BigDecimal cxPlanQty;
    private BigDecimal planQty;
    private Integer produceOrder;
    private BigDecimal pieceCount;
    private BigDecimal netDemandMeters;
    private BigDecimal bigRollConsumeMeters;
    private BigDecimal vehiclePlanQuantity;
    private String storageLaneCode;
    private List<Cd15LaneAllocationDraft> laneAllocations;
    private BigDecimal stockMetersAtSix;
    private String cutMode;
    private String sourceType;
    private String analysis;
}