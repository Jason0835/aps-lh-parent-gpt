package com.zlt.aps.gdyy.mapper;

import com.zlt.aps.gdyy.api.domain.entity.GdyyDispatcherLog;

import java.util.List;

/**
 * 钢带压延调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface GdyyDispatcherLogMapper 
{
    /**
     * 查询钢带压延调度员排程操作日志
     * 
     * @param id 钢带压延调度员排程操作日志ID
     * @return 钢带压延调度员排程操作日志
     */
    public GdyyDispatcherLog selectGdyyDispatcherLogById(Long id);

    /**
     * 查询钢带压延调度员排程操作日志列表
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 钢带压延调度员排程操作日志集合
     */
    public List<GdyyDispatcherLog> selectGdyyDispatcherLogList(GdyyDispatcherLog gdyyDispatcherLog);

    /**
     * 新增钢带压延调度员排程操作日志
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 结果
     */
    public int insertGdyyDispatcherLog(GdyyDispatcherLog gdyyDispatcherLog);

    /**
     * 修改钢带压延调度员排程操作日志
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 结果
     */
    public int updateGdyyDispatcherLog(GdyyDispatcherLog gdyyDispatcherLog);

    /**
     * 删除钢带压延调度员排程操作日志
     * 
     * @param id 钢带压延调度员排程操作日志ID
     * @return 结果
     */
    public int deleteGdyyDispatcherLogById(Long id);

    /**
     * 批量删除钢带压延调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteGdyyDispatcherLogByIds(Long[] ids);

}
