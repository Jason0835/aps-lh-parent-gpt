package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
/** TM/TC 新规格识别公共运行态模型。 */
@Data
public class ScheduleNewSpecInfoModel {
    protected Boolean newSpec;
    protected Integer lookbackDays;
    protected String lookbackDaysSource;
    protected Integer advanceShiftCount;
    protected String advanceShiftCountSource;
    protected Integer baseGuardShiftCount;
    protected Integer effectiveGuardShiftCount;
    protected Integer formingWindowStartClass;
    protected Integer formingWindowEndClass;
    protected Integer formingWindowEstimatedShiftCount;
    protected Date previousStockDate;
    protected BigDecimal previousDayStockQty;
    protected Boolean previousDayStockExists;
    protected Date historyStartDate;
    protected Date historyEndDate;
    protected Boolean historySchedulePlanExists;
    protected Integer normalTargetShift;
    protected Integer adjustedTargetShift;
    protected List<Integer> adjustedTargetWindow;
    protected Integer demandShift;
    protected BigDecimal demandQty;
    protected String reason;

    public boolean isNewSpecHit() {
        return Boolean.TRUE.equals(newSpec);
    }
}
