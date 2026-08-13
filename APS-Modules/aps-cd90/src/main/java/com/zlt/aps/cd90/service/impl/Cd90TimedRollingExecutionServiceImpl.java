package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90TimedRollingOutput;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.engine.service.Cd90TimedRollingService;
import com.zlt.aps.cd90.service.Cd90TimedRollingExecutionService;
import com.zlt.aps.cd90.service.Cd90TimedRollingPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

/** CD90定时滚动排程锁、Engine和持久化编排。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90TimedRollingExecutionServiceImpl
        implements Cd90TimedRollingExecutionService {

    private final Cd90AutoScheduleLockService lockService;
    private final Cd90ScheduleTaskService taskService;
    private final Cd90TimedRollingService rollingService;
    private final Cd90TimedRollingPersistService persistService;

    @Override
    public void execute(String taskId, Cd90RollingTarget target, String inputVersion) {
        RLock lock = lockService.getLock(target.getFactoryCode(), target.getScheduleDate());
        try {
            if (!lock.tryLock()) {
                taskService.markPendingFailed(taskId, "同排程日已有任务持有执行锁");
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[直裁定时滚动] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            Cd90TimedRollingOutput output = rollingService.execute(
                    target, inputVersion,
                    (progress, stage, stageName, shift) -> taskService.updateProgress(
                            taskId, progress, stage, stageName));
            persistService.persist(taskId, target, output, lock);
        } catch (Exception exception) {
            log.error("[直裁定时滚动] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
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
