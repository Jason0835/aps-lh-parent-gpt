package com.zlt.aps.factory.utils;

import com.google.common.collect.Lists;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
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
      String unProductionReason = productionPlan.getNoProductionReason();
      MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
      BeanUtils.copyProperties(productionPlan, noProductionPlan);
      noProductionPlan.setId(null);
      noProductionPlan.setReason(unProductionReason);
      int needProductionQty = productionPlan.getFactProdReqQty();
      int plannedQty = sumProductionMap.getOrDefault(monthPlanId, 0);
      int unProductionQty = needProductionQty - plannedQty;
      String isProduction = plannedQty > 0?YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode();
      noProductionPlan.setIsProduction(isProduction);
      if(unProductionQty == 0) {
        return;
      }
      if(!sumProductionMap.containsKey(monthPlanId)) {
        noProductionPlan.setUnProductionQty(unProductionQty);
        noProductionPlanList.add(noProductionPlan);
        return;
      }
      //不排产计划
      if (!CollectionUtils.isEmpty(noProductionRecordMap) && noProductionRecordMap.containsKey(monthPlanId)) {
        noProductionPlan.setUnProductionQty(needProductionQty);
        noProductionPlanList.add(noProductionPlan);
      }
    });
    return noProductionPlanList;
  }

  /**
   * 提取不排产计划条件
   * isProduction = 0 或是 可排产量为0
   *
   * @param monthPlanInit
   * @return
   */
  private static boolean hasNoProduction(MonthPlanProductionRequirePlanVo monthPlanInit) {
    if (null == monthPlanInit) {
      return false;
    }
    if (YesOrNoEnum.NO.getCode().equals(monthPlanInit.getIsProduction())) {
      return true;
    }
    Integer productionQty = monthPlanInit.getProductionQty();
    if (null == productionQty) {
      productionQty = BigDecimal.ZERO.intValue();
    }
    return productionQty <= BigDecimal.ZERO.intValue();
  }

  public static List<MonthPlanNoProductionPlan> createNoProductionRecordData(List<MonthPlanProductionRequirePlanVo> requirePlanList) {
    List<MonthPlanNoProductionPlan> factoryNoProductionPlanList = new ArrayList<>();
    requirePlanList.stream().filter(monthPlanInit -> hasNoProduction(monthPlanInit)).forEach(monthPlanInit -> {
      MonthPlanNoProductionPlan noProductionRecord = new MonthPlanNoProductionPlan();
      BeanUtils.copyProperties(monthPlanInit, noProductionRecord);
      noProductionRecord.setId(null);
      noProductionRecord.setReason(monthPlanInit.getNoProductionReason());
      factoryNoProductionPlanList.add(noProductionRecord);
    });
    return factoryNoProductionPlanList;
  }
}
