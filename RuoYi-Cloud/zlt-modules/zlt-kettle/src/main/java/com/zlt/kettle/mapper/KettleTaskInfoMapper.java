package com.zlt.kettle.mapper;

import com.zlt.kettle.api.domain.TaskInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KettleTaskInfoMapper {
    /**
     * 读取任务信息
     * @param taskInfo
     * @return
     */
    List<TaskInfo> getTaskInfoList(TaskInfo taskInfo);

    /**
     * 根据ID读取TaskInfo
     * @param id
     * @param taskType
     * @return
     */
    TaskInfo selectOneRecord(@Param("id") Integer id, @Param("taskType") String taskType);
}
