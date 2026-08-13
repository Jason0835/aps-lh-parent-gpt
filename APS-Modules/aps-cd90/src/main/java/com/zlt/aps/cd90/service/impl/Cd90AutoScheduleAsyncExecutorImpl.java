package com.zlt.aps.cd90.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleProgressListener;
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
                taskService.markPendingFailed(taskId,
                        I18nUtil.getMessage("ui.cd90.schedule.taskActive"));
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[直裁自动排程] 任务已被其他执行者处理, taskId={}", taskId);
                return;
            }
            // 先基于工厂和排程日期准备自动排程上下文，加载后续算法执行所需的参数、班次窗口和输入版本快照。
            Cd90AutoScheduleContext context = engineService.prepare(factoryCode, scheduleDate);
            updateProgress(taskId, 15, "PREPARE", "自动排程上下文准备完成");

            // 注册排程过程中的进度回调，将引擎内部各阶段的进度持续同步回任务表，
            // 便于前端轮询查看当前执行阶段和完成百分比。
            Cd90ScheduleProgressListener listener = (progress, stage, stageName, shift) ->
                    updateProgress(taskId, progress, stage, stageName);

            // 执行直裁自动排程引擎，生成尚未落库的排程输出草稿。
            Cd90AutoScheduleOutputDraft output = engineService.execute(context, listener);
            updateProgress(taskId, 95, "PERSIST", "保存自动排程结果");

            // 将排程结果草稿落库，并在持久化阶段复用当前分布式锁，
            // 确保结果写入和旧数据替换过程不被并发任务打断。
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

    private void updateProgress(String taskId, int progress, String stage, String stageName) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("自动排程任务状态已变化，停止当前执行");
        }
        if (!taskService.updateProgress(taskId, progress, stage, stageName)) {
            log.warn("[直裁自动排程] 任务心跳更新失败但继续执行, taskId={}, progress={}, stage={}",
                    taskId, progress, stage);
        }
    }
}
