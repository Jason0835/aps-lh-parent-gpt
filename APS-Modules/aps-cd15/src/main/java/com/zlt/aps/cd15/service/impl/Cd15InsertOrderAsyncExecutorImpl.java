package com.zlt.aps.cd15.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.service.Cd15InsertOrderAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15InsertRollingPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/** CD15斜裁人工调整异步执行器。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15InsertOrderAsyncExecutorImpl implements Cd15InsertOrderAsyncExecutor {

    private final Cd15AutoScheduleLockService lockService;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15InsertRollingPersistService persistService;

    @Async
    @Override
    public void execute(String taskId, Cd15InsertOrderRequest request) {
        this.executeInternal(taskId, request.getFactoryCode(), this.localDate(request.getScheduleDate()),
                "插单", lock -> persistService.persist(taskId, request, lock));
    }

    @Async
    @Override
    public void executeTransfer(String taskId, Cd15TransferMachineRequest request) {
        this.executeInternal(taskId, request.getFactoryCode(), this.localDate(request.getScheduleDate()),
                "转机台", lock -> persistService.persistTransfer(taskId, request, lock));
    }

    @Async
    @Override
    public void executeChangeQty(String taskId, Cd15ChangeQtyRequest request) {
        this.executeInternal(taskId, request.getFactoryCode(), this.localDate(request.getScheduleDate()),
                "调量", lock -> persistService.persistChangeQty(taskId, request, lock));
    }

    private void executeInternal(String taskId, String factoryCode, LocalDate scheduleDate,
                                 String actionName, PersistAction persistAction) {
        RLock lock = lockService.getLock(factoryCode, scheduleDate);
        try {
            if (!lock.tryLock()) {
                taskService.markFailed(taskId, "CD15" + actionName + "执行锁已被占用");
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[CD15{}] 任务已被其他执行者处理, taskId={}", actionName, taskId);
                return;
            }
            this.updateProgress(taskId, 20, "LOAD_EXISTING", "加载原排程任务链");
            this.updateProgress(taskId, 80, "ROLLING_COMPLETE", actionName + "后续滚动重排提交中");
            persistAction.persist(lock);
        } catch (Exception exception) {
            log.error("[CD15{}] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    actionName, taskId, factoryCode, scheduleDate, exception);
            taskService.markFailed(taskId, I18nUtil.getMessage("ui.message.operation.failed"));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void updateProgress(String taskId, int progress, String stage, String stageName) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("CD15人工调整任务状态已变化，停止当前执行");
        }
        taskService.updateProgress(taskId, progress, stage, stageName);
    }

    private LocalDate localDate(java.util.Date scheduleDate) {
        return scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private interface PersistAction {
        void persist(RLock lock);
    }
}