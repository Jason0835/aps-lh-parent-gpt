package com.zlt.aps.factory.domain.vo;

import lombok.Getter;

import java.io.Serializable;

/**
 * 日产能限制控制
 * 包含：实际排产量、实际消耗产能量
 * 按天计算，如是单模，则单模，如是双模计算
 *
 * @author ZLT
 * @date 20250828
 */
@Getter
public class DayProductionCapacityParityVo implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDate;
    /**
     * 实际排产量
     */
    private Long realProductionQty;
    /**
     * 实际预占产能数量
     * 并不一定等于realProductionQty
     * 如果有换规格、洗模消耗时，则≠realProductionQty
     */
    private Long realPreemptionQty;

    /**
     * 构造日产能对等控制计算对象-辅助类
     *
     * @param productionDate    排产日
     * @param realProductionQty 实际排产量
     * @param realPreemptionQty 跨天消耗的时间
     */
    public DayProductionCapacityParityVo(Integer productionDate, Long realProductionQty, Long realPreemptionQty) {
        this.productionDate = productionDate;
        this.realProductionQty = realProductionQty;
        this.realPreemptionQty = realPreemptionQty;
    }
}
