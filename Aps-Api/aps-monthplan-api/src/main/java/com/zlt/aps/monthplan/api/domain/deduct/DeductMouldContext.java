package com.zlt.aps.monthplan.api.domain.deduct;

import lombok.Data;

import java.time.LocalDate;

/**
 * 降模排产参数传递上下文
 */
@Data
public class DeductMouldContext {

    /**
     * 当前日期
     */
    private LocalDate currentDate;

    /**
     * 收尾日
     */
    private LocalDate deadLineDate;

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

    /**
     * 参数：分配的机台数，默认3
     */
    private Integer paramAssignedMachines = 3;

    /**
     * 参数：临近收尾天数7天，默认7
     */
    private Integer paramNearDeadline7 = 7;

    /**
     * 参数：临近收尾天数7天，降低的台数，默认3台
     */
    private Integer paramReduceMachines3 = 3;

    /**
     * 参数：临近收尾天数5天，默认5
     */
    private Integer paramNearDeadline5 = 5;

    /**
     * 参数：临近收尾天数5天，降低的台数，默认2台
     */
    private Integer paramReduceMachines2 = 2;

    /**
     * 参数：临近收尾天数2天，默认2
     */
    private Integer paramNearDeadline2 = 2;

    /**
     * 参数：临近收尾天数2天，降低的台数，默认1台
     */
    private Integer paramReduceMachines1 = 1;
}
