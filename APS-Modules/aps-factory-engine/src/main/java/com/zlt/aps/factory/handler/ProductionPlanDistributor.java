package com.zlt.aps.factory.handler;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工厂排产计划排产量分配器
 *
 * @author ZLT
 * @date 20251231
 */
public class ProductionPlanDistributor {

    /**
     *
     *
     * @param realDayProductionQty
     * @param continueSkuPlanList
     * @return
     */
    public Map<Long, Long> allocationProductionQty(Long realDayProductionQty, List<MonthPlanProductionRequirePlanVo> continueSkuPlanList) {
        if (CollectionUtils.isEmpty(continueSkuPlanList)) {
            return Collections.emptyMap();
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = continueSkuPlanList.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return Collections.emptyMap();
        }
        //todo 怎么分配
        Map<Long, Long> realDeductionMap = new HashMap<>();
        //先高优先级，再其他净需求
        List<MonthPlanProductionRequirePlanVo> heightPlanList = hasProductionList.stream().filter(groupPlan -> groupPlan.getHeightProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        realDayProductionQty = deductionHeightProductionQty(heightPlanList, realDeductionMap, realDayProductionQty);
        if (realDayProductionQty <= BigDecimal.ZERO.longValue()) {
            return realDeductionMap;
        }
        //再其它净需求
        List<MonthPlanProductionRequirePlanVo> noHeightPlanList = hasProductionList.stream().filter(groupPlan -> groupPlan.getProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        deductionNoHeightQty(noHeightPlanList, realDeductionMap, realDayProductionQty);
        return realDeductionMap;
    }


    /**
     * 扣减高优先级待排产量
     *
     * @param heightPlanList
     * @param realDeductionMap
     * @param realDayProductionQty
     * @return
     */
    private Long deductionHeightProductionQty(List<MonthPlanProductionRequirePlanVo> heightPlanList, Map<Long, Long> realDeductionMap, Long realDayProductionQty) {
        if (CollectionUtils.isEmpty(heightPlanList)) {
            return realDayProductionQty;
        }
        //高优先级量降序排序
        heightPlanList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getHeightProductionQty, Comparator.reverseOrder()));
        for (MonthPlanProductionRequirePlanVo productionPlan : heightPlanList) {
            if (realDayProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            Long monthPlanId = productionPlan.getMonthPlanId();
            Long heightProductionQty = productionPlan.getHeightProductionQty();
            if (heightProductionQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            Long realDeductionQty = Math.min(heightProductionQty, realDayProductionQty);
            if (realDeductionQty > BigDecimal.ZERO.longValue()) {
                //扣减计划需求量，并汇总计划总扣减量
                deductionHeightProductionQty(productionPlan, realDeductionQty);
                Long sumDeductionQty = realDeductionMap.get(monthPlanId);
                if (null == sumDeductionQty) {
                    sumDeductionQty = BigDecimal.ZERO.longValue();
                }
                sumDeductionQty = sumDeductionQty + realDeductionQty;
                realDeductionMap.put(monthPlanId, sumDeductionQty);
            }
            realDayProductionQty = realDayProductionQty - realDeductionQty;
        }
        return realDayProductionQty;
    }

    /**
     * 扣减非高优先级净需求
     *
     * @param noHeightPlanList     非高优先级需求计划
     * @param realDeductionMap     计划总扣减量
     * @param realDayProductionQty 需扣减量
     * @return
     */
    private Long deductionNoHeightQty(List<MonthPlanProductionRequirePlanVo> noHeightPlanList, Map<Long, Long> realDeductionMap, Long realDayProductionQty) {
        if (realDayProductionQty <= BigDecimal.ZERO.longValue()) {
            return realDayProductionQty;
        }
        if (CollectionUtils.isEmpty(noHeightPlanList)) {
            return realDayProductionQty;
        }
        //非高优先级量降序排序
        noHeightPlanList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getProductionQty, Comparator.reverseOrder()));
        for (MonthPlanProductionRequirePlanVo productionPlan : noHeightPlanList) {
            if (realDayProductionQty <= BigDecimal.ZERO.longValue()) {
                break;
            }
            Long monthPlanId = productionPlan.getMonthPlanId();
            Long noHeightProductionQty = productionPlan.getProductionQty();
            if (noHeightProductionQty <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            Long realDeductionQty = Math.min(noHeightProductionQty, realDayProductionQty);
            if (realDeductionQty > BigDecimal.ZERO.longValue()) {
                //扣减计划需求量，并汇总计划总扣减量
                deductionNoHeightProductionQty(productionPlan, realDeductionQty);
                Long sumDeductionQty = realDeductionMap.get(monthPlanId);
                if (null == sumDeductionQty) {
                    sumDeductionQty = BigDecimal.ZERO.longValue();
                }
                sumDeductionQty = sumDeductionQty + realDeductionQty;
                realDeductionMap.put(monthPlanId, sumDeductionQty);
            }
            realDayProductionQty = realDayProductionQty - realDeductionQty;
        }
        return realDayProductionQty;
    }

    /**
     * 扣减高优先级量
     * 扣减高优先级需要同时扣减总需排产量
     *
     * @param productionPlan   计划
     * @param dayProductionQty 真实日排产量
     */
    private void deductionHeightProductionQty(MonthPlanProductionRequirePlanVo productionPlan, Long dayProductionQty) {
        Long heightProductionQty = productionPlan.getHeightProductionQty();
        Long productionQty = productionPlan.getProductionQty();
        heightProductionQty = heightProductionQty - dayProductionQty;
        productionQty = productionQty - dayProductionQty;
        productionPlan.setHeightProductionQty(heightProductionQty);
        productionPlan.setProductionQty(productionQty);
        if (productionQty <= BigDecimal.ZERO.longValue()) {
            productionPlan.setIsProduction(YesOrNoEnum.NO.getCode());
        }
    }

    /**
     * 扣减非高优先级净需求量
     * 只扣减总需求量的值
     *
     * @param productionPlan   计划
     * @param dayProductionQty 真实日排产量
     */
    private void deductionNoHeightProductionQty(MonthPlanProductionRequirePlanVo productionPlan, Long dayProductionQty) {
        Long productionQty = productionPlan.getProductionQty();
        productionQty = productionQty - dayProductionQty;
        productionPlan.setProductionQty(productionQty);
        if (productionQty <= BigDecimal.ZERO.longValue()) {
            productionPlan.setIsProduction(YesOrNoEnum.NO.getCode());
        }
    }
}
