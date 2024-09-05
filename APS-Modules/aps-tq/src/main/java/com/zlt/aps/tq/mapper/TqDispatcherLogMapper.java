package com.zlt.aps.tq.mapper;

import com.zlt.aps.tq.api.domain.entity.TqDispatcherLog;

import java.util.List;

/**
 * 胎圈调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface TqDispatcherLogMapper 
{
    /**
     * 查询胎圈调度员排程操作日志
     * 
     * @param id 胎圈调度员排程操作日志ID
     * @return 胎圈调度员排程操作日志
     */
    public TqDispatcherLog selectTqDispatcherLogById(Long id);

    /**
     * 查询胎圈调度员排程操作日志列表
     * 
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 胎圈调度员排程操作日志集合
     */
    public List<TqDispatcherLog> selectTqDispatcherLogList(TqDispatcherLog tqDispatcherLog);

    /**
     * 新增胎圈调度员排程操作日志
     * 
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 结果
     */
    public int insertTqDispatcherLog(TqDispatcherLog tqDispatcherLog);

    /**
     * 修改胎圈调度员排程操作日志
     * 
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 结果
     */
    public int updateTqDispatcherLog(TqDispatcherLog tqDispatcherLog);

    /**
     * 删除胎圈调度员排程操作日志
     * 
     * @param id 胎圈调度员排程操作日志ID
     * @return 结果
     */
    public int deleteTqDispatcherLogById(Long id);

    /**
     * 批量删除胎圈调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTqDispatcherLogByIds(Long[] ids);

}
