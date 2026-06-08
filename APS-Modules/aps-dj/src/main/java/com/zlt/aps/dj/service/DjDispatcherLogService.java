package com.zlt.aps.dj.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;

/**
 * 垫胶调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface DjDispatcherLogService
{
    /**
     * 查询垫胶调度员排程操作日志
     * 
     * @param id 垫胶调度员排程操作日志ID
     * @return 垫胶调度员排程操作日志
     */
    public DjDispatcherLog selectNcDispatcherLogById(Long id);

    /**
     * 查询垫胶调度员排程操作日志列表
     * 
     * @param ncDispatcherLog 垫胶调度员排程操作日志
     * @return 垫胶调度员排程操作日志集合
     */
    public List<DjDispatcherLog> selectNcDispatcherLogList(DjDispatcherLog ncDispatcherLog);

    /**
     * 新增垫胶调度员排程操作日志
     * 
     * @param ncDispatcherLog 垫胶调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertNcDispatcherLog(DjDispatcherLog ncDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(DjDispatcherLog dispatcherLog);
}
