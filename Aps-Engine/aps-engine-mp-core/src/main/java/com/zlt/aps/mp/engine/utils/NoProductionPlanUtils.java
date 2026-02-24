package com.zlt.aps.mp.engine.utils;

import com.google.common.collect.Lists;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 不排产计划工具类
 * @author Yelq
 */
@Slf4j
public class NoProductionPlanUtils {
  public static List<MonthPlanNoProductionPlan> buildNoProductionPlanList(Map<Long, MonthPlanProductionRequirePlanVo> productionPlanMap, Map<Long, MonthPlanNoProductionPlan> noProductionRecordMap, Map<Long, Integer> sumProductionMap) {
    List<MonthPlanNoProductionPlan> noProductionPlanList = Lists.newArrayList();
    productionPlanMap.forEach((key, productionPlan) -> {
      Long monthPlanId = productionPlan.getMonthPlanId();
      int needProductionQty = productionPlan.getFactProdReqQty();
      int plannedQty = sumProductionMap.getOrDefault(monthPlanId, 0);
      int unProductionQty = needProductionQty - plannedQty;
      if(needProductionQty == 0 && YesOrNoEnum.YES.getCode().equals(productionPlan.getIsProduction())){
          return;
      }
      if(unProductionQty <= 0) {
        return;
      }
      String unProductionReason = productionPlan.getNoProductionReason();
      MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
      BeanUtils.copyProperties(productionPlan, noProductionPlan);
      noProductionPlan.setId(null);
      noProductionPlan.setReason(unProductionReason);
      String isProduction = plannedQty > 0?YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode();
      noProductionPlan.setIsProduction(isProduction);
      noProductionPlan.setUnProductionQty(unProductionQty);
      //有排产计划，则取排产数量
      if (sumProductionMap.containsKey(monthPlanId) && unProductionQty > BigDecimal.ZERO.intValue()) {
        noProductionPlanList.add(noProductionPlan);
        return;
      }
      //不排产计划
      if (!CollectionUtils.isEmpty(noProductionRecordMap) && noProductionRecordMap.containsKey(monthPlanId)) {
         MonthPlanNoProductionPlan  existNoNoProductionPlan =  noProductionRecordMap.get(monthPlanId);
         noProductionPlan.setReason(existNoNoProductionPlan.getReason());
         noProductionPlanList.add(noProductionPlan);
         return;
      }
      // 即没有排产计划，又不是不排产计划
      if (unProductionQty > BigDecimal.ZERO.intValue()) {
        noProductionPlanList.add(noProductionPlan);
      }
    });
    return noProductionPlanList;
  }

  public static MonthPlanNoProductionPlan createNoProductionRecordData(MonthPlanProductionRequirePlanVo monthPlanInit) {
    if(YesOrNoEnum.YES.getCode().equals(monthPlanInit.getIsProduction())) {
       return null;
    }
    MonthPlanNoProductionPlan noProductionRecord = new MonthPlanNoProductionPlan();
    BeanUtils.copyProperties(monthPlanInit, noProductionRecord);
    noProductionRecord.setId(null);
    noProductionRecord.setReason(monthPlanInit.getNoProductionReason());
    return noProductionRecord;
  }
}
