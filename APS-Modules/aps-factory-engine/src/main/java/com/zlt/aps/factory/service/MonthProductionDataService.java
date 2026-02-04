package com.zlt.aps.factory.service;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldUsedStatusLog;

import java.util.List;

/**
 * 排产调用数据获取服务类
 * 月份排产模块接口定义类
 *
 * @author ZLT
 * @date 20260204
 */
public interface MonthProductionDataService {

    /**
     * 根据上下文，删除某个版本的初始化数据
     *
     * @param context
     */
    void deletedInitData(Context context);

    /**
     * 根据上下文，删除某个版本的模具排产数据
     *
     * @param context
     */
    void deletedMouldProductionData(Context context);

    /**
     * 保存排产初始化信息
     *
     * @param monthPlanInitList
     */
    void saveMonthPlanInit(List<MonthPlanProductionRequirePlanVo> monthPlanInitList);

    /**
     * 保存模具排程排产流程日志
     *
     * @param productionLog 日志信息
     */
    void saveMouldProductionLog(MouldProductionLog productionLog);
    /**
     * 根据查询条件，获取工厂的排产计划信息
     * 从初始化中获取
     *
     * @param context
     * @return
     */
    List<MonthPlanProductionRequirePlanVo> getFactoryMonthPlanManufacturing(Context context);

    /**
     * 保存模具状态日志
     *
     * @param usedLogList
     */
    void saveMouldUsedLog(List<MpMouldUsedStatusLog> usedLogList);
}
