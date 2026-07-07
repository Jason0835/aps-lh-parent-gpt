package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.api.domain.vo.Cd90ChangeQtyRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90TransferMachineRequest;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90InsertRollingService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.service.Cd90InsertOrderAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90InsertRollingPersistService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 直裁插单异步执行器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90InsertOrderAsyncExecutorImpl implements Cd90InsertOrderAsyncExecutor {

    private final Cd90AutoScheduleLockService lockService;
    private final Cd90ScheduleTaskService taskService;
    private final Cd90InsertRollingService insertRollingService;
    private final Cd90InsertRollingPersistService persistService;

    @Async
    @Override
    public void execute(String taskId, Cd90InsertOrderRequest request) {
        LocalDate scheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                log.info("[直裁插单] 执行锁已由其他任务持有, taskId={}, factoryCode={}, scheduleDate={}",
                        taskId, request.getFactoryCode(), scheduleDate);
                taskService.markFailed(taskId,
                        I18nUtil.getMessage("ui.cd90.insert.activeTask"));
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[直裁插单] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            this.updateProgress(taskId, 20, "LOAD_EXISTING", "加载原排程任务链");
            Cd90InsertRollingOutput output = insertRollingService.execute(request);
            this.updateProgress(taskId, 90, "ROLLING_COMPLETE", "插单滚动重排完成");
            persistService.persist(taskId, request, output, lock);
        } catch (Exception exception) {
            log.error("[直裁插单] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, request.getFactoryCode(), scheduleDate, exception);
            taskService.markFailed(taskId, I18nUtil.getMessage("ui.cd90.insert.executeFailed"));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Async
    @Override
    public void executeTransfer(String taskId, Cd90TransferMachineRequest request) {
        LocalDate scheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                log.info("[直裁转机台] 执行锁已由其他任务持有, taskId={}, factoryCode={}, scheduleDate={}",
                        taskId, request.getFactoryCode(), scheduleDate);
                taskService.markFailed(taskId,
                        I18nUtil.getMessage("ui.cd90.insert.activeTask"));
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[直裁转机台] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            this.updateProgress(taskId, 20, "LOAD_EXISTING", "加载原排程任务链");
            Cd90InsertRollingOutput output = insertRollingService.executeTransfer(request);
            this.updateProgress(taskId, 90, "ROLLING_COMPLETE", "转机台滚动重排完成");
            persistService.persistTransfer(taskId, request, output, lock);
        } catch (Exception exception) {
            log.error("[直裁转机台] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, request.getFactoryCode(), scheduleDate, exception);
            taskService.markFailed(taskId, I18nUtil.getMessage("ui.cd90.insert.executeFailed"));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    @Async
    @Override
    public void executeChangeQty(String taskId, Cd90ChangeQtyRequest request) {
        LocalDate scheduleDate = request.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        RLock lock = lockService.getLock(request.getFactoryCode(), scheduleDate);
        try {
            if (!lock.tryLock()) {
                log.info("[直裁调量] 执行锁已由其他任务持有, taskId={}, factoryCode={}, scheduleDate={}",
                        taskId, request.getFactoryCode(), scheduleDate);
                taskService.markFailed(taskId,
                        I18nUtil.getMessage("ui.cd90.insert.activeTask"));
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[直裁调量] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            this.updateProgress(taskId, 20, "LOAD_EXISTING", "加载原排程任务链");
            Cd90InsertRollingOutput output = insertRollingService.executeChangeQty(request);
            this.updateProgress(taskId, 90, "ROLLING_COMPLETE", "调量滚动重排完成");
            persistService.persistChangeQty(taskId, request, output, lock);
        } catch (Exception exception) {
            log.error("[直裁调量] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, request.getFactoryCode(), scheduleDate, exception);
            taskService.markFailed(taskId, I18nUtil.getMessage("ui.cd90.insert.executeFailed"));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    private void updateProgress(String taskId, int progress, String stage, String stageName) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("插单任务状态已变化，停止当前执行");
        }
        if (!taskService.updateProgress(taskId, progress, stage, stageName)) {
            log.warn("[直裁插单] 任务心跳更新失败但继续执行, taskId={}, progress={}, stage={}",
                    taskId, progress, stage);
        }
    }
}
