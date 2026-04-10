package com.zlt.aps.lh.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 排程日期对象：班次序号与对应日历展示日（MM/dd）
 *
 * @author APS
 */
@Data
public class LhScheduleShiftDateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次序号（1～8） */
    private int shift;

    /** 班次对应日期展示，格式 MM/dd，如 04/08 */
    private String shiftDate;
}
