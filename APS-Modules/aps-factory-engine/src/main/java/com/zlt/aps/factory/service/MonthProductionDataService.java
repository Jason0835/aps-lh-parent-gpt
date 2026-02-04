package com.zlt.aps.factory.service;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ContinueGroupInfo;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldUsedStatusLog;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;

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
     * 获取工厂排程版本
     *
     * @param context 排产上下文
     * @return
     */
    MpFactoryProductionVersion getFactoryMonthPlanVersion(Context context);

    /**
     * 获取工厂需求计划版本第一个版本信息
     * (可能没有排，也可能有排)
     *
     * @param context
     * @return
     */
    MpFactoryProductionVersion getFirstFactoryMonthPlanVersion(Context context);

    /**
     * 根据工厂编码、年份、月份获取对应的定稿版本信息
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    MpFactoryProductionVersion getFinalVersion(String factoryCode, Integer year, Integer month);

    /**
     * 更新分厂排程版本
     * 补充 初始化版本及排产版本
     *
     * @param updateVersion
     * @return
     */
    int updateFactoryProductionVersion(MpFactoryProductionVersion updateVersion);
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

    /**
     * 获取续作SKU信息，包含续作机台及使用的模具数
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    List<ContinueProductInfo> getContinueProductionInfo(String factoryCode, Integer year, Integer month, Integer lastDay);

    /**
     * 获取在机结构信息，从结构排产表中获取
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    List<ContinueGroupInfo> getContinueGroupInfo(String factoryCode, Integer year, Integer month, Integer lastDay);
    /**
     * 获取历史分组排产数据信息
     *
     * @param context 排产上下文
     * @return
     */
    List<MpStructureAllocation> getHistoryStructureAllocationInfo(Context context);
}
