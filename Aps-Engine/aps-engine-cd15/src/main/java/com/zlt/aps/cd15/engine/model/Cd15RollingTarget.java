package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** CD15定时滚动目标班次。 */
@Data
@Builder
public class Cd15RollingTarget {

    /** 工厂编码。 */
    private String factoryCode;
    /** 排程日期。 */
    private LocalDate scheduleDate;
    /** 当前需要重排的CD15批次号。 */
    private String batchNo;
    /** 目标业务班次编码。 */
    private String targetShiftCode;
    /** 目标结果班次字段。 */
    private String targetClassField;
    /** 目标结果班次序号，滚动时仅替换该班次及后续班次。 */
    private int targetClassIndex;
    /** 交接时间。 */
    private LocalDateTime handoverTime;
    /** 触发窗口开始时间。 */
    private LocalDateTime windowStart;
    /** 触发窗口结束时间。 */
    private LocalDateTime windowEnd;
}