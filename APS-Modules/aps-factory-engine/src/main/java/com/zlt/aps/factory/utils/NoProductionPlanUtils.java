package com.zlt.aps.factory.utils;

import com.google.common.collect.Lists;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 不排产计划工具类
 * @author Yelq
 */
public class NoProductionPlanUtils {
  public static List<MonthPlanNoProductionPlan> buildNoProductionPlanList(Map<Long, MonthPlanProductionRequirePlanVo> productionPlanMap, Map<Long, Integer> sumProductionMap) {
    List<MonthPlanNoProductionPlan> list = Lists.newArrayList();
    productionPlanMap.forEach((key, productionPlan) -> {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String unProductionReason = productionPlan.getNoProductionReason();
        MonthPlanNoProductionPlan noProductionPlan = new MonthPlanNoProductionPlan();
        BeanUtils.copyProperties(productionPlan, noProductionPlan);
        noProductionPlan.setBaseVale(null);
        noProductionPlan.setId(null);
        noProductionPlan.setReason(unProductionReason);
        Integer needProductionQty = productionPlan.getFactProdReqQty();
        Integer plannedQty = sumProductionMap.getOrDefault(monthPlanId,0);
        long unProductionQty = needProductionQty - plannedQty;
        noProductionPlan.setUnProductionQty(unProductionQty);
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
