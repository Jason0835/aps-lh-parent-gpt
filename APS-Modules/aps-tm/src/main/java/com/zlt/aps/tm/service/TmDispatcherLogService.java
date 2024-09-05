package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胎面调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface TmDispatcherLogService
{
    /**
     * 查询胎面调度员排程操作日志
     * 
     * @param id 胎面调度员排程操作日志ID
     * @return 胎面调度员排程操作日志
     */
    public TmDispatcherLog selectTmDispatcherLogById(Long id);

    /**
     * 查询胎面调度员排程操作日志列表
     * 
     * @param tmDispatcherLog 胎面调度员排程操作日志
     * @return 胎面调度员排程操作日志集合
     */
    public List<TmDispatcherLog> selectTmDispatcherLogList(TmDispatcherLog tmDispatcherLog);

    /**
     * 新增胎面调度员排程操作日志
     * 
     * @param tmDispatcherLog 胎面调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertTmDispatcherLog(TmDispatcherLog tmDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(TmDispatcherLog dispatcherLog);
}
