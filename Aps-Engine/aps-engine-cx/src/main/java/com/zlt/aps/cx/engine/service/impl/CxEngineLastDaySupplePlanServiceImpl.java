package com.zlt.aps.cx.engine.service.impl;

import java.util.List;

import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.mapper.CxEngineLastDaySupplePlanMapper;
import com.zlt.aps.cx.engine.service.CxEngineLastDaySupplePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 成型前日计划增补Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
@Service("cxEngineLastDaySupplePlanService")
public class CxEngineLastDaySupplePlanServiceImpl implements CxEngineLastDaySupplePlanService
{
    @Autowired
    private CxEngineLastDaySupplePlanMapper cxEngineLastDaySupplePlanMapper;

    /**
     * 查询成型前日计划增补
     * 
     * @param id 成型前日计划增补ID
     * @return 成型前日计划增补
     */
    @Override
    public CxEngineLastDaySupplePlan selectCxEngineLastDaySupplePlanById(Long id)
    {
        return cxEngineLastDaySupplePlanMapper.selectCxEngineLastDaySupplePlanById(id);
    }

    /**
     * 查询成型前日计划增补列表
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 成型前日计划增补
     */
    @Override
    public List<CxEngineLastDaySupplePlan> selectCxEngineLastDaySupplePlanList(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan)
    {
        return cxEngineLastDaySupplePlanMapper.selectCxEngineLastDaySupplePlanList(cxEngineLastDaySupplePlan);
    }

    /**
     * 新增成型前日计划增补
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    @Override
    public int insertCxEngineLastDaySupplePlan(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan)
    {
        cxEngineLastDaySupplePlan.setBaseVale(null);
        return cxEngineLastDaySupplePlanMapper.insertCxEngineLastDaySupplePlan(cxEngineLastDaySupplePlan);
    }

    /**
     * 修改成型前日计划增补
     * 
     * @param cxEngineLastDaySupplePlan 成型前日计划增补
     * @return 结果
     */
    @Override
    public int updateCxEngineLastDaySupplePlan(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan)
    {
        cxEngineLastDaySupplePlan.setBaseVale(cxEngineLastDaySupplePlan.getId());
        return cxEngineLastDaySupplePlanMapper.updateCxEngineLastDaySupplePlan(cxEngineLastDaySupplePlan);
    }

    /**
     * 批量删除成型前日计划增补
     * 
     * @param ids 需要删除的成型前日计划增补ID
     * @return 结果
     */
    @Override
    public int deleteCxEngineLastDaySupplePlanByIds(Long[] ids)
    {
        return cxEngineLastDaySupplePlanMapper.deleteCxEngineLastDaySupplePlanByIds(ids);
    }

    /**
     * 删除成型前日计划增补信息
     * 
     * @param id 成型前日计划增补ID
     * @return 结果
     */
    @Override
    public int deleteCxEngineLastDaySupplePlanById(Long id)
    {
        return cxEngineLastDaySupplePlanMapper.deleteCxEngineLastDaySupplePlanById(id);
    }

    /**
     * 批量生成增补计划列表
     * @param cxEngineLastDaySupplePlanList
     * @return
     */
    @Override
    public int batchInsertLastDaySupplePlan(List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList) {
        return cxEngineLastDaySupplePlanMapper.batchInsertLastDaySupplePlan(cxEngineLastDaySupplePlanList);
    }

    /**
     * 从增补批次日期中获取未确认的增补计划列表
     * @param cxEngineLastDaySupplePlan
     * @return
     */
    @Override
    public List<CxEngineLastDaySupplePlan> selectSupplePlanListByCondition(CxEngineLastDaySupplePlan cxEngineLastDaySupplePlan) {
        return cxEngineLastDaySupplePlanMapper.selectSupplePlanListByCondition(cxEngineLastDaySupplePlan);
    }

    /**
     *  根据增补计划自动更新昨日排程三班及自动增补计划量
     * @param suppleScheduleTaskList
     * @return
     */
    @Override
    public int batchUpdateLastScheduleTask(String suppleBatchNo,List<CxEngineScheduleResult> suppleScheduleTaskList,List<CxEngineScheduleResult> lastDayScheduleTaskList) {
       /* //1、进行批次原始排程留存
        List<CxEngineScheduleResultVersion> versionList =BeanConverUtil.converList(lastDayScheduleTaskList, CxEngineScheduleResultVersion.class);
        for(CxEngineScheduleResultVersion version:versionList){
            version.setSuppleBatchNo(suppleBatchNo);//绑定留存所对应的批次版本
        }
        //增补批次的原始计划留存
        cxEngineLastDaySupplePlanMapper.batchInsertCxScheduleResultVersion(versionList);*/

        return cxEngineLastDaySupplePlanMapper.batchUpdateLastScheduleTask(suppleScheduleTaskList);
    }

    /**
     * 根据增补日期删除增补计划列表
     * @param suppleDate 成型前日计划增补批次日期
     * @return
     */
    @Override
    public int deleteCxEngineLastDaySupplePlanBySuppleDate(String suppleDate) {
        return cxEngineLastDaySupplePlanMapper.deleteCxEngineLastDaySupplePlanBySuppleDate(suppleDate);
    }

    @Override
    public int updateSingleLhQtyBatch(List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList) {
        return cxEngineLastDaySupplePlanMapper.updateSingleLhQtyBatch(cxEngineLastDaySupplePlanList);
    }

    @Override
    public List<String> selectAllCloseMachine(String suppleDate) {
        return cxEngineLastDaySupplePlanMapper.selectAllCloseMachine(suppleDate);
    }
}
