package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.constant.ProductionConstant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Sku日排产明细对象
 * 排产日、排产量，损耗量
 *
 * @author ZLT
 * @date 20250125
 */
@Slf4j
@Getter
public class DayProductionCapacityDetailHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 排产Sku
     */
    private String materialDesc;
    /**
     * 使用模具信息
     */
    private Set<String> productionMouldInfo;
    /**
     * 排产量
     */
    private Integer productionQty;
    /**
     * 损耗量
     */
    private Integer lossQty;

    /**
     * 构建空数据，初始化
     *
     * @param productionDay 排产日
     * @param materialDesc  排产Sku
     * @return
     */
    public static DayProductionCapacityDetailHelper createInitEmpty(Integer productionDay, String materialDesc) {
        DayProductionCapacityDetailHelper detail = new DayProductionCapacityDetailHelper();
        detail.productionDay = productionDay;
        detail.materialDesc = materialDesc;
        detail.productionMouldInfo = new HashSet<>();
        detail.productionQty = BigDecimal.ZERO.intValue();
        detail.lossQty = BigDecimal.ZERO.intValue();
        return detail;
    }

    /**
     * 增加排产数量
     *
     * @param doubleMouldCode 使用的模具
     * @param productionQty   排产量
     * @param lossQty         损耗量
     */
    public void addProductionQty(Set<String> doubleMouldCode, Integer productionQty, Integer lossQty) {
        if (CollectionUtils.isEmpty(doubleMouldCode) || ProductionConstant.DOUBLE_MOULD_PRODUCTION != doubleMouldCode.size()) {
            return;
        }
        Integer realProductionQty = BigDecimal.ZERO.intValue();
        if (null != productionQty && productionQty > BigDecimal.ZERO.intValue()) {
            realProductionQty = realProductionQty + productionQty;
        }
        if (null != lossQty && lossQty > BigDecimal.ZERO.intValue()) {
            realProductionQty = realProductionQty + lossQty;
        }
        if (realProductionQty <= BigDecimal.ZERO.intValue()) {
            return;
        }
        productionMouldInfo.addAll(doubleMouldCode);
        this.productionQty = this.productionQty + productionQty;
        this.lossQty = this.lossQty + lossQty;
    }

    /**
     * 释放排产量
     *
     * @param usedMouldCode 使用模具
     * @param productionQty 排产量
     * @param lossQty       损耗量
     */
    public void deductionProductionQty(Set<String> usedMouldCode, Integer productionQty, Integer lossQty) {
        if (CollectionUtils.isEmpty(usedMouldCode)) {
            return;
        }
        boolean isNoAllUsed = false;
        for (String singleMouldCode : usedMouldCode) {
            if (!productionMouldInfo.contains(singleMouldCode)) {
                isNoAllUsed = true;
                break;
            }
        }
        if (isNoAllUsed) {
            return;
        }
        this.productionQty = this.productionQty - productionQty;
        this.lossQty = this.lossQty - lossQty;
        if (this.productionQty < BigDecimal.ZERO.intValue()) {
            this.productionQty = BigDecimal.ZERO.intValue();
        }
        if (this.lossQty < BigDecimal.ZERO.intValue()) {
            this.lossQty = BigDecimal.ZERO.intValue();
        }
    }

}
