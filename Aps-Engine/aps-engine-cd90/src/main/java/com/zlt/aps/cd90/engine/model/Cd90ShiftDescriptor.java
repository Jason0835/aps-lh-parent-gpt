package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动排程使用的直裁班次描述。
 */
@Data
@Builder
public class Cd90ShiftDescriptor {

    /** 业务班次编码，用于读取库排等班次基础数据。 */
    private String shiftCode;
    /** 排程结果班次字段，如CLASS1。 */
    private String classField;
    /** 排程窗口中的班次顺序。 */
    private Integer shiftOrder;
    /** 班次开始时间。 */
    private LocalDateTime startTime;
    /** 班次结束时间。 */
    private LocalDateTime endTime;
    /** 班次总秒数。 */
    private int durationSeconds;
}
