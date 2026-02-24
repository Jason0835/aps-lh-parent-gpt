package com.zlt.aps.mp.common.utils;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yelq
 */ // 在现有的服务类中添加新方法，并指定事务传播行为
@Service
@RequiredArgsConstructor
public class ProductionSchedulingService {
  // 排产
  private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void executeSchedulingInNewTransaction(DpDemandPlan param,Context context) {
    monthPlanProductionSchedulingService.general(context);
  }
}
