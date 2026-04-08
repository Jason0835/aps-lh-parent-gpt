package com.zlt.aps.mp.engine.domain.vo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

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
     * 构造函数
     *
     * @param materialDesc 物料描述
     * @param maxLhDays    lh组最大可排产天数
     * @param maxMouldDays 模具最大可排产天数
     * @param needDays     还需排产天数
     */
    public ProductionSkuPriorityVo(String materialDesc, Integer maxLhDays, Integer maxMouldDays, Integer needDays) {
        this.materialDesc = materialDesc;
        this.maxLhDays = maxLhDays;
        this.maxMouldDays = maxMouldDays;
        this.needDays = needDays;
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

}
