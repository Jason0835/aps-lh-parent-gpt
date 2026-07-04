package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStage;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskType;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.mapper.Cd90ScheduleTaskMapper;
import com.zlt.aps.cd90.engine.service.Cd90RollingScheduleTaskService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/** 定时滚动排程任务幂等服务实现。 */
@Service
@RequiredArgsConstructor
public class Cd90RollingScheduleTaskServiceImpl implements Cd90RollingScheduleTaskService {

    private final Cd90ScheduleTaskMapper taskMapper;
    private final Cd90ScheduleTaskService taskService;

    @Override
    public Cd90ScheduleTask findSuccessfulByIdempotencyKey(String factoryCode,
                                                            String idempotencyKey) {
        if (isBlank(factoryCode) || isBlank(idempotencyKey)) {
            return null;
        }
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd90ScheduleTask::getTaskType, Cd90ScheduleTaskType.ROLLING_SCHEDULE)
                .eq(Cd90ScheduleTask::getIdempotencyKey, idempotencyKey)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.SUCCESS)
                .orderByDesc(Cd90ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd90ScheduleTask createPending(String factoryCode, Date scheduleDate,
                                          String requestSnapshot, String idempotencyKey) {
        Cd90ScheduleTask activeTask = taskService.findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            return activeTask;
        }
        Cd90ScheduleTask task = new Cd90ScheduleTask();
        task.setTaskId("CD90-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        task.setFactoryCode(factoryCode);
        task.setScheduleDate(scheduleDate);
        task.setTaskType(Cd90ScheduleTaskType.ROLLING_SCHEDULE);
        task.setIdempotencyKey(idempotencyKey);
        task.setTriggerType("TIMER");
        task.setTaskStatus(Cd90ScheduleTaskStatus.PENDING);
        task.setProgress(0);
        task.setCurrentStage(Cd90ScheduleTaskStage.VALIDATE_DATA);
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
