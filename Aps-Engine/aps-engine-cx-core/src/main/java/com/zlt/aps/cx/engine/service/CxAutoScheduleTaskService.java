package com.zlt.aps.cx.engine.service;


import com.zlt.aps.cx.engine.domain.CxAutoScheduleTask;

import java.util.List;
import java.util.Map;

/**
  *  成型自动排程任务逻辑层接口
  * @ClassName CxAutoScheduleTaskService
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 18:23
  * @Version 1.0
**/
public interface CxAutoScheduleTaskService {

    /**
     * 根据条件加载成型自动排程任务
     * @param cxAutoScheduleTask
     * @return
     */
    public List<CxAutoScheduleTask> selectCxAutoScheduleTaskList(CxAutoScheduleTask cxAutoScheduleTask);

    /**
     * 批量添加成型自动排程任务
     * @param inertTaskList
     * @return
     */
    public int batchInertCxAutoScheduleTask(List<CxAutoScheduleTask> inertTaskList);

    /**
     * 更新自动排程任务信息
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

    /**
     * 加载昨日任务
     * @param lastDateStr
     * @return
     */
    public Map<String,CxAutoScheduleTask> loadLastDayTaskMap(String lastDateStr);



}
