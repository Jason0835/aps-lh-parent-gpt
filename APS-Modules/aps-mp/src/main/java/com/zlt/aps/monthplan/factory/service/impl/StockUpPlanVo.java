package com.zlt.aps.monthplan.factory.service.impl;

import lombok.Getter;

import java.io.Serializable;

/**
 * 备货计算辅助类
 *
 * @author ZLT
 * @date 20250512
 */
@Getter
public class StockUpPlanVo implements Serializable {
    /**
     * 计划量
     */
    private Long planQty;
    /**
     * 备货量
     */
    private Long stockQty;
    /**
     * 剩余库存量
     */
    private Long leftOverQty;

    /**
     * 构建备货计划对象
     *
     * @param planQty  计划量
     * @param stockQty 备货量
     */
    public StockUpPlanVo(Long planQty, Long stockQty, Long leftOverQty) {
        this.planQty = planQty;
        this.stockQty = stockQty;
        this.leftOverQty = leftOverQty;
    }
}
