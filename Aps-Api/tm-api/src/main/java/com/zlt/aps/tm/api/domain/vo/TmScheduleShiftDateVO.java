package com.zlt.aps.tm.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎面排程班次日期VO
 * 用于前端列表表头动态展示班次对应的日期
 *
 * @author APS
 */
@Data
public class TmScheduleShiftDateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次序号（1～6） */
    private int shift;

    /** 班次类型：night=夜班, morning=早班, afternoon=中班 */
    private String shiftType;

    /** 班次对应日期展示，格式 MM/dd，如 06/23 */
    private String shiftDate;

    /** 班次实际开始时间，用于前端禁用已经开始的插单班次。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date shiftStartTime;
}
