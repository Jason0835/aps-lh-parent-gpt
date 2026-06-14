package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.service.Cd90AutoScheduleAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90AutoSchedulePersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** 直裁自动排程异步执行实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleAsyncExecutorImpl implements Cd90AutoScheduleAsyncExecutor {

    private final Cd90AutoScheduleLockService lockService;
    private final Cd90ScheduleTaskService taskService;
    private final Cd90AutoScheduleEngineService engineService;
    private final Cd90AutoSchedulePersistService persistService;

    @Async
    @Override
    public void execute(String taskId, String factoryCode, Date scheduleDate) {
        LocalDate localDate = scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        RLock lock = lockService.getLock(factoryCode, localDate);
        try {
            if (!lock.tryLock()) {
                log.info("[直裁自动排程] 执行锁已由其他任务持有, taskId={}, factoryCode={}, scheduleDate={}",
                        taskId, factoryCode, localDate);
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[直裁自动排程] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            Cd90AutoScheduleContext context = engineService.prepare(factoryCode, scheduleDate);
            taskService.updateProgress(taskId, 20, "CALCULATE", "执行多班自动排程");
            Cd90AutoScheduleOutputDraft output = engineService.execute(context);
            taskService.updateProgress(taskId, 95, "PERSIST", "保存自动排程结果");
            persistService.persist(taskId, context, output, lock);
        } catch (Exception exception) {
            log.error("[直裁自动排程] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, factoryCode, localDate, exception);
            taskService.markFailed(taskId, exception.getMessage());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
