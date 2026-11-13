package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/**
 * 一份排程日期和班序对物理班次的表示。
 */
@Data
public class RollingPhysicalShiftRepresentation {

    /**
     * 该结果行所属排程日期。
     */
    private LocalDate scheduleDate;

    /**
     * 该排程日期下的班序。
     */
    private Integer shiftOrder;

    /**
     * 班次物理开始时间。
     */
    private Date startTime;

    /**
     * 班次物理结束时间。
     */
    private Date endTime;
}
