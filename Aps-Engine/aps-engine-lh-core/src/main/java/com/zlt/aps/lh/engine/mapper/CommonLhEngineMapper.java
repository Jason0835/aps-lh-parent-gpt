package com.zlt.aps.lh.engine.mapper;

import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.engine.domain.LhAutoScheduleLog;
import com.zlt.aps.lh.engine.domain.LhEngineLossRate;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.domain.LhSapMonthPlanSurplus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通用Mapper接口
 */
public interface CommonLhEngineMapper {
    /**
     * 查询成型机台信息列表
     *
     * @param lhMachineInfo 成型机台信息
     * @return 成型机台信息集合
     */
    public List<LhMachineInfo> selectMachineInfoList(LhMachineInfo lhMachineInfo);

    /**
     * 批量生成硫化自动排程
     * @param lhEngineScheduleResultList
     * @return
     */
    public int batchInsertLhScheduleResult(@Param("scheduleResultList") List<LhEngineScheduleResult> lhEngineScheduleResultList);

    /**
     * 插单生成硫化排程结果
     * @param lhEngineScheduleResult
     * @return
     */
    public int insertLhEngineScheduleResult(LhEngineScheduleResult lhEngineScheduleResult);

    /**
     * 删除指定日期的排程数据
     * @param scheduleDate
     */
    void deleteLhSchedule(@Param("scheduleDate") String scheduleDate);

    /**
     * 把排程数据同步到log表
     * @param scheduleDate
     */
    void syncLhScheduleToLog(@Param("scheduleDate") String scheduleDate);

    /**
     * 根据条件查询硫化排程结果数据
     * @param lhEngineScheduleResult
     * @return
     */
    public List<LhEngineScheduleResult> selectLhEngineScheduleResultList(LhEngineScheduleResult lhEngineScheduleResult);

    /**
     * 转机台更新硫化机台
     * @param lhEngineScheduleResult
     * @return
     */
    int updateLhScheduleLhMachine(LhEngineScheduleResult lhEngineScheduleResult);

    /**
     * 获取硫化工序的耗损率
     * @param lhEngineLossRate
     * @return
     */
    List<LhEngineLossRate> selectLossSettingList(LhEngineLossRate lhEngineLossRate);

    /**
     * 获取硫化模具变更计划数据
     * @param lhApsMoldAdjustPlan
     * @return
     */
    List<LhApsMoldAdjustPlan> selectLhApsMoldAdjustPlanList(LhApsMoldAdjustPlan lhApsMoldAdjustPlan);

    /**
     * 获取硫化机当前在产规格数据列表
     * @param lhInProductionSpec
     * @return
     */
    List<LhInProductionSpec> selectLhInProductionSpecList(LhInProductionSpec lhInProductionSpec);

    /**
     * 获取硫化外胎汇总数据
     * @param lhSapMonthPlanSurplus
     * @return
     */
    List<LhSapMonthPlanSurplus> selectLhSapMonthPlanSurplusList(LhSapMonthPlanSurplus lhSapMonthPlanSurplus);


    /**
     * 根据排程日期进行对应工单日志删除
     * @param scheduleDate yyyy-MM-dd
     * @return
     */
    int deleteLhScheduleLogByScheduleDate(@Param("scheduleDate") String scheduleDate);

    /**
     * 批量插入自动排程工单对应的日志信息表
     * @param scheduleLogList
     * @return
     */
    int batchInsertLhScheduleLogResult(@Param("scheduleLogList") List<LhAutoScheduleLog> scheduleLogList);

    /**
     * 单条插入日志
     * @param lhAutoScheduleLog
     * @return
     */
    int insertLhAutoScheduleLog(LhAutoScheduleLog lhAutoScheduleLog);
}
