package com.zlt.aps.factory.utils;

import com.google.common.collect.Lists;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
      Integer needProductionQty = productionPlan.getCxCapacityRequireQty();
      //有排产计划，则取排产数量
      if (sumProductionMap.containsKey(monthPlanId)) {
        Integer plannedQty = sumProductionMap.get(monthPlanId);
        int unProductionQty = needProductionQty - plannedQty;
        if (!needProductionQty.equals(plannedQty)) {
          noProductionPlan.setUnProductionQty(unProductionQty);
          noProductionPlanList.add(noProductionPlan);
        }
        return;
      }
      //不排产计划
      if (!CollectionUtils.isEmpty(noProductionRecordMap) && noProductionRecordMap.containsKey(monthPlanId)) {
        noProductionPlan.setUnProductionQty(needProductionQty);
        noProductionPlanList.add(noProductionPlan);
        return;
      }
      //即没有排产计划，又不是不排产计划
      Integer plannedQty = productionPlan.getProductionQty();
      int unProductionQty = needProductionQty - plannedQty;
      if (StringUtils.isNotBlank(unProductionReason)  && unProductionQty >= BigDecimal.ZERO.longValue()) {
        noProductionPlan.setUnProductionQty(unProductionQty);
        noProductionPlanList.add(noProductionPlan);
      }
    });
    return noProductionPlanList;
  }

  public static Map<String, FactoryMonthPlanMouldDayResult> convertToMaterialDescMap(
      List<FactoryMonthPlanMouldDayResult> dayResultList) {
    if(CollectionUtils.isEmpty(dayResultList)) {
      return Collections.emptyMap();
    }
    return dayResultList.stream()
        .filter(Objects::nonNull)
        .filter(result -> !StringUtils.isEmpty(result.getMaterialDesc()))
        .collect(Collectors.toMap(
            FactoryMonthPlanMouldDayResult::getMaterialCode,
            Function.identity(),
            (existing, replacement) -> existing
        ));
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
