package com.zlt.aps.mp.api.domain.deduct;

import lombok.Data;

/**
 * 降模排产参数传递上下文
 */
@Data
public class DeductMouldContext {

    /**
     * 当前日期
     */
    private Integer currentDate;

    /**
     * 收尾日
     */
    private Integer deadLineDate;

    /**
     * 预计收尾天数
     */
    private Integer expectedDays;

    /**
     * 前日计划量
     */
    private Integer preDayQty;

    /**
     * 前日机台数
     */
    private Integer preDayMachines;

    /**
     * 前日剩余量
     */
    private Integer preRemainQty;

}
