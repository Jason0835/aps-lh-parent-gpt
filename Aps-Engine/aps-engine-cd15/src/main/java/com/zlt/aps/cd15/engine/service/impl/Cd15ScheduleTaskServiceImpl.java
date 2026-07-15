package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStage;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.mapper.Cd15ScheduleTaskMapper;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 斜裁自动排程任务状态服务实现。
 * <p>任务状态与 CD90 保持一致，算法执行链路后续补齐。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleTaskServiceImpl implements Cd15ScheduleTaskService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final Cd15ScheduleTaskMapper taskMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd15ScheduleTask createPending(String factoryCode, Date scheduleDate, String taskType,
                                          String triggerType, String requestSnapshot, String createBy) {
        return this.createPending(factoryCode, scheduleDate, taskType, triggerType,
                requestSnapshot, null, createBy, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd15ScheduleTask createPending(String factoryCode, Date scheduleDate, String taskType,
                                          String triggerType, String requestSnapshot,
                                          String inputVersion, String createBy) {
        return this.createPending(factoryCode, scheduleDate, taskType, triggerType,
                requestSnapshot, inputVersion, createBy, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Cd15ScheduleTask createPending(String factoryCode, Date scheduleDate, String taskType,
                                          String triggerType, String requestSnapshot,
                                          String inputVersion, String createBy,
                                          String idempotencyKey) {
        Cd15ScheduleTask activeTask = this.findActive(factoryCode, scheduleDate);
        if (activeTask != null) {
            return activeTask;
        }

        Cd15ScheduleTask task = new Cd15ScheduleTask();
        task.setTaskId("CD15-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        task.setFactoryCode(factoryCode);
        task.setScheduleDate(scheduleDate);
        task.setTaskType(taskType);
        task.setIdempotencyKey(idempotencyKey);
        task.setTriggerType(triggerType);
        task.setTaskStatus(Cd15ScheduleTaskStatus.PENDING);
        task.setProgress(0);
        task.setCurrentStage(Cd15ScheduleTaskStage.VALIDATE_DATA);
        task.setCurrentStageName("等待基础数据校验");
        task.setRequestSnapshot(requestSnapshot);
        task.setInputVersion(inputVersion);
        task.setCreateBy(createBy);
        taskMapper.insert(task);

        log.info("[斜裁排程任务] 创建等待执行任务, taskId={}, factoryCode={}, scheduleDate={}, taskType={}, triggerType={}",
                task.getTaskId(), factoryCode, scheduleDate, taskType, triggerType);
        return task;
    }

    @Override
    public Cd15ScheduleTask findSuccessfulByIdempotencyKey(String factoryCode, String taskType,
                                                           String idempotencyKey) {
        if (factoryCode == null || factoryCode.trim().isEmpty()
                || taskType == null || taskType.trim().isEmpty()
                || idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return null;
        }
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleTask::getTaskType, taskType)
                .eq(Cd15ScheduleTask::getIdempotencyKey, idempotencyKey)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.SUCCESS)
                .orderByDesc(Cd15ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public Cd15ScheduleTask findByTaskId(String taskId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId));
    }

    @Override
    public Cd15ScheduleTask findLatest(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleTask::getScheduleDate, scheduleDate)
                .orderByDesc(Cd15ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public Cd15ScheduleTask findActive(String factoryCode, Date scheduleDate) {
        return taskMapper.selectOne(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getFactoryCode, factoryCode)
                .eq(Cd15ScheduleTask::getScheduleDate, scheduleDate)
                .in(Cd15ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd15ScheduleTaskStatus.PENDING, Cd15ScheduleTaskStatus.RUNNING))
                .orderByDesc(Cd15ScheduleTask::getCreateTime)
                .last("limit 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String taskId) {
        Date now = new Date();
        boolean updated = taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.PENDING)
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getProgress, 5)
                .set(Cd15ScheduleTask::getCurrentStage, Cd15ScheduleTaskStage.VALIDATE_DATA)
                .set(Cd15ScheduleTask::getCurrentStageName, "基础数据校验")
                .set(Cd15ScheduleTask::getStartTime, now)
                .set(Cd15ScheduleTask::getLastHeartbeatTime, now)) == 1;
        log.info("[斜裁自动排程] 任务启动状态更新, taskId={}, updated={}", taskId, updated);
        return updated;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean updateProgress(String taskId, int progress, String stage, String stageName) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("自动排程任务进度必须在0到100之间");
        }
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getProgress, progress)
                .set(Cd15ScheduleTask::getCurrentStage, stage)
                .set(Cd15ScheduleTask::getCurrentStageName, stageName)
                .set(Cd15ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markSuccess(String taskId, String batchNo) {
        return this.updateSuccess(taskId, batchNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessInCurrentTransaction(String taskId, String batchNo) {
        return this.updateSuccess(taskId, batchNo);
    }

    private boolean updateSuccess(String taskId, String batchNo) {
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.SUCCESS)
                .set(Cd15ScheduleTask::getProgress, 100)
                .set(Cd15ScheduleTask::getCurrentStage, Cd15ScheduleTaskStage.COMPLETE)
                .set(Cd15ScheduleTask::getCurrentStageName, "执行完成")
                .set(Cd15ScheduleTask::getBatchNo, batchNo)
                .set(Cd15ScheduleTask::getEndTime, new Date())
                .set(Cd15ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markFailed(String taskId, String errorMessage) {
        String summary = this.truncateError(errorMessage, "自动排程执行失败");
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .in(Cd15ScheduleTask::getTaskStatus,
                        Arrays.asList(Cd15ScheduleTaskStatus.PENDING, Cd15ScheduleTaskStatus.RUNNING))
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.FAILED)
                .set(Cd15ScheduleTask::getErrorMessage, summary)
                .set(Cd15ScheduleTask::getEndTime, new Date())
                .set(Cd15ScheduleTask::getLastHeartbeatTime, new Date())) == 1;
    }

    @Override
    public List<Cd15ScheduleTask> findRunningTasks(int limit) {
        int queryLimit = limit <= 0 ? 500 : Math.min(limit, 1000);
        return taskMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .orderByAsc(Cd15ScheduleTask::getLastHeartbeatTime)
                .last("limit " + queryLimit));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean markTimeoutFailed(String taskId, String errorMessage) {
        String summary = this.truncateError(errorMessage, "自动排程任务心跳超时");
        return taskMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleTask>()
                .eq(Cd15ScheduleTask::getTaskId, taskId)
                .eq(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.RUNNING)
                .set(Cd15ScheduleTask::getTaskStatus, Cd15ScheduleTaskStatus.FAILED)
                .set(Cd15ScheduleTask::getCurrentStageName, "心跳超时补偿失败")
                .set(Cd15ScheduleTask::getErrorMessage, summary)
                .set(Cd15ScheduleTask::getEndTime, new Date())) == 1;
    }

    private String truncateError(String errorMessage, String defaultMessage) {
        String summary = errorMessage == null ? defaultMessage : errorMessage;
        if (summary.length() > MAX_ERROR_LENGTH) {
            return summary.substring(0, MAX_ERROR_LENGTH);
        }
        return summary;
    }
}