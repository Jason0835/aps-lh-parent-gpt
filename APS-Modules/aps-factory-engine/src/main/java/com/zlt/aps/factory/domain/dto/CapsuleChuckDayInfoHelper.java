package com.zlt.aps.factory.domain.dto;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 胶囊卡盘日排产限制对象
 * 卡盘数量
 * 日使用量
 *
 * @author ZLT
 * @date 20260119
 */
@Getter
public class CapsuleChuckDayInfoHelper implements Serializable {
    /**
     * 胶囊卡盘分组Id
     */
    private String groupId;
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 卡盘总数
     */
    private Integer maxLimitQty;
    /**
     * 已使用量
     */
    private Integer usedQty;
    /**
     * 已使用模具--模具对应胶囊使用
     */
    private Set<String> usedMouldSet;

    /**
     * 创建初始化对象
     *
     * @param groupId       胶囊卡盘
     * @param productionDay 排产日
     * @param maxLimitQty   最大数量
     * @return
     */
    public static CapsuleChuckDayInfoHelper buildInit(String groupId, Integer productionDay, Integer maxLimitQty) {
        return new CapsuleChuckDayInfoHelper(groupId, productionDay, maxLimitQty);
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
     * 胶囊使用量 + 1
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
     * @param groupId       分组Id
     * @param productionDay 排产日
     * @param maxLimitQty   最大量
     */
    CapsuleChuckDayInfoHelper(String groupId, Integer productionDay, Integer maxLimitQty) {
        this.groupId = groupId;
        this.productionDay = productionDay;
        this.maxLimitQty = maxLimitQty;
        this.usedQty = BigDecimal.ZERO.intValue();
        this.usedMouldSet = new HashSet<>();
    }

}
