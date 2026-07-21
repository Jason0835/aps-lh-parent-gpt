package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStage;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.mapper.Cd15ScheduleTaskMapper;
import com.zlt.aps.cd15.engine.service.Cd15RollingScheduleTaskService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/** 定时滚动排程任务幂等服务实现。 */
@Service
@RequiredArgsConstructor
public class Cd15RollingScheduleTaskServiceImpl implements Cd15RollingScheduleTaskService {

    private final Cd15ScheduleTaskMapper taskMapper;
    private final Cd15ScheduleTaskService taskService;

    @Override
    public Cd15ScheduleTask findSuccessfulByIdempotencyKey(String factoryCode,
                                                            String idempotencyKey) {
        if (isBlank(factoryCode) || isBlank(idempotencyKey)) {
            return null;
        }
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleTask::getTaskType, Cd15ScheduleTaskType.ROLLING_SCHEDULE)
                .eq(Cd15ScheduleTask::getIdempotencyKey, idempotencyKey)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.SUCCESS)
                .orderByDesc(Cd15ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd15ScheduleTask createPending(String factoryCode, Date scheduleDate,
                                          String requestSnapshot, String idempotencyKey) {
        Cd15ScheduleTask activeTask = taskService.findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            return activeTask;
        }
        Cd15ScheduleTask task = new Cd15ScheduleTask();
        task.setTaskId("CD15-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        task.setFactoryCode(factoryCode);
        task.setScheduleDate(scheduleDate);
        task.setTaskType(Cd15ScheduleTaskType.ROLLING_SCHEDULE);
        task.setIdempotencyKey(idempotencyKey);
        task.setTriggerType("TIMER");
        task.setTaskStatus(Cd15ScheduleTaskStatus.PENDING);
        task.setProgress(0);
        task.setCurrentStage(Cd15ScheduleTaskStage.VALIDATE_DATA);
        task.setCurrentStageName("等待滚动排程数据校验");
        task.setRequestSnapshot(requestSnapshot);
        task.setCreateBy("aps-job");
        taskMapper.insert(task);
        return task;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
