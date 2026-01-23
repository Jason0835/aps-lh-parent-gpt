package com.zlt.aps.factory.utils;

import com.google.common.collect.Lists;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
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
public class NoProductionPlanUtils {
  public static List<MonthPlanNoProductionPlan> buildNoProductionPlanList(Map<Long, MonthPlanProductionRequirePlanVo> productionPlanMap, List<FactoryMonthPlanMouldDayResult> dayResultList, Map<Long, Integer> sumProductionMap) {
    List<MonthPlanNoProductionPlan> list = Lists.newArrayList();
    Map<String,FactoryMonthPlanMouldDayResult> mouldDayResultMap =  convertToMaterialDescMap(dayResultList);
    productionPlanMap.forEach((key, productionPlan) -> {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String unProductionReason = productionPlan.getNoProductionReason();
        MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
        BeanUtils.copyProperties(productionPlan, noProductionPlan);
        noProductionPlan.setBaseVale(null);
        noProductionPlan.setId(null);
        noProductionPlan.setReason(unProductionReason);
        Integer needProductionQty = productionPlan.getCxCapacityRequireQty();
        Integer plannedQty = sumProductionMap.getOrDefault(monthPlanId,0);
        int unProductionQty = needProductionQty - plannedQty;
        noProductionPlan.setUnProductionQty(unProductionQty);
        if(StringUtils.isBlank(productionPlan.getMaterialDesc()) || !mouldDayResultMap.containsKey(productionPlan.getMaterialDesc())){
          list.add(noProductionPlan);
          return;
        }
        FactoryMonthPlanMouldDayResult mouldDayResult = mouldDayResultMap.get(productionPlan.getMaterialDesc());
        if(null != mouldDayResult.getDifferenceQty() && mouldDayResult.getDifferenceQty() == 0){
            return;
        }
        if (!needProductionQty.equals(plannedQty)) {
          list.add(noProductionPlan);
          return;
        }
        if(hasNoProduction(productionPlan)) {
            list.add(noProductionPlan);
            return;
        }
        //即没有排产计划，又不是不排产计划
        if (StringUtils.isNotBlank(unProductionReason) &&  unProductionQty >= BigDecimal.ZERO.longValue()) {
          list.add(noProductionPlan);
        }
    });
    return list;
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

}
