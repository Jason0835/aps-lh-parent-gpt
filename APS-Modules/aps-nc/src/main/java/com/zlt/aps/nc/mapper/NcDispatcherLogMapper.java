package com.zlt.aps.nc.mapper;

import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;

import java.util.List;

/**
 * 内衬调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface NcDispatcherLogMapper 
{
    /**
     * 查询内衬调度员排程操作日志
     * 
     * @param id 内衬调度员排程操作日志ID
     * @return 内衬调度员排程操作日志
     */
    public NcDispatcherLog selectNcDispatcherLogById(Long id);

    /**
     * 查询内衬调度员排程操作日志列表
     * 
     * @param ncDispatcherLog 内衬调度员排程操作日志
     * @return 内衬调度员排程操作日志集合
     */
    public List<NcDispatcherLog> selectNcDispatcherLogList(NcDispatcherLog ncDispatcherLog);

    /**
     * 新增内衬调度员排程操作日志
     * 
     * @param ncDispatcherLog 内衬调度员排程操作日志
     * @return 结果
     */
    public int insertNcDispatcherLog(NcDispatcherLog ncDispatcherLog);

    /**
     * 修改内衬调度员排程操作日志
     * 
     * @param ncDispatcherLog 内衬调度员排程操作日志
     * @return 结果
     */
    public int updateNcDispatcherLog(NcDispatcherLog ncDispatcherLog);

    /**
     * 删除内衬调度员排程操作日志
     * 
     * @param id 内衬调度员排程操作日志ID
     * @return 结果
     */
    public int deleteNcDispatcherLogById(Long id);

    /**
     * 批量删除内衬调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcDispatcherLogByIds(Long[] ids);

}
