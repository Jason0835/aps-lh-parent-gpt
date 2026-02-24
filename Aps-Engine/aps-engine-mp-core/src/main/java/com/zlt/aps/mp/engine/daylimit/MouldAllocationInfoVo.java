package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工厂模具分配比例
 *
 * @author ZLT
 * @date 20251217
 */
@Data
public class MouldAllocationInfoVo implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 花纹
     */
    private String pattern;
    /**
     * 结构名
     */
    private String structureName;
    /**
     * 规格
     */
    private String specifications;
    /**
     * 分配数量
     */
    private Integer allocationQty;

    /**
     * 日限制信息集合
     */
    private Map<Integer, MouldAllocationDayInfoHelper> dayLimitInfoMap;

    /**
     * 业务重复键：结构|*|主花纹
     *
     * @return
     */
    public String getDuplicateKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, structureName, mainPattern);
    }

    /**
     * 获取剩余使用量的模具数量
     * 续作Sku场景，以第一天的排产量来确定
     *
     * @return
     */
    public Integer getLeftOverUsedQtyByContinueSku() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Optional<Map.Entry<Integer, MouldAllocationDayInfoHelper>> minEntry = dayLimitInfoMap.entrySet().stream().min(Map.Entry.comparingByKey());
        MouldAllocationDayInfoHelper dayLimit = minEntry.get().getValue();
        if (null == dayLimit) {
            return BigDecimal.ZERO.intValue();
        }
        return dayLimit.getLeftOverUsedQty();
    }

    /**
     * 获取可进行两副模具的排产日范围
     *
     * @return
     */
    public Set<Integer> getEnableDoubleMouldProductionRange() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return new HashSet<>();
        }
        List<MouldAllocationDayInfoHelper> dayLimitList = dayLimitInfoMap.values().stream().collect(Collectors.toList());
        List<MouldAllocationDayInfoHelper> enableList = dayLimitList.stream().filter(singleDay -> singleDay.getLeftOverUsedQty() >= ProductionConstant.DOUBLE_MOULD_PRODUCTION).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enableList)) {
            return new HashSet<>();
        }
        return enableList.stream().map(MouldAllocationDayInfoHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 清空模具分配比例的每日使用量
     */
    public void clearDayUsed() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        dayLimitInfoMap.forEach((productionDay, dayLimit) -> dayLimit.clearUsedCount());
    }

    /**
     * 在productionDay天，模具使用量+1
     *
     * @param productionDay 排产日
     * @param mouldCode     型腔模号
     */
    public void addUsedCount(Integer productionDay, String mouldCode) {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        MouldAllocationDayInfoHelper dayLimit = dayLimitInfoMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addUsedCount(mouldCode);
    }

    /**
     * 在productionDay天，模具使用量-1
     *
     * @param productionDay 排产日
     * @param mouldCode     型腔模号
     */
    public void deductionUsedCount(Integer productionDay, String mouldCode) {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        MouldAllocationDayInfoHelper dayLimit = dayLimitInfoMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionUsedCount(mouldCode);
    }
}
