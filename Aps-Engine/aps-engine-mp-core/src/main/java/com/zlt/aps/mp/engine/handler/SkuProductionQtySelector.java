package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ProductionQtyModelEnum;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SkuNeedProductionInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sku排产量选择器
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260403
 */
@Slf4j
public class SkuProductionQtySelector {

    /**
     * 从分组计划中获取选中Sku(selectedMaterialDesc)还需排产量
     * 如果需整个排产，则为所有未排量，否则先排产高优级量
     *
     * @param productionPlanList   分组排产计划(TBR-结构名)
     * @param selectedMaterialDesc 选中的Sku
     * @param isAllSum             是否都一起排
     * @return
     */
    public static SkuNeedProductionInfo getNeedProductionQty(List<MonthPlanProductionRequirePlanVo> productionPlanList, String selectedMaterialDesc, boolean isAllSum) {
        if (CollectionUtils.isEmpty(productionPlanList) || StringUtils.isBlank(selectedMaterialDesc)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> selectedPlanList = productionPlanList.stream().filter(plan -> plan.hasThisRoundSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selectedPlanList)) {
            return null;
        }
        //需按净需求一起排产
        if (YesOrNoEnum.YES.getValue().equals(selectedPlanList.get(BigDecimal.ZERO.intValue()).getIsProductionBySum())) {
            return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, selectedPlanList);
        }
        //是否有供应链优先标记
        List<MonthPlanProductionRequirePlanVo> hasPrioritizeList = selectedPlanList.stream().filter(plan -> plan.hasPrioritizeQty()).collect(Collectors.toList());
        //供应链优先
        if (!CollectionUtils.isEmpty(hasPrioritizeList)) {
            return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, hasPrioritizeList);
        }
        //是否有高优级排产量
        List<MonthPlanProductionRequirePlanVo> heightList = selectedPlanList.stream().filter(plan -> plan.getHeightProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        //高优先级优先
        if (!CollectionUtils.isEmpty(heightList)) {
            if (isAllSum) {
                //20260329 只要挑选上来，就一起排
                return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, selectedPlanList);
            }
            return new SkuNeedProductionInfo(ProductionQtyModelEnum.HEIGHT_QTY, heightList);
        }
        return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, selectedPlanList);
    }


}
