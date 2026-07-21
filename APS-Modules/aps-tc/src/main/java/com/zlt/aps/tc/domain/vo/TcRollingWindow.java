package com.zlt.aps.tc.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 胎侧自动滚动命中的排程班次窗口。
 */
@Data
public class TcRollingWindow {

    /** 工厂编码。 */
    private String factoryCode;

    /** 六班结果归属排程日期。 */
    private Date scheduleDate;

    /** 待调整班次顺序。 */
    private Integer targetShiftOrder;

    /** 班次实际开始时间。 */
    private Date shiftStartTime;
}
