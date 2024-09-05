package com.zlt.aps.xwyy.mapper;

import com.zlt.aps.xwyy.api.domain.entity.XwyyDispatcherLog;

import java.util.List;

/**
 * 纤维压延调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface XwyyDispatcherLogMapper 
{
    /**
     * 查询纤维压延调度员排程操作日志
     * 
     * @param id 纤维压延调度员排程操作日志ID
     * @return 纤维压延调度员排程操作日志
     */
    public XwyyDispatcherLog selectXwyyDispatcherLogById(Long id);

    /**
     * 查询纤维压延调度员排程操作日志列表
     * 
     * @param xwyyDispatcherLog 纤维压延调度员排程操作日志
     * @return 纤维压延调度员排程操作日志集合
     */
    public List<XwyyDispatcherLog> selectXwyyDispatcherLogList(XwyyDispatcherLog xwyyDispatcherLog);

    /**
     * 新增纤维压延调度员排程操作日志
     * 
     * @param xwyyDispatcherLog 纤维压延调度员排程操作日志
     * @return 结果
     */
    public int insertXwyyDispatcherLog(XwyyDispatcherLog xwyyDispatcherLog);

    /**
     * 修改纤维压延调度员排程操作日志
     * 
     * @param xwyyDispatcherLog 纤维压延调度员排程操作日志
     * @return 结果
     */
    public int updateXwyyDispatcherLog(XwyyDispatcherLog xwyyDispatcherLog);

    /**
     * 删除纤维压延调度员排程操作日志
     * 
     * @param id 纤维压延调度员排程操作日志ID
     * @return 结果
     */
    public int deleteXwyyDispatcherLogById(Long id);

    /**
     * 批量删除纤维压延调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteXwyyDispatcherLogByIds(Long[] ids);

}
