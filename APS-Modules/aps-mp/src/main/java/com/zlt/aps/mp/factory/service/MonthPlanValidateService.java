package com.zlt.aps.mp.factory.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;

import java.time.YearMonth;
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
     * @param monthPlanVersion          需求版本号
     * @param productVersion            排产版本号
     * @param isAdjust                  是否调整
     * @param dailyCapacityMap          主要为开停产信息
     * @param monthPlanList             所有排产计划信息
     * @param yearMonth                 计划年月
     * @param importLogId               导入日志
     * @param importErrorLogs           存储错误信息
     */
    void validateEmbryoAllocation(String monthPlanVersion,
                                  String productVersion,
                                  boolean isAdjust,
                                  Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap,
                                  List<FactoryMonthPlanMouldDayResult> monthPlanList,
                                  YearMonth yearMonth,
                                  Long importLogId,
                                  List<ImportErrorLog> importErrorLogs);
}
