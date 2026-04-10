package com.zlt.aps.lh.api.domain.vo;

import java.io.Serializable;

/**
 * 排程日期对象：班次序号与对应日历展示日（MM/dd）
 *
 * @author APS
 */
public class LhScheduleShiftDateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次序号（1～8） */
    private int shift;

    /** 班次对应日期展示，格式 MM/dd，如 04/08 */
    private String shiftDate;

    public int getShift() {
        return shift;
    }

    public void setShift(int shift) {
        this.shift = shift;
    }

    public String getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(String shiftDate) {
        this.shiftDate = shiftDate;
    }
}
