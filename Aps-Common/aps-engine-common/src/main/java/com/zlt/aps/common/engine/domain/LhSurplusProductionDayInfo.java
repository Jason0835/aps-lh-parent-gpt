package com.zlt.aps.common.engine.domain;

import lombok.Getter;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

/**
 * Sku硫化余量计算
 * 排产日相关参数信息
 *
 * @author ZLT
 * @date 20260830
 */
@Getter
public class LhSurplusProductionDayInfo implements Serializable {
    /**
     * 当前排产月份
     */
    private YearMonth productionYearMonth;
    /**
     * 计划计算起始日，正常为1
     */
    private Integer startDay;
    /**
     * 排产周期排产日集合(3天8个班)
     */
    private List<Date> realProductionCycleList;
    /**
     * 最大间断天数
     */
    private Integer maxDiscontinueDays;

    /**
     * 构造函数：排产日相关信息参数
     *
     * @param productionYearMonth     当前排产月份
     * @param startDay                计划计算起始日，正常为1
     * @param realProductionCycleList 排产周期排产日集合(3天8个班)
     * @param maxDiscontinueDays      最大间断天数
     */
    public LhSurplusProductionDayInfo(YearMonth productionYearMonth, Integer startDay, List<Date> realProductionCycleList, Integer maxDiscontinueDays) {
        this.productionYearMonth = productionYearMonth;
        this.startDay = startDay;
        this.realProductionCycleList = realProductionCycleList;
        this.maxDiscontinueDays = maxDiscontinueDays;
    }
}
