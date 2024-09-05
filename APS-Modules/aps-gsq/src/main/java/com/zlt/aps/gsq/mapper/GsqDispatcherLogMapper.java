package com.zlt.aps.gsq.mapper;

import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;

import java.util.List;

/**
 * 钢丝圈调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface GsqDispatcherLogMapper 
{
    /**
     * 查询钢丝圈调度员排程操作日志
     * 
     * @param id 钢丝圈调度员排程操作日志ID
     * @return 钢丝圈调度员排程操作日志
     */
    public GsqDispatcherLog selectGsqDispatcherLogById(Long id);

    /**
     * 查询钢丝圈调度员排程操作日志列表
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 钢丝圈调度员排程操作日志集合
     */
    public List<GsqDispatcherLog> selectGsqDispatcherLogList(GsqDispatcherLog gsqDispatcherLog);

    /**
     * 新增钢丝圈调度员排程操作日志
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 结果
     */
    public int insertGsqDispatcherLog(GsqDispatcherLog gsqDispatcherLog);

    /**
     * 修改钢丝圈调度员排程操作日志
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 结果
     */
    public int updateGsqDispatcherLog(GsqDispatcherLog gsqDispatcherLog);

    /**
     * 删除钢丝圈调度员排程操作日志
     * 
     * @param id 钢丝圈调度员排程操作日志ID
     * @return 结果
     */
    public int deleteGsqDispatcherLogById(Long id);

    /**
     * 批量删除钢丝圈调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteGsqDispatcherLogByIds(Long[] ids);

}
