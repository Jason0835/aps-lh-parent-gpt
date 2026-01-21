package com.zlt.aps.factory.daylimit;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 轮胎成型鼓日排产限制对象
 * 总数量
 * 日使用量
 *
 * @author ZLT
 * @date 20260119
 */
@Getter
public class TireDrumDayInfoHelper implements Serializable {
    /**
     * 鼓分组Id
     */
    private String groupId;
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 总数
     */
    private Integer maxLimitQty;
    /**
     * 已使用量
     */
    private Integer usedQty;
    /**
     * 已使用成型机台
     */
    private Set<String> usedCxMachineSet;

    /**
     * 创建初始化对象
     *
     * @param groupId       胶囊卡盘
     * @param productionDay 排产日
     * @param maxLimitQty   最大数量
     * @return
     */
    public static TireDrumDayInfoHelper buildInit(String groupId, Integer productionDay, Integer maxLimitQty) {
        return new TireDrumDayInfoHelper(groupId, productionDay, maxLimitQty);
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
     * 鼓使用量 + 1
     *
     * @param cxMachineCode 成型机台
     */
    public void addUsedCount(String cxMachineCode) {
        if (StringUtils.isBlank(cxMachineCode)) {
            return;
        }
        if (usedCxMachineSet.contains(cxMachineCode)) {
            return;
        }
        usedCxMachineSet.add(cxMachineCode);
        Integer existUsedQty = usedQty;
        if (null == existUsedQty) {
            existUsedQty = BigDecimal.ZERO.intValue();
        }
        existUsedQty = existUsedQty + BigDecimal.ONE.intValue();
        usedQty = existUsedQty;
    }

    /**
     * 鼓使用量 - 1
     */
    public void deductionUsedCount(String cxMachineCode) {
        Integer existUsedQty = usedQty;
        if (null == existUsedQty) {
            return;
        }
        if (usedCxMachineSet.contains(cxMachineCode)) {
            usedCxMachineSet.remove(cxMachineCode);
        }
        existUsedQty = existUsedQty - BigDecimal.ONE.intValue();
        usedQty = existUsedQty;
    }

    /**
     * 清空鼓使用量
     */
    public void clearUsedCount() {
        usedQty = BigDecimal.ZERO.intValue();
        usedCxMachineSet = new HashSet<>();
    }

    /**
     * 构造函数
     *
     * @param groupId       分组Id
     * @param productionDay 排产日
     * @param maxLimitQty   最大量
     */
    TireDrumDayInfoHelper(String groupId, Integer productionDay, Integer maxLimitQty) {
        this.groupId = groupId;
        this.productionDay = productionDay;
        this.maxLimitQty = maxLimitQty;
        this.usedQty = BigDecimal.ZERO.intValue();
        this.usedCxMachineSet = new HashSet<>();
    }

}
