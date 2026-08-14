package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStage;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.mapper.Cd15ScheduleTaskMapper;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15RollingScheduleTaskService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.redisson.api.RLock;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/** 定时滚动排程任务幂等服务实现。 */
@Service
@RequiredArgsConstructor
public class Cd15RollingScheduleTaskServiceImpl implements Cd15RollingScheduleTaskService {

    private final Cd15ScheduleTaskMapper taskMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleLockService lockService;

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
        RLock submissionLock = lockService.getLock(factoryCode,
                this.toLocalDate(scheduleDate));
        boolean releaseAfterTransaction = false;
        try {
            if (!submissionLock.tryLock()) {
                return null;
            }
            Cd15ScheduleTask activeTask = taskService.findActive(factoryCode, scheduleDate);
            if (activeTask != null) {
                return null;
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
            releaseAfterTransaction = this.releaseLockAfterTransaction(submissionLock);
            return task;
        } finally {
            if (!releaseAfterTransaction && submissionLock.isHeldByCurrentThread()) {
                submissionLock.unlock();
            }
        }
    }

    /** 事务提交或回滚后释放提交锁，避免未提交任务被下一次检查越过。 */
    private boolean releaseLockAfterTransaction(RLock submissionLock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (submissionLock.isHeldByCurrentThread()) {
                            submissionLock.unlock();
                        }
                    }
                });
        return true;
    }

    /** 将排程日期转换为分布式锁使用的本地日期。 */
    private LocalDate toLocalDate(Date scheduleDate) {
        if (scheduleDate instanceof java.sql.Date) {
            return ((java.sql.Date) scheduleDate).toLocalDate();
        }
        return scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
