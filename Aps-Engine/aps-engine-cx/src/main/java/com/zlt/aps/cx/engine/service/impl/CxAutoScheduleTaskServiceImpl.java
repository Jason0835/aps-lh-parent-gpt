package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxAutoScheduleTask;
import com.zlt.aps.cx.engine.mapper.CxAutoScheduleTaskMapper;
import com.zlt.aps.cx.engine.service.CxAutoScheduleTaskService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * 成型自动排程任务逻辑层实现类
  * @ClassName CxAutoScheduleTaskServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 18:27
  * @Version 1.0
**/
@Service("cxAutoScheduleTaskService")
@Slf4j
public class CxAutoScheduleTaskServiceImpl implements CxAutoScheduleTaskService {

    @Autowired
    private CxAutoScheduleTaskMapper cxAutoScheduleTaskMapper;
    @Override
    public List<CxAutoScheduleTask> selectCxAutoScheduleTaskList(CxAutoScheduleTask cxAutoScheduleTask) {
        return cxAutoScheduleTaskMapper.selectCxAutoScheduleTaskList(cxAutoScheduleTask);
    }

    /**
     * 批量创建自动排程任务
     * @param inertTaskList
     * @return
     */
    @Override
    public int batchInertCxAutoScheduleTask(List<CxAutoScheduleTask> inertTaskList) {
        return cxAutoScheduleTaskMapper.insertCxAutoScheduleTaskList(inertTaskList);
    }

    /**
     * 更新自动排程任务
     * @param cxAutoScheduleTask
     * @return
     */
    @Override
    public int updateCxAutoScheduleTask(CxAutoScheduleTask cxAutoScheduleTask) {
        return cxAutoScheduleTaskMapper.updateCxAutoScheduleTask(cxAutoScheduleTask);
    }

    @Override
    public int deleteCxAutoScheduleTaskById(Long id) {
        return cxAutoScheduleTaskMapper.deleteCxAutoScheduleTaskById(id);
    }

    @Override
    public int deleteCxAutoScheduleTaskByIds(Long[] ids) {
        return cxAutoScheduleTaskMapper.deleteCxAutoScheduleTaskByIds(ids);
    }

    @Override
    public Map<String, CxAutoScheduleTask> loadLastDayTaskMap(String lastDateStr) {
        Map<String,CxAutoScheduleTask> lastDayTaskMap=null;
        CxAutoScheduleTask condition=new CxAutoScheduleTask();
        condition.setScheduleDate(lastDateStr);
        List<CxAutoScheduleTask> lastDayTaskList=this.selectCxAutoScheduleTaskList(condition);
        if(StringUtils.isNotEmpty(lastDayTaskList)){
            lastDayTaskMap=new HashMap<>();
            for(CxAutoScheduleTask cxAutoScheduleTask:lastDayTaskList){
                if(cxAutoScheduleTask.getRemainTaskQty()>0L){//去掉没有任务剩余量的任务数据
                    String mapKey= CxScheduleUtils.getMapKeyByInputString(cxAutoScheduleTask.getCxOrderNo(),cxAutoScheduleTask.getClassShift()+"");
                    lastDayTaskMap.put(mapKey,cxAutoScheduleTask);
                }

            }
        }
        return lastDayTaskMap;
    }
}
