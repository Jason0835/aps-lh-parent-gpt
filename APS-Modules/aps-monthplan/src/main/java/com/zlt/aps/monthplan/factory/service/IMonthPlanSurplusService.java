package com.zlt.aps.monthplan.factory.service;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;

import java.util.List;

/**
 *  月度剩余量业务接口
 *  @author Yelq
 */
public interface IMonthPlanSurplusService {
  /**
   *  保存月度剩余量
   * @param finalList
   */
  void savePlanSurplusList(List<FactoryMonthPlanProdFinal> finalList);
  /**
   *
   * @param importList
   */
  void finalUpdatePlanSurplusList(List<MonthPlanProductionFinalResult> importList);
}
