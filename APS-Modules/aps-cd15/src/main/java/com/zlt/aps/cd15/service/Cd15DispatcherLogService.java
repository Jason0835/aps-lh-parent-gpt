package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15DispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 15度裁断调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface Cd15DispatcherLogService
{
    /**
     * 查询15度裁断调度员排程操作日志
     * 
     * @param id 15度裁断调度员排程操作日志ID
     * @return 15度裁断调度员排程操作日志
     */
    public Cd15DispatcherLog selectCd15DispatcherLogById(Long id);

    /**
     * 查询15度裁断调度员排程操作日志列表
     * 
     * @param cd15DispatcherLog 15度裁断调度员排程操作日志
     * @return 15度裁断调度员排程操作日志集合
     */
    public List<Cd15DispatcherLog> selectCd15DispatcherLogList(Cd15DispatcherLog cd15DispatcherLog);

    /**
     * 新增15度裁断调度员排程操作日志
     * 
     * @param cd15DispatcherLog 15度裁断调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertCd15DispatcherLog(Cd15DispatcherLog cd15DispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(Cd15DispatcherLog dispatcherLog);
}
