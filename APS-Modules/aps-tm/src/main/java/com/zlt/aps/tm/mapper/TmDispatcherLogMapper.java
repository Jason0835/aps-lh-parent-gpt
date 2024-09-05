package com.zlt.aps.tm.mapper;

import java.util.List;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;

/**
 * 胎面调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface TmDispatcherLogMapper 
{
    /**
     * 查询胎面调度员排程操作日志
     * 
     * @param id 胎面调度员排程操作日志ID
     * @return 胎面调度员排程操作日志
     */
    public TmDispatcherLog selectTmDispatcherLogById(Long id);

    /**
     * 查询胎面调度员排程操作日志列表
     * 
     * @param tmDispatcherLog 胎面调度员排程操作日志
     * @return 胎面调度员排程操作日志集合
     */
    public List<TmDispatcherLog> selectTmDispatcherLogList(TmDispatcherLog tmDispatcherLog);

    /**
     * 新增胎面调度员排程操作日志
     * 
     * @param tmDispatcherLog 胎面调度员排程操作日志
     * @return 结果
     */
    public int insertTmDispatcherLog(TmDispatcherLog tmDispatcherLog);

    /**
     * 修改胎面调度员排程操作日志
     * 
     * @param tmDispatcherLog 胎面调度员排程操作日志
     * @return 结果
     */
    public int updateTmDispatcherLog(TmDispatcherLog tmDispatcherLog);

    /**
     * 删除胎面调度员排程操作日志
     * 
     * @param id 胎面调度员排程操作日志ID
     * @return 结果
     */
    public int deleteTmDispatcherLogById(Long id);

    /**
     * 批量删除胎面调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTmDispatcherLogByIds(Long[] ids);

}
