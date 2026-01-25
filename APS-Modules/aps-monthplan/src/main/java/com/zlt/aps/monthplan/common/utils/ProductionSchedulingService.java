package com.zlt.aps.monthplan.common.utils;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Yelq
 */ // 在现有的服务类中添加新方法，并指定事务传播行为
@Service
@RequiredArgsConstructor
public class ProductionSchedulingService {
  // 排产
  private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

  public void executeSchedulingInNewTransaction(DpDemandPlan param,Context context) {
    monthPlanProductionSchedulingService.general(context);
  }
}
