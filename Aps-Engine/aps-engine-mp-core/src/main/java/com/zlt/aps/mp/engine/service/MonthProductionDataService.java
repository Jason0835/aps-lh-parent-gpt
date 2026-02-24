package com.zlt.aps.mp.engine.service;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ContinueGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.ContinueProductInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.monthplan.api.domain.entity.*;

import java.util.List;
import java.util.Map;

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
     * 增加一条分厂排程版本记录
     *
     * @param updateVersion
     * @return
     */
    int addFactoryProductionVersion(MpFactoryProductionVersion updateVersion);

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
     * 根据上下文，删除某个版本的分组排产数据
     * Tbr 为结构排产表
     *
     * @param context
     */
    void deletedGroupProductionData(Context context);

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
     * 获取分厂在指定年份、月份的不排产物料信息，并按物料分组
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    @Deprecated
    Map<String, FactoryNoProduction> getFactoryNoProductionConfiguration(String factoryCode, Integer year, Integer month);

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

    /**
     * 获取当前排产版本的分配信息
     *
     * @param context 排产上下文
     * @return
     */
    List<MpStructureAllocation> getStructureAllocationInfoByProductionVersion(Context context);

    /**
     * 保存分组计划的成型转产结果
     * TBR-为结构
     * PCR-英寸
     *
     * @param allocationResult
     */
    void saveGroupConversionResult(List<MpStructureAllocation> allocationResult);

    /**
     * 保存模具排产明细日志
     *
     * @param detailLogList
     */
    void saveMouldProductionDetailLog(List<FactoryMonthPlanMouldDayDetail> detailLogList);

    /**
     * 保存模具排产结果信息
     *
     * @param dayResultList
     */
    void saveMouldProductionResult(List<FactoryMonthPlanMouldDayResult> dayResultList);

    /**
     * 保存排产统计结果信息
     *
     * @param productionStatisticsResultList
     */
    void saveProductionStatisticsResult(List<MpMonthPlanStatistics> productionStatisticsResultList);

    /**
     * 保存未排计划信息
     *
     * @param noProductionPlanList
     */
    void saveNoProductionPlan(List<MonthPlanNoProductionPlan> noProductionPlanList);
}
