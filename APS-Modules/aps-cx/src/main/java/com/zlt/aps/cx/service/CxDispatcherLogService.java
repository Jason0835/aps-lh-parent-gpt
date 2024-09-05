package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.entity.CxDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface CxDispatcherLogService
{
    /**
     * 查询成型调度员排程操作日志
     * 
     * @param id 成型调度员排程操作日志ID
     * @return 成型调度员排程操作日志
     */
    public CxDispatcherLog selectCxDispatcherLogById(Long id);

    /**
     * 查询成型调度员排程操作日志列表
     * 
     * @param cxDispatcherLog 成型调度员排程操作日志
     * @return 成型调度员排程操作日志集合
     */
    public List<CxDispatcherLog> selectCxDispatcherLogList(CxDispatcherLog cxDispatcherLog);

    /**
     * 新增成型调度员排程操作日志
     * 
     * @param cxDispatcherLog 成型调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertCxDispatcherLog(CxDispatcherLog cxDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(CxDispatcherLog dispatcherLog);
}
