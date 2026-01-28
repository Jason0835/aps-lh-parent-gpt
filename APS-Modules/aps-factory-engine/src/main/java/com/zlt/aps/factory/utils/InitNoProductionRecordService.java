package com.zlt.aps.factory.utils;

import com.google.common.collect.Maps;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * 初始化不排产记录
 * @author Yelq
 */
@Service
public class InitNoProductionRecordService {

  public void initNoProductionRecord(TbrProductionContext productionContext, MonthPlanProductionRequirePlanVo requirePlan) {
    Map<Long, MonthPlanNoProductionPlan> noProductionRecordMap = productionContext.getNoProductionRecordMap();
    if(CollectionUtils.isEmpty(noProductionRecordMap)) {
      noProductionRecordMap = Maps.newHashMap();
      productionContext.setNoProductionRecordMap(noProductionRecordMap);
    }
    //生成不排产数据
    MonthPlanNoProductionPlan  noProductionRecord = NoProductionPlanUtils.createNoProductionRecordData(requirePlan);
    noProductionRecordMap.put(noProductionRecord.getMonthPlanId(), noProductionRecord);
  }
}
