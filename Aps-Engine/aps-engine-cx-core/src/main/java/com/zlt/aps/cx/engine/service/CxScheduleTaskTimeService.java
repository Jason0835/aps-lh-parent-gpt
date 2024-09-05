package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

import java.util.Date;
import java.util.List;

/**
 * 成型排程任务时间Service接口
 * 
 * @author Joran.zhang
 * @date 2022-05-17
 */
public interface CxScheduleTaskTimeService
{
    /**
     * 查询成型排程任务时间
     * 
     * @param id 成型排程任务时间ID
     * @return 成型排程任务时间
     */
    public CxScheduleTaskTime selectCxScheduleTaskTimeById(Long id);

    /**
     * 查询成型排程任务时间列表
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 成型排程任务时间集合
     */
    public List<CxScheduleTaskTime> selectCxScheduleTaskTimeList(CxScheduleTaskTime cxScheduleTaskTime);

    /**
     * 新增成型排程任务时间
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 结果
     */
    @Transactional
    public int insertCxScheduleTaskTime(CxScheduleTaskTime cxScheduleTaskTime);


    /**
     * 批量新增成型任务计划时间
     * @param cxScheduleTaskTimeList
     * @return
     */
    public int batchInsertCxScheduleTaskTime(List<CxScheduleTaskTime> cxScheduleTaskTimeList);

    /**
     * 修改成型排程任务时间
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 结果
     */
    @Transactional
    public int updateCxScheduleTaskTime(CxScheduleTaskTime cxScheduleTaskTime);

    /**
     * 批量删除成型排程任务时间
     * 
     * @param ids 需要删除的成型排程任务时间ID
     * @return 结果
     */
    @Transactional
    public int deleteCxScheduleTaskTimeByIds(Long[] ids);

    /**
     * 删除成型排程任务时间信息
     * 
     * @param id 成型排程任务时间ID
     * @return 结果
     */
    @Transactional
    public int deleteCxScheduleTaskTimeById(Long id);

    /**
     * 校验成型排程任务时间唯一性
     */
    public String checkCxScheduleTaskTimeUnique(CxScheduleTaskTime cxScheduleTaskTime);

    /**
     * 导入成型排程任务时间数据
     */
    @Transactional
    public AjaxResult importData(List<CxScheduleTaskTime> list, boolean updateSupport, Long importLogId);

    /**
     * 根据排程日期进行删除
     * @param scheduleDate yyyyMMdd
     * @return
     */
    int deleteCxScheduleTaskTimeByScheduleDate(Date scheduleDate);
}
