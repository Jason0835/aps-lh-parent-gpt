package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 胎面自动滚动命中的排程班次窗口。
 */
@Data
public class TmRollingWindow {

    /** 工厂编码。 */
    private String factoryCode;

    /** 六班结果归属排程日期。 */
    private Date scheduleDate;

    /** MES库存物理日期。 */
    private Date stockDate;

    /** 待调整班次顺序。 */
    private Integer targetShiftOrder;

    /** 班次实际开始时间。 */
    private Date shiftStartTime;
}
