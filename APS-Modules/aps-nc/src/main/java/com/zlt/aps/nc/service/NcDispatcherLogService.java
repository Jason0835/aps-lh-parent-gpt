package com.zlt.aps.nc.service;

import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 内衬调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface NcDispatcherLogService
{
    /**
     * 查询内衬调度员排程操作日志
     * 
     * @param id 内衬调度员排程操作日志ID
     * @return 内衬调度员排程操作日志
     */
    public NcDispatcherLog selectNcDispatcherLogById(Long id);

    /**
     * 查询内衬调度员排程操作日志列表
     * 
     * @param ncDispatcherLog 内衬调度员排程操作日志
     * @return 内衬调度员排程操作日志集合
     */
    public List<NcDispatcherLog> selectNcDispatcherLogList(NcDispatcherLog ncDispatcherLog);

    /**
     * 新增内衬调度员排程操作日志
     * 
     * @param ncDispatcherLog 内衬调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertNcDispatcherLog(NcDispatcherLog ncDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(NcDispatcherLog dispatcherLog);
}
