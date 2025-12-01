package com.zlt.mix.schedule.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.dto.ScheduleOperLogDto;
import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 排程操作日志Service接口
 *
 * @author chen
 * @date 2022-07-13
 */
public interface ScheduleOperLogService extends IService<ScheduleOperLog> {
    /**
     * 查询排程操作日志列表
     *
     * @param scheduleOperLog 排程操作日志
     * @return 排程操作日志集合
     */
    List<ScheduleOperLog> selectScheduleOperLogList(ScheduleOperLog scheduleOperLog);

    /**
     * 保存排程操作日志信息（id为空则新增，id不为空则修改）
     *
     * @param scheduleOperLog 要保存的数据
     */
    void saveScheduleOperLog(ScheduleOperLog scheduleOperLog);

    /**
     * 批量插入排程操作日志
     * @param list 要插入的记录
     */
    public void batchInsertScheduleOperLogInfo(List<ScheduleOperLog> list);

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    public byte[] exportData(ScheduleOperLogDto dto);
}
