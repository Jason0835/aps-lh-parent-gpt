package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStage;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.mapper.Cd90ScheduleTaskMapper;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.List;

/**
 * 直裁自动排程任务状态服务实现。
 *
 * <p>每次状态和进度变更均使用独立短事务，不包裹算法全过程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90ScheduleTaskServiceImpl implements Cd90ScheduleTaskService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final Cd90ScheduleTaskMapper taskMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd90ScheduleTask createPending(String factoryCode, Date scheduleDate, String triggerType,
                                          String requestSnapshot, String createBy) {
        Cd90ScheduleTask activeTask = findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            return activeTask;
        }

        Cd90ScheduleTask task = new Cd90ScheduleTask();
        task.setTaskId("CD90-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        task.setFactoryCode(factoryCode);
        task.setScheduleDate(scheduleDate);
        task.setTriggerType(triggerType);
        task.setTaskStatus(Cd90ScheduleTaskStatus.PENDING);
        task.setProgress(0);
        task.setCurrentStage(Cd90ScheduleTaskStage.VALIDATE_DATA);
        task.setCurrentStageName("等待基础数据校验");
        task.setRequestSnapshot(requestSnapshot);
        task.setCreateBy(createBy);
        taskMapper.insert(task);
        log.info("[直裁自动排程] 创建等待执行任务, taskId={}, factoryCode={}, scheduleDate={}, triggerType={}",
                task.getTaskId(), factoryCode, scheduleDate, triggerType);
        return task;
    }

    @Override
    public Cd90ScheduleTask findByTaskId(String taskId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId));
    }

    @Override
    public Cd90ScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd90ScheduleTask::getScheduleDate, scheduleDate)
                .orderByDesc(Cd90ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public Cd90ScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd90ScheduleTask::getScheduleDate, scheduleDate)
                .in(Cd90ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd90ScheduleTaskStatus.PENDING, Cd90ScheduleTaskStatus.RUNNING))
                .orderByDesc(Cd90ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        boolean updated = taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.PENDING)
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getProgress, 5)
                .set(Cd90ScheduleTask::getCurrentStage, Cd90ScheduleTaskStage.VALIDATE_DATA)
                .set(Cd90ScheduleTask::getCurrentStageName, "基础数据校验")
                .set(Cd90ScheduleTask::getStartTime, now)
                .set(Cd90ScheduleTask::getLastHeartbeatTime, now)) == 1;
        log.info("[直裁自动排程] 任务启动状态更新, taskId={}, updated={}", taskId, updated);
        return updated;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("自动排程任务进度必须在0至100之间");
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getProgress, progress)
                .set(Cd90ScheduleTask::getCurrentStage, stage)
                .set(Cd90ScheduleTask::getCurrentStageName, stageName)
                .set(Cd90ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, String batchNo) {
        return updateSuccess(taskId, batchNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessInCurrentTransaction(String taskId, String batchNo) {
        return updateSuccess(taskId, batchNo);
    }

    private boolean updateSuccess(String taskId, String batchNo) {
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.SUCCESS)
                .set(Cd90ScheduleTask::getProgress, 100)
                .set(Cd90ScheduleTask::getCurrentStage, Cd90ScheduleTaskStage.COMPLETE)
                .set(Cd90ScheduleTask::getCurrentStageName, "执行完成")
                .set(Cd90ScheduleTask::getBatchNo, batchNo)
                .set(Cd90ScheduleTask::getEndTime, new Date())
                .set(Cd90ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage) {
        String summary = errorMessage == null ? "自动排程执行失败" : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) {
            summary = summary.substring(0, MAX_ERROR_LENGTH);
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .in(Cd90ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd90ScheduleTaskStatus.PENDING, Cd90ScheduleTaskStatus.RUNNING))
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.FAILED)
                .set(Cd90ScheduleTask::getErrorMessage, summary)
                .set(Cd90ScheduleTask::getEndTime, new Date())
                .set(Cd90ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    public List<Cd90ScheduleTask> findRunningTasks(int limit) {
        int queryLimit = limit <= 0 ? 500 : Math.min(limit, 1000);
        return taskMapper.selectList(new LambdaQueryWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .orderByAsc(Cd90ScheduleTask::getLastHeartbeatTime)
                .last("limit " + queryLimit));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markTimeoutFailed(String taskId, String errorMessage) {
        String summary = errorMessage == null ? "自动排程任务心跳超时" : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) summary = summary.substring(0, MAX_ERROR_LENGTH);
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleTask>()
                .eq(Cd90ScheduleTask::getTaskId, taskId)
                .eq(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.RUNNING)
                .set(Cd90ScheduleTask::getTaskStatus, Cd90ScheduleTaskStatus.FAILED)
                .set(Cd90ScheduleTask::getCurrentStageName, "心跳超时补偿失败")
                .set(Cd90ScheduleTask::getErrorMessage, summary)
                .set(Cd90ScheduleTask::getEndTime, new Date())) == 1;
    }
}
