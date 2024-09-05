package com.zlt.aps.gdyy.service;

import com.zlt.aps.gdyy.api.domain.entity.GdyyDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 钢带压延调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface GdyyDispatcherLogService
{
    /**
     * 查询钢带压延调度员排程操作日志
     * 
     * @param id 钢带压延调度员排程操作日志ID
     * @return 钢带压延调度员排程操作日志
     */
    public GdyyDispatcherLog selectGdyyDispatcherLogById(Long id);

    /**
     * 查询钢带压延调度员排程操作日志列表
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 钢带压延调度员排程操作日志集合
     */
    public List<GdyyDispatcherLog> selectGdyyDispatcherLogList(GdyyDispatcherLog gdyyDispatcherLog);

    /**
     * 新增钢带压延调度员排程操作日志
     * 
     * @param gdyyDispatcherLog 钢带压延调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertGdyyDispatcherLog(GdyyDispatcherLog gdyyDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(GdyyDispatcherLog dispatcherLog);
}
