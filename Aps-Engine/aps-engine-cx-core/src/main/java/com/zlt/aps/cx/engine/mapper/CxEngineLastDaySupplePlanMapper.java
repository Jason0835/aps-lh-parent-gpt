package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResultVersion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型前日计划增补Mapper接口
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
public interface CxEngineLastDaySupplePlanMapper 
{
    /**
     * 查询成型前日计划增补
     * 
     * @param id 成型前日计划增补ID
     * @return 成型前日计划增补
     */
    public CxEngineLastDaySupplePlan selectCxEngineLastDaySupplePlanById(Long id);

    /**
     * 查询成型前日计划增补列表
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 成型前日计划增补集合
     */
    public List<CxEngineLastDaySupplePlan> selectCxEngineLastDaySupplePlanList(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 新增成型前日计划增补
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    public int insertCxEngineLastDaySupplePlan(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 修改成型前日计划增补
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    public int updateCxEngineLastDaySupplePlan(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 删除成型前日计划增补
     * 
     * @param id 成型前日计划增补ID
     * @return 结果
     */
    public int deleteCxEngineLastDaySupplePlanById(Long id);

    /**
     * 批量删除成型前日计划增补
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxEngineLastDaySupplePlanByIds(Long[] ids);

    /**
     * 批量生成增补计划列表
     * @param cxEngineLastDaySupplePlanList
     * @return
     */
    public int batchInsertLastDaySupplePlan(@Param("list") List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList);

    /**
     * 根据条件从增补批次中获取未确认的增补计划
     * @param cxEngineLastDaySupplePlan
     * @return
     */
    public List<CxEngineLastDaySupplePlan> selectSupplePlanListByCondition(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 根据增补计划自动进行计划量增补和原因分析
     * @param lastDayScheduleTaskSuppleList
     * @return
     */
    public int batchUpdateLastScheduleTask(@Param("list") List<CxEngineScheduleResult> lastDayScheduleTaskSuppleList);

    /**
     * 根据增补计划自动进行计划量增补和原因分析
     * @param lastDayScheduleTaskSuppleList
     * @return
     */
    public int batchInsertCxScheduleResultVersion(@Param("scheduleResultVersionList") List<CxEngineScheduleResultVersion> lastDayScheduleTaskSuppleList);

    /**
     * 删除成型前日计划增补
     * @param suppleDate 成型前日计划增补计划日期
     * @return 结果
     */
    public int deleteCxEngineLastDaySupplePlanBySuppleDate(@Param("suppleDate") String suppleDate);

    /**
     * 批量更新重算单班硫化量
     * @param lastDayScheduleTaskSuppleList
     * @return
     */
    public int updateSingleLhQtyBatch(@Param("list") List<CxEngineLastDaySupplePlan> lastDayScheduleTaskSuppleList);

    /**
     * 查询全部收尾的规格
     * @param suppleDate 成型前日计划增补计划日期
     * @return 结果
     */
    public List<String> selectAllCloseMachine(@Param("suppleDate") String suppleDate);

}
