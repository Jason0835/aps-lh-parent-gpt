package com.zlt.kettle.service;

import com.zlt.kettle.api.domain.TaskInfo;
import com.zlt.kettle.mapper.KettleTaskInfoMapper;
import org.kettle.scheduler.common.povo.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public interface KettleTaskInfoService {
    /**
     * 读取任务信息
     * @param taskInfo
     * @return
     */
    List<TaskInfo> getTaskInfoList(TaskInfo taskInfo);

    /**
     * 执行一个任务
     * @param id
     * @param taskType
     * @return
     */
    Result runOneTaskInfo(Integer id, String taskType);
}
