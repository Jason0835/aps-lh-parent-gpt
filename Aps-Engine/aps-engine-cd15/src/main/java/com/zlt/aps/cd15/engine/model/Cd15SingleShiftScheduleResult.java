package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CD15 单规格单班试排结果。
 */
@Data
@Builder
public class Cd15SingleShiftScheduleResult {

    private boolean scheduled;
    private String unscheduledReasonCode;
    private String unscheduledReason;
    private Cd15ScheduleResultDraft draft;
    private String factoryCode;
    private Date scheduleDate;
    private String steelStripCode;
    private String bigRollCode;
    private String cuttingAngle;
    private String machineCode;
    private String classField;
    private String shiftDisplayName;
    private BigDecimal demandQty;
    private BigDecimal scheduledQty;
    private BigDecimal unscheduledQty;
    private String orderNo;
    private String groupNo;

    public static Cd15SingleShiftScheduleResult scheduled(Cd15ScheduleResultDraft draft) {
        return Cd15SingleShiftScheduleResult.builder().scheduled(true).draft(draft).build();
    }

    public static Cd15SingleShiftScheduleResult unscheduled(String reasonCode, String reason) {
        return Cd15SingleShiftScheduleResult.builder()
                .scheduled(false)
                .unscheduledReasonCode(reasonCode)
                .unscheduledReason(reason)
                .build();
    }
}