package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90DispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 90度裁断调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface Cd90DispatcherLogService
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
    @Transactional
    public int insertCd90DispatcherLog(Cd90DispatcherLog cd90DispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(Cd90DispatcherLog dispatcherLog);
}
