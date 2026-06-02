package com.zlt.aps.tq.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 胎圈排程班次日期VO
 * 用于前端列表表头动态展示班次对应的日期
 *
 * @author APS
 */
@Data
public class TqScheduleShiftDateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次序号（1～6） */
    private int shift;

    /** 班次类型：night=夜班, morning=早班, afternoon=中班 */
    private String shiftType;

    /** 班次对应日期展示，格式 MM/dd，如 06/03 */
    private String shiftDate;
}
