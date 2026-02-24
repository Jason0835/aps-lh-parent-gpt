package com.zlt.aps.factory.handler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 日排产量计算结果辅助类
 *
 * @author ZLT
 * @date 20260114
 */
@Getter
@Slf4j
public class DayProductionQtyHelper implements Serializable {
    /**
     * 排产天
     */
    private Integer productionDay;
    /**
     * 是否需要隔天换模
     */
    private boolean isProductionNextDay;
    /**
     * 排产量，换模为8
     * 换活字块，需要看前一个Sku的排产量
     */
    private Integer productionQty;
    /**
     * 当前天损耗量
     */
    private Integer lossQty;
    /**
     * 如果是隔天换模，则下一天的损耗量
     */
    private Integer nextDayLossQty;
    /**
     * 是否完毕
     */
    private boolean isFinish;

    /**
     * 构建天的排产量
     *
     * @param productionDay       排产日
     * @param isProductionNextDay 是否需要隔天排产
     * @param productionQty       排产量
     * @param lossQty             损耗量
     * @param nextDayLossQty      隔天损耗量
     * @param isFinish            是否排产完毕
     */
    public DayProductionQtyHelper(Integer productionDay, boolean isProductionNextDay, Integer productionQty, Integer lossQty, Integer nextDayLossQty, boolean isFinish) {
        this.productionDay = productionDay;
        this.isProductionNextDay = isProductionNextDay;
        this.productionQty = productionQty;
        this.lossQty = lossQty;
        this.nextDayLossQty = nextDayLossQty;
        this.isFinish = isFinish;
    }
}
