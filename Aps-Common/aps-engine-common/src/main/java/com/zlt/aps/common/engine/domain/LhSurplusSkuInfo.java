package com.zlt.aps.common.engine.domain;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Getter;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Sku硫化余量计算
 * Sku相关信息
 *
 * @author ZLT
 * @date 20260830
 */
@Getter
public class LhSurplusSkuInfo implements Serializable {
    /**
     * 计算的Sku对象
     */
    private FactoryMonthPlanProductionFinalResult skuInfo;
    /**
     * 对应的月计划信息
     */
    private List<FactoryMonthPlanProductionFinalResult> allMonthPlanList;
    /**
     * 对应的欠产信息
     */
    private Map<YearMonth, Integer> monthOverdueQtyMap;
    /**
     * 对应的完成量
     */
    private Integer finishedQty;
    /**
     * 对应的硫化日计划调整信息
     */
    private List<LhDayPlanAdjustVo> allLhDayAdjustList;

    /**
     * Sku余量计算-Sku数据相关信息
     *
     * @param skuInfo            计算的Sku对象
     * @param allMonthPlanList   对应的月计划信息
     * @param monthOverdueQtyMap 对应的欠产信息
     * @param finishedQty        对应的完成量
     * @param allLhDayAdjustList 对应的硫化日计划调整信息
     */
    public LhSurplusSkuInfo(FactoryMonthPlanProductionFinalResult skuInfo, List<FactoryMonthPlanProductionFinalResult> allMonthPlanList, Map<YearMonth, Integer> monthOverdueQtyMap, Integer finishedQty, List<LhDayPlanAdjustVo> allLhDayAdjustList) {
        this.skuInfo = skuInfo;
        this.allMonthPlanList = allMonthPlanList;
        this.monthOverdueQtyMap = monthOverdueQtyMap;
        this.finishedQty = finishedQty;
        this.allLhDayAdjustList = allLhDayAdjustList;
    }
}
