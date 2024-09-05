package com.zlt.aps.cx.engine.mapper;


import com.zlt.aps.cx.engine.domain.CxAutoScheduleTask;

import java.util.List;

/**
  * 成型工序任务表
  * @ClassName CxAutoScheduleTaskMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 17:40
  * @Version 1.0
**/
public interface CxAutoScheduleTaskMapper {

    /**
     * 根据条件加载排程任务表
     * @param cxAutoScheduleTask
     * @return
     */
   public List<CxAutoScheduleTask> selectCxAutoScheduleTaskList(CxAutoScheduleTask cxAutoScheduleTask);

    /**
     * 批量插入排程任务
     * @param insertTaskList
     * @return
     */
   public int insertCxAutoScheduleTaskList(List<CxAutoScheduleTask> insertTaskList);

    /**
     * 更新排程任务
     * @param cxAutoScheduleTask
     * @return
     */
   public int updateCxAutoScheduleTask(CxAutoScheduleTask cxAutoScheduleTask);

    /**
     * 删除排程任务
     *
     * @param id 排程任务ID
     * @return 结果
     */
    public int deleteCxAutoScheduleTaskById(Long id);

    /**
     * 批量删除排程任务
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxAutoScheduleTaskByIds(Long[] ids);
}
