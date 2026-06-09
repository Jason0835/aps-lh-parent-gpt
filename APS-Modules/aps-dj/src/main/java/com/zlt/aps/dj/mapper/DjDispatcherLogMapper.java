package com.zlt.aps.dj.mapper;

import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;

/**
 * 垫胶调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface DjDispatcherLogMapper 
{
    /**
     * 查询垫胶调度员排程操作日志
     * 
     * @param id 垫胶调度员排程操作日志ID
     * @return 垫胶调度员排程操作日志
     */
    public DjDispatcherLog selectNcDispatcherLogById(Long id);

    /**
     * 查询垫胶调度员排程操作日志列表
     * 
     * @param ncDispatcherLog 垫胶调度员排程操作日志
     * @return 垫胶调度员排程操作日志集合
     */
    public List<DjDispatcherLog> selectNcDispatcherLogList(DjDispatcherLog ncDispatcherLog);

    /**
     * 新增垫胶调度员排程操作日志
     * 
     * @param ncDispatcherLog 垫胶调度员排程操作日志
     * @return 结果
     */
    public int insertNcDispatcherLog(DjDispatcherLog ncDispatcherLog);

    /**
     * 修改垫胶调度员排程操作日志
     * 
     * @param ncDispatcherLog 垫胶调度员排程操作日志
     * @return 结果
     */
    public int updateNcDispatcherLog(DjDispatcherLog ncDispatcherLog);

    /**
     * 删除垫胶调度员排程操作日志
     * 
     * @param id 垫胶调度员排程操作日志ID
     * @return 结果
     */
    public int deleteNcDispatcherLogById(Long id);

    /**
     * 批量删除垫胶调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcDispatcherLogByIds(Long[] ids);

}
