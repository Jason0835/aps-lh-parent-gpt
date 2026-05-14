package com.zlt.aps.mp.engine.domain.vo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Set;

/**
 * 排产Sku比较对象
 *
 * @author ZLT
 * @date 20260403
 */
@Slf4j
@Getter
public class ProductionSkuPriorityVo implements Serializable {
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 硫化组最大可排产天数
     */
    private Integer maxLhDays;
    /**
     * 最大模具可排产天数
     */
    private Integer maxMouldDays;
    /**
     * 计划还需排产天数
     */
    private Integer needDays;
    /**
     * 是否供应链优先-即物料优先
     */
    private boolean hasSupplyChainPriority;
    /**
     * 排产日集合
     */
    private Set<Integer> productionDaySet;

    /**
     * 构造函数
     *
     * @param materialDesc 物料描述
     * @param maxLhDays    lh组最大可排产天数
     * @param maxMouldDays 模具最大可排产天数
     * @param needDays     还需排产天数
     */
    public ProductionSkuPriorityVo(String materialDesc, Set<Integer> productionDaySet, Integer maxLhDays, Integer maxMouldDays, Integer needDays, boolean hasSupplyChainPriority) {
        this.materialDesc = materialDesc;
        this.productionDaySet = productionDaySet;
        this.maxLhDays = maxLhDays;
        this.maxMouldDays = maxMouldDays;
        this.needDays = needDays;
        this.hasSupplyChainPriority = hasSupplyChainPriority;
    }

    /**
     * 产能可覆盖
     *
     * @return
     */
    public boolean isCovered() {
        Integer effectiveDays = Math.min(maxLhDays, maxMouldDays);
        if (effectiveDays < needDays) {
            return false;
        }
        return true;
    }

    /**
     * 在产能不可覆盖的情形下，获取剩余还需排产天数
     *
     * @return
     */
    public Integer getDiffValueByNoCovered() {
        Integer effectiveDays = Math.min(maxLhDays, maxMouldDays);
        return Math.abs(needDays - effectiveDays);
    }

    /**
     * 是否具有相同排产日范围
     *
     * @param compareObject
     * @return
     */
    public boolean isSameProductionDay(ProductionSkuPriorityVo compareObject) {
        if (null == compareObject) {
            return false;
        }
        if (CollectionUtils.isEmpty(productionDaySet) || CollectionUtils.isEmpty(compareObject.getProductionDaySet())) {
            return false;
        }
        return productionDaySet.equals(compareObject.getProductionDaySet());
    }
}
