package com.zlt.aps.xwyy.service;

import com.zlt.aps.xwyy.api.domain.entity.XwyyDispatcherLog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 纤维压延调度员排程操作日志Service接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface XwyyDispatcherLogService
{
    /**
     * 查询纤维压延调度员排程操作日志
     * 
     * @param id 纤维压延调度员排程操作日志ID
     * @return 纤维压延调度员排程操作日志
     */
    public XwyyDispatcherLog selectXwyyDispatcherLogById(Long id);

    /**
     * 查询纤维压延调度员排程操作日志列表
     * 
     * @param xwyyDispatcherLog 纤维压延调度员排程操作日志
     * @return 纤维压延调度员排程操作日志集合
     */
    public List<XwyyDispatcherLog> selectXwyyDispatcherLogList(XwyyDispatcherLog xwyyDispatcherLog);

    /**
     * 新增纤维压延调度员排程操作日志
     * 
     * @param xwyyDispatcherLog 纤维压延调度员排程操作日志
     * @return 结果
     */
    @Transactional
    public int insertXwyyDispatcherLog(XwyyDispatcherLog xwyyDispatcherLog);

    /**
     * 导出Excel
     * @param dispatcherLog 参数
     * @return 字节数组
     */
    public byte[] export(XwyyDispatcherLog dispatcherLog);
}
