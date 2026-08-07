package com.zlt.aps.tq.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 胎圈自动滚动命中的排程班次窗口。
 *
 * <p>对齐胎面 TmRollingWindow，承载窗口识别结果，
 * 用于在 TqAutoRollingApplicationService 中执行库存同步、校验、滚动。</p>
 *
 * @author APS
 */
@Data
public class TqRollingWindow {

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
