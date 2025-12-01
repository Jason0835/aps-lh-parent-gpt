package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化排产 班次定义时间
 * @date 2025/2/19
 */
@Data
public class ShiftTimeWindowDTO implements Serializable {


    private static final long serialVersionUID = -3835269209945264431L;

    /**
     * 班次名称：一班、二班、次日晚班、次日早班
     */
    private String shiftName;

    /**
     * 班次开始时间
     */
    private Date startTime;

    /**
     * 班次结束时间
     */
    private Date endTime;

    public ShiftTimeWindowDTO() {
        super();
    }
    public ShiftTimeWindowDTO(String shiftName, Date startTime, Date endTime) {
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
