package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型排程任务时间Mapper接口
 * 
 * @author Joran.zhang
 * @date 2022-05-17
 */
public interface CxScheduleTaskTimeMapper 
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
    public int insertCxScheduleTaskTime(CxScheduleTaskTime cxScheduleTaskTime);

    /**
     * 批量新增成型任务时间
     * @param cxScheduleTaskTimeList
     * @return
     */
    public int batchInsertCxScheduleTaskTime(@Param("list") List<CxScheduleTaskTime> cxScheduleTaskTimeList);

    /**
     * 修改成型排程任务时间
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 结果
     */
    public int updateCxScheduleTaskTime(CxScheduleTaskTime cxScheduleTaskTime);

    /**
     * 删除成型排程任务时间
     * 
     * @param id 成型排程任务时间ID
     * @return 结果
     */
    public int deleteCxScheduleTaskTimeById(Long id);

    /**
     * 批量删除成型排程任务时间
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxScheduleTaskTimeByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxScheduleTaskTime> list);

    /**
     * 根据排程日期进行删除
     * @param scheduleDate
     * @return
     */
    int deleteCxScheduleTaskTimeByScheduleDate(@Param("scheduleDate") String scheduleDate);
}
