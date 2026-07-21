package com.zlt.aps.cd15.service.impl;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.engine.service.Cd15TimedRollingService;
import com.zlt.aps.cd15.service.Cd15TimedRollingExecutionService;
import com.zlt.aps.cd15.service.Cd15TimedRollingPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

/** CD15定时滚动排程锁、Engine和持久化编排。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingExecutionServiceImpl
        implements Cd15TimedRollingExecutionService {

    private final Cd15AutoScheduleLockService lockService;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15TimedRollingService rollingService;
    private final Cd15TimedRollingPersistService persistService;

    @Override
    public void execute(String taskId, Cd15RollingTarget target, String inputVersion) {
        RLock lock = lockService.getLock(target.getFactoryCode(), target.getScheduleDate());
        try {
            if (!lock.tryLock()) {
                taskService.markFailed(taskId, "同排程日已有任务持有执行锁");
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[斜裁定时滚动] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            Cd15TimedRollingOutput output = rollingService.execute(
                    target, inputVersion,
                    (progress, stage, stageName, shift) -> taskService.updateProgress(
                            taskId, progress, stage, stageName));
            persistService.persist(taskId, target, output, lock);
        } catch (Exception exception) {
            log.error("[斜裁定时滚动] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, target.getFactoryCode(), target.getScheduleDate(), exception);
            taskService.markFailed(taskId, safeMessage(exception));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String safeMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null
                || exception.getMessage().trim().isEmpty()) {
            return "定时滚动排程执行失败";
        }
        return exception.getMessage();
    }
}
