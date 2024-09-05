package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型前日计划增补Service接口
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
public interface CxEngineLastDaySupplePlanService
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
    @Transactional
    public int insertCxEngineLastDaySupplePlan(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 修改成型前日计划增补
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    @Transactional
    public int updateCxEngineLastDaySupplePlan(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 批量删除成型前日计划增补
     * 
     * @param ids 需要删除的成型前日计划增补ID
     * @return 结果
     */
    @Transactional
    public int deleteCxEngineLastDaySupplePlanByIds(Long[] ids);

    /**
     * 删除成型前日计划增补信息
     * 
     * @param id 成型前日计划增补ID
     * @return 结果
     */
    @Transactional
    public int deleteCxEngineLastDaySupplePlanById(Long id);

    /**
     * 批量新增增补计划列表
     * @param cxEngineLastDaySupplePlanList
     * @return
     */
    public int batchInsertLastDaySupplePlan(List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList);

    /**
     * 根据条件从增补批次中获取未确认的增补计划
     * @param cxEngineLastDaySupplePlan
     * @return
     */
    public List<CxEngineLastDaySupplePlan> selectSupplePlanListByCondition(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan);

    /**
     * 根据增补计划自动进行计划量增补和原因分析
     * @param suppleScheduleTaskList 增补计划列表
     * @param lastDayScheduleTaskList 原始昨天增补前计划列表
     * @return
     */
    public int batchUpdateLastScheduleTask(String suppleBatchNo, List<CxEngineScheduleResult> suppleScheduleTaskList, List<CxEngineScheduleResult> lastDayScheduleTaskList);

    /**
     * 删除成型前日计划增补批次信息
     *
     * @param suppleDate 成型前日计划增补批次日期
     * @return 结果
     */
    @Transactional
    public int deleteCxEngineLastDaySupplePlanBySuppleDate(String suppleDate);

    /**
     * 批量更新单班硫化量
     * @param cxEngineLastDaySupplePlanList
     * @return
     */
    public int updateSingleLhQtyBatch(List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList);

    /**
     * 查询全部收尾的规格
     * @param suppleDate 成型前日计划增补计划日期
     * @return 结果
     */
    public List<String> selectAllCloseMachine(String suppleDate);
}
