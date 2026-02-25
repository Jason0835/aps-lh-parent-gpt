package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.utils.NoProductionPlanUtils;
import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * 初始化不排产记录
 * @author Yelq
 */
@Service
public class InitNoProductionRecordHandler {

  public void initNoProductionRecord(TbrProductionContext productionContext, MonthPlanProductionRequirePlanVo requirePlan) {
    Map<Long, MonthPlanNoProductionPlan> noProductionRecordMap = productionContext.getNoProductionRecordMap();
    if(CollectionUtils.isEmpty(noProductionRecordMap)) {
      noProductionRecordMap = Maps.newHashMap();
      productionContext.setNoProductionRecordMap(noProductionRecordMap);
    }
    //生成不排产数据
    MonthPlanNoProductionPlan  noProductionRecord = NoProductionPlanUtils.createNoProductionRecordData(requirePlan);
    if(noProductionRecord != null) {
      noProductionRecordMap.put(noProductionRecord.getMonthPlanId(), noProductionRecord);
    }
  }
}
