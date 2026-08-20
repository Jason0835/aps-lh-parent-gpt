package com.zlt.aps.mp.factory.service;

import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;

import java.util.List;
import java.util.Map;

/**
 * 月计划数据验证服务
 * 导入、计划调整都可使用
 *
 * @author zlt
 */
public interface MonthPlanValidateService {

    /**
     * 校验多台机是否可进行胎胚分配
     *
     * @param monthPlanVersion 需求版本号
     * @param productVersion   排产版本号
     * @param dailyCapacityMap 主要为开停产信息
     * @param monthPlanList    所有排产计划信息
     */
    void validateEmbryoAllocation(String monthPlanVersion,
                                           String productVersion,
                                           Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap,
                                           List<FactoryMonthPlanMouldDayResult> monthPlanList);
}
