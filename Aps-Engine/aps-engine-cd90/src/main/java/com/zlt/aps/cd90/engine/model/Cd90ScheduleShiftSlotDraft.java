package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 排程主结果草稿中的单班次槽位。 */
@Data
@Builder
public class Cd90ScheduleShiftSlotDraft {

    private String classField;
    private LocalDate scheduleDate;
    private BigDecimal planQuantity;
    private BigDecimal finishQuantity;
    private int produceOrder;
    private BigDecimal finishRate;
    private LocalDateTime expectedStartTime;
    private LocalDateTime expectedEndTime;
}
