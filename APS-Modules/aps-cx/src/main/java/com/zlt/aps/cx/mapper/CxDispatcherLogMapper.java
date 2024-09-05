package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxDispatcherLog;

import java.util.List;

/**
 * 成型调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface CxDispatcherLogMapper 
{
    /**
     * 查询成型调度员排程操作日志
     * 
     * @param id 成型调度员排程操作日志ID
     * @return 成型调度员排程操作日志
     */
    public CxDispatcherLog selectCxDispatcherLogById(Long id);

    /**
     * 查询成型调度员排程操作日志列表
     * 
     * @param cxDispatcherLog 成型调度员排程操作日志
     * @return 成型调度员排程操作日志集合
     */
    public List<CxDispatcherLog> selectCxDispatcherLogList(CxDispatcherLog cxDispatcherLog);

    /**
     * 新增成型调度员排程操作日志
     * 
     * @param cxDispatcherLog 成型调度员排程操作日志
     * @return 结果
     */
    public int insertCxDispatcherLog(CxDispatcherLog cxDispatcherLog);

    /**
     * 删除成型调度员排程操作日志
     * 
     * @param id 成型调度员排程操作日志ID
     * @return 结果
     */
    public int deleteCxDispatcherLogById(Long id);

    /**
     * 批量删除成型调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxDispatcherLogByIds(Long[] ids);

}
