package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;

import java.util.List;

/**
 * 硫化调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface LhDispatcherLogMapper 
{
    /**
     * 查询硫化调度员排程操作日志
     * 
     * @param id 硫化调度员排程操作日志ID
     * @return 硫化调度员排程操作日志
     */
    public LhDispatcherLog selectLhDispatcherLogById(Long id);

    /**
     * 查询硫化调度员排程操作日志列表
     * 
     * @param lhDispatcherLog 硫化调度员排程操作日志
     * @return 硫化调度员排程操作日志集合
     */
    public List<LhDispatcherLog> selectLhDispatcherLogList(LhDispatcherLog lhDispatcherLog);

    /**
     * 新增硫化调度员排程操作日志
     * 
     * @param lhDispatcherLog 硫化调度员排程操作日志
     * @return 结果
     */
    public int insertLhDispatcherLog(LhDispatcherLog lhDispatcherLog);

    /**
     * 删除硫化调度员排程操作日志
     * 
     * @param id 硫化调度员排程操作日志ID
     * @return 结果
     */
    public int deleteLhDispatcherLogById(Long id);

    /**
     * 批量删除硫化调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhDispatcherLogByIds(Long[] ids);

}
