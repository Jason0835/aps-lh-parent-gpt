package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;

import java.util.List;

/**
 * 胎侧调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface TcDispatcherLogMapper 
{
    /**
     * 查询胎侧调度员排程操作日志
     * 
     * @param id 胎侧调度员排程操作日志ID
     * @return 胎侧调度员排程操作日志
     */
    public TcDispatcherLog selectTcDispatcherLogById(Long id);

    /**
     * 查询胎侧调度员排程操作日志列表
     * 
     * @param tcDispatcherLog 胎侧调度员排程操作日志
     * @return 胎侧调度员排程操作日志集合
     */
    public List<TcDispatcherLog> selectTcDispatcherLogList(TcDispatcherLog tcDispatcherLog);

    /**
     * 新增胎侧调度员排程操作日志
     * 
     * @param tcDispatcherLog 胎侧调度员排程操作日志
     * @return 结果
     */
    public int insertTcDispatcherLog(TcDispatcherLog tcDispatcherLog);

    /**
     * 修改胎侧调度员排程操作日志
     * 
     * @param tcDispatcherLog 胎侧调度员排程操作日志
     * @return 结果
     */
    public int updateTcDispatcherLog(TcDispatcherLog tcDispatcherLog);

    /**
     * 删除胎侧调度员排程操作日志
     * 
     * @param id 胎侧调度员排程操作日志ID
     * @return 结果
     */
    public int deleteTcDispatcherLogById(Long id);

    /**
     * 批量删除胎侧调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTcDispatcherLogByIds(Long[] ids);

}
