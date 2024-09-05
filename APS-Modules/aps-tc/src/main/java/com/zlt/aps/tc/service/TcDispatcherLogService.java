package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 胎侧调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface TcDispatcherLogService
{
    /**
     * 查询胎侧调度员排程操作日志
     * 
     * @param id 胎侧调度员排程操作日志ID
     * @return 胎侧调度员排程操作日志
     */
    public TcDispatcherLog selectTcDispatcherLogById(Long id);

    /**
     * 查询胎侧调度员排程操作日志列表
     * 
     * @param tcDispatcherLog 胎侧调度员排程操作日志
     * @return 胎侧调度员排程操作日志集合
     */
    public List<TcDispatcherLog> selectTcDispatcherLogList(TcDispatcherLog tcDispatcherLog);

    /**
     * 新增胎侧调度员排程操作日志
     * 
     * @param tcDispatcherLog 胎侧调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertTcDispatcherLog(TcDispatcherLog tcDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(TcDispatcherLog dispatcherLog);
}
