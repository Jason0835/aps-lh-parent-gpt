package com.zlt.aps.common.engine.domain;

import lombok.Getter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Sku硫化余量计算
 * 排产日相关参数信息
 *
 * @author ZLT
 * @date 20261103
 */
@Getter
public class LhSkuSurplusControlParam implements Serializable {
    /**
     * 计划起始日：最新库存抓取日
     */
    private Date planStartDate;
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
     * 构造函数
     *
     * @param planStartDate           库存抓取日：余量计算的起始日
     * @param addLastMonthOverdueQty  是否加入上月超欠产
     * @param realProductionCycleList 排程日期集合
     * @param extendDays              扩展天数
     * @param skuMaxDiscontinueDays   Sku最大间断天数
     */
    public LhSkuSurplusControlParam(Date planStartDate, boolean addLastMonthOverdueQty, List<Date> realProductionCycleList, Integer extendDays, Integer skuMaxDiscontinueDays) {
        this.planStartDate = planStartDate;
        this.addLastMonthOverdueQty = addLastMonthOverdueQty;
        this.realProductionCycleList = realProductionCycleList;
        this.extendDays = extendDays;
        this.skuMaxDiscontinueDays = skuMaxDiscontinueDays;
    }
}
