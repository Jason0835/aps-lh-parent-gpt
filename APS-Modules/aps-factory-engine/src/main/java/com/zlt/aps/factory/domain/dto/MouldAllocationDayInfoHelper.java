package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模具分配比例日控制信息对象
 * 模壳日数量限制
 * 模块日使用量
 *
 * @author ZLT
 * @date 20260116
 */
@Getter
public class MouldAllocationDayInfoHelper implements Serializable {
    /**
     * 控制维度 结构 + 主花纹
     */
    private String controlDimension;
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
     * @param controlDimension 控制维度
     * @param productionDay    排产日
     * @param maxLimitQty      最大数量
     * @return
     */
    public static MouldAllocationDayInfoHelper buildInit(String controlDimension, Integer productionDay, Integer maxLimitQty) {
        return new MouldAllocationDayInfoHelper(controlDimension, productionDay, maxLimitQty);
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
     * 使用量 + 1
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
     * 使用量 - 1
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
     * 清空使用量
     */
    public void clearUsedCount() {
        usedQty = BigDecimal.ZERO.intValue();
    }

    /**
     * 构造函数
     *
     * @param controlDimension 控制维度
     * @param productionDay    控制日
     * @param maxLimitQty      最大数量
     */
    MouldAllocationDayInfoHelper(String controlDimension, Integer productionDay, Integer maxLimitQty) {
        this.controlDimension = controlDimension;
        this.productionDay = productionDay;
        this.maxLimitQty = maxLimitQty;
        this.usedQty = BigDecimal.ZERO.intValue();
    }

}
