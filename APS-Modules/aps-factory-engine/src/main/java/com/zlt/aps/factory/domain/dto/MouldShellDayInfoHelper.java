package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模壳的日信息对象
 * 模壳日数量限制
 * 模块日使用量
 *
 * @author ZLT
 * @date 20260116
 */
@Getter
public class MouldShellDayInfoHelper implements Serializable {
    /**
     * 模套型号
     */
    private String mouldSetCode;
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 模壳数量
     */
    private Integer maxLimitQty;
    /**
     * 已使用量
     */
    private Integer usedQty;

    /**
     * 创建初始化对象
     *
     * @param mouldSetCode  模壳型号
     * @param productionDay 排产日
     * @param maxLimitQty   最大数量
     * @return
     */
    public static MouldShellDayInfoHelper buildInit(String mouldSetCode, Integer productionDay, Integer maxLimitQty) {
        return new MouldShellDayInfoHelper(mouldSetCode, productionDay, maxLimitQty);
    }

    /**
     * 获取剩余可使用量
     */
    public Integer getLeftOverUsedQty() {
        if (null == maxLimitQty) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == usedQty) {
            return maxLimitQty;
        }
        if (maxLimitQty <= usedQty) {
            return BigDecimal.ZERO.intValue();
        }
        return maxLimitQty - usedQty;
    }

    /**
     * 模壳使用量 + 1
     */
    public void addUsedCount() {
        Integer existUsedQty = usedQty;
        if (null == existUsedQty) {
            existUsedQty = BigDecimal.ZERO.intValue();
        }
        existUsedQty = existUsedQty + BigDecimal.ONE.intValue();
        usedQty = existUsedQty;
    }

    /**
     * 模壳使用量 - 1
     */
    public void deductionUsedCount() {
        Integer existUsedQty = usedQty;
        if (null == existUsedQty) {
            return;
        }
        existUsedQty = existUsedQty - BigDecimal.ONE.intValue();
        usedQty = existUsedQty;
    }

    /**
     * 清空模壳使用量
     */
    public void clearUsedCount() {
        usedQty = BigDecimal.ZERO.intValue();
    }

    /**
     * 构造函数
     *
     * @param mouldSetCode
     * @param productionDay
     * @param maxLimitQty
     */
    MouldShellDayInfoHelper(String mouldSetCode, Integer productionDay, Integer maxLimitQty) {
        this.mouldSetCode = mouldSetCode;
        this.productionDay = productionDay;
        this.maxLimitQty = maxLimitQty;
        this.usedQty = BigDecimal.ZERO.intValue();
    }

}
