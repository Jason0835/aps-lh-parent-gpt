package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.api.domain.entity.GsqDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 钢丝圈调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface GsqDispatcherLogService
{
    /**
     * 查询钢丝圈调度员排程操作日志
     * 
     * @param id 钢丝圈调度员排程操作日志ID
     * @return 钢丝圈调度员排程操作日志
     */
    public GsqDispatcherLog selectGsqDispatcherLogById(Long id);

    /**
     * 查询钢丝圈调度员排程操作日志列表
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 钢丝圈调度员排程操作日志集合
     */
    public List<GsqDispatcherLog> selectGsqDispatcherLogList(GsqDispatcherLog gsqDispatcherLog);

    /**
     * 新增钢丝圈调度员排程操作日志
     * 
     * @param gsqDispatcherLog 钢丝圈调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertGsqDispatcherLog(GsqDispatcherLog gsqDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(GsqDispatcherLog dispatcherLog);
}
