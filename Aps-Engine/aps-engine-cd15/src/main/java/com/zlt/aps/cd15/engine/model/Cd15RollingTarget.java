package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 一次定时滚动检查解析出的目标批次和班次。 */
@Data
@Builder
public class Cd15RollingTarget {

    /** 工厂编码。 */
    private String factoryCode;
    /** 排程日期。 */
    private LocalDate scheduleDate;
    /** 当前有效批次号。 */
    private String batchNo;
    /** 目标班次编码。 */
    private String targetShiftCode;
    /** 目标结果字段，如CLASS4。 */
    private String targetClassField;
    /** 交班物理时间。 */
    private LocalDateTime handoverTime;
    /** 滚动窗口开始时间。 */
    private LocalDateTime windowStart;
    /** 滚动窗口结束时间。 */
    private LocalDateTime windowEnd;
}
