package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胎圈调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface TqDispatcherLogService
{
    /**
     * 查询胎圈调度员排程操作日志
     * 
     * @param id 胎圈调度员排程操作日志ID
     * @return 胎圈调度员排程操作日志
     */
    public TqDispatcherLog selectTqDispatcherLogById(Long id);

    /**
     * 查询胎圈调度员排程操作日志列表
     * 
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 胎圈调度员排程操作日志集合
     */
    public List<TqDispatcherLog> selectTqDispatcherLogList(TqDispatcherLog tqDispatcherLog);

    /**
     * 新增胎圈调度员排程操作日志
     * 
     * @param tqDispatcherLog 胎圈调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertTqDispatcherLog(TqDispatcherLog tqDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(TqDispatcherLog dispatcherLog);
}
