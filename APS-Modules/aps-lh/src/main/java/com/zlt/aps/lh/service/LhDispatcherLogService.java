package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 硫化调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface LhDispatcherLogService
{
    /**
     * 查询硫化调度员排程操作日志
     * 
     * @param id 硫化调度员排程操作日志ID
     * @return 硫化调度员排程操作日志
     */
    public LhDispatcherLog selectLhDispatcherLogById(Long id);

    /**
     * 查询硫化调度员排程操作日志列表
     * 
     * @param lhDispatcherLog 硫化调度员排程操作日志
     * @return 硫化调度员排程操作日志集合
     */
    public List<LhDispatcherLog> selectLhDispatcherLogList(LhDispatcherLog lhDispatcherLog);

    /**
     * 新增硫化调度员排程操作日志
     * 
     * @param lhDispatcherLog 硫化调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertLhDispatcherLog(LhDispatcherLog lhDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(LhDispatcherLog dispatcherLog);
}
