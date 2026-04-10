package com.zlt.aps.lh.api.domain.dto;

import java.io.Serializable;

/**
 * 排程日期对象列表查询参数
 *
 * @author APS
 */
public class LhScheduleShiftDateQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期（窗口结束日），格式 yyyy-MM-dd */
    private String scheduleDate;

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }
}
