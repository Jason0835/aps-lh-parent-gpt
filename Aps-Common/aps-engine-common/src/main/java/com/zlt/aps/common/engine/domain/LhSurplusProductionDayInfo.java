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
     * 是否含上个月超欠产
     */
    private boolean addLastMonthOverdueQty;
    /**
     * 排产周期排产日集合(3天8个班)
     */
    private List<Date> realProductionCycleList;
    /**
     * 提前排产：最大延长日
     */
    private Integer extendDays;
    /**
     * Sku最大间隔天数
     */
    private Integer skuMaxDiscontinueDays;

    /**
     * 构造函数：排产日相关信息参数
     *
     * @param productionYearMonth     当前排产月份
     * @param startDay                计划计算起始日，正常为1
     * @param addLastMonthOverdueQty
     * @param realProductionCycleList 排产周期排产日集合(3天8个班)
     * @param extendDays              提前生产天数
     * @param skuMaxDiscontinueDays   Sku连续排产最大间隔天数
     */
    public LhSurplusProductionDayInfo(YearMonth productionYearMonth,
                                      Integer startDay,
                                      boolean addLastMonthOverdueQty,
                                      List<Date> realProductionCycleList,
                                      Integer extendDays,
                                      Integer skuMaxDiscontinueDays) {
        this.productionYearMonth = productionYearMonth;
        this.startDay = startDay;
        this.addLastMonthOverdueQty = addLastMonthOverdueQty;
        this.realProductionCycleList = realProductionCycleList;
        this.extendDays = extendDays;
        this.skuMaxDiscontinueDays = skuMaxDiscontinueDays;
    }
}
