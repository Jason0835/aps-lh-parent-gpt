package com.zlt.aps.mp.engine.daylimit;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工厂模壳台账信息
 *
 * @author ZLT
 * @date 20251217
 */
@Data
public class MouldShellBaseInfoVo implements Serializable {

    /**
     * 工厂编码
     */
    private String factoryCode;

    /**
     * 模套型号-模壳
     */
    private String mouldSetCode;

    /**
     * 在机数量
     */
    private Integer machineQty;

    /**
     * 在库数量
     */
    private Integer onHandQty;
    /**
     * 总数量
     */
    private Integer totalQty;

    /**
     * 模块日限制信息集合
     */
    private Map<Integer, MouldShellDayInfoHelper> dayLimitInfoMap;

    /**
     * 创建无限制的模壳实例
     *
     * @param mouldSetCode
     * @return
     */
    public static MouldShellBaseInfoVo createNoLimit(String mouldSetCode) {
        MouldShellBaseInfoVo noLimit = new MouldShellBaseInfoVo();
        noLimit.setMouldSetCode(mouldSetCode);
        noLimit.setMachineQty(BigDecimal.ZERO.intValue());
        noLimit.setOnHandQty(Integer.MAX_VALUE);
        noLimit.setTotalQty(Integer.MAX_VALUE);
        return noLimit;
    }

    /**
     * 获取剩余使用量的模壳数量
     * 续作Sku场景，以第一天的排产量来确定
     *
     * @return
     */
    public Integer getLeftOverUsedQtyByContinueSku() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return BigDecimal.ZERO.intValue();
        }
        Optional<Map.Entry<Integer, MouldShellDayInfoHelper>> minEntry = dayLimitInfoMap.entrySet().stream().min(Map.Entry.comparingByKey());
        MouldShellDayInfoHelper dayLimit = minEntry.get().getValue();
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
        List<MouldShellDayInfoHelper> dayLimitList = dayLimitInfoMap.values().stream().collect(Collectors.toList());
        List<MouldShellDayInfoHelper> enableList = dayLimitList.stream().filter(singleDay -> singleDay.getLeftOverUsedQty() >= ProductionConstant.DOUBLE_MOULD_PRODUCTION).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enableList)) {
            return new HashSet<>();
        }
        return enableList.stream().map(MouldShellDayInfoHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 清空模壳的每日使用量
     */
    public void clearDayUsed() {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        dayLimitInfoMap.forEach((productionDay, dayLimit) -> dayLimit.clearUsedCount());
    }

    /**
     * 在productionDay天，模壳使用量+1
     *
     * @param productionDay 排产日
     * @param mouldCode     型腔模号
     */
    public void addUsedCount(Integer productionDay, String mouldCode) {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        MouldShellDayInfoHelper dayLimit = dayLimitInfoMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addUsedCount(mouldCode);
    }

    /**
     * 在productionDay天，模壳使用量-1
     *
     * @param productionDay 排产日
     * @param mouldCode     型腔模号
     */
    public void deductionUsedCount(Integer productionDay, String mouldCode) {
        if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
            return;
        }
        MouldShellDayInfoHelper dayLimit = dayLimitInfoMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionUsedCount();
    }
}
