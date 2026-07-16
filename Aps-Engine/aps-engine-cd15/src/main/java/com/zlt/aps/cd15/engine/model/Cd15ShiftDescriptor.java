package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CD15 自动排程使用的班次描述。
 */
@Data
@Builder
public class Cd15ShiftDescriptor {

    /** 标准三班编码。 */
    private String shiftCode;
    /** 配置中的班次名称。 */
    private String shiftName;
    /** 面向页面和任务进度的班次展示名称。 */
    private String shiftDisplayName;
    /** 班次业务归属日期，跨日班次取结束日。 */
    private LocalDate scheduleDate;
    /** 排程结果班次字段，如 CLASS1。 */
    private String classField;
    /** 排程结果班次序号。 */
    private int classIndex;
    /** 班次全局顺序。 */
    private Integer shiftOrder;
    /** 班次开始时点。 */
    private LocalDateTime startTime;
    /** 班次结束时点。 */
    private LocalDateTime endTime;
    /** 班次总秒数。 */
    private int durationSeconds;
}
