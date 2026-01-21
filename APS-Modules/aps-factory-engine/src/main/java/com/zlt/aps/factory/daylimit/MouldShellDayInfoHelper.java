package com.zlt.aps.factory.daylimit;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 模壳的日信息对象
 * 模壳日数量限制
 * 模壳日使用量
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
     * 已使用模具
     */
    private Set<String> usedMouldSet;

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
     *
     * @param mouldCode 型腔模号
     */
    public void addUsedCount(String mouldCode) {
        if (StringUtils.isBlank(mouldCode)) {
            return;
        }
        if (usedMouldSet.contains(mouldCode)) {
            return;
        }
        usedMouldSet.add(mouldCode);
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
        usedMouldSet = new HashSet<>();
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
        this.usedMouldSet = new HashSet<>();
    }

}
