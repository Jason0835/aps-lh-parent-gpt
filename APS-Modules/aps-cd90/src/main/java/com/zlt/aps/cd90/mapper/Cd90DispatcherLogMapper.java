package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90DispatcherLog;

import java.util.List;

/**
 * 90度裁断调度员排程操作日志Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface Cd90DispatcherLogMapper 
{
    /**
     * 查询90度裁断调度员排程操作日志
     * 
     * @param id 90度裁断调度员排程操作日志ID
     * @return 90度裁断调度员排程操作日志
     */
    public Cd90DispatcherLog selectCd90DispatcherLogById(Long id);

    /**
     * 查询90度裁断调度员排程操作日志列表
     * 
     * @param cd90DispatcherLog 90度裁断调度员排程操作日志
     * @return 90度裁断调度员排程操作日志集合
     */
    public List<Cd90DispatcherLog> selectCd90DispatcherLogList(Cd90DispatcherLog cd90DispatcherLog);

    /**
     * 新增90度裁断调度员排程操作日志
     * 
     * @param cd90DispatcherLog 90度裁断调度员排程操作日志
     * @return 结果
     */
    public int insertCd90DispatcherLog(Cd90DispatcherLog cd90DispatcherLog);

    /**
     * 修改90度裁断调度员排程操作日志
     * 
     * @param cd90DispatcherLog 90度裁断调度员排程操作日志
     * @return 结果
     */
    public int updateCd90DispatcherLog(Cd90DispatcherLog cd90DispatcherLog);

    /**
     * 删除90度裁断调度员排程操作日志
     * 
     * @param id 90度裁断调度员排程操作日志ID
     * @return 结果
     */
    public int deleteCd90DispatcherLogById(Long id);

    /**
     * 批量删除90度裁断调度员排程操作日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCd90DispatcherLogByIds(Long[] ids);

}
