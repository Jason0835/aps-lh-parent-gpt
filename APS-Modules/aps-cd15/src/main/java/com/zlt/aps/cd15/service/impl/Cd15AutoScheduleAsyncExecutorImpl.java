package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15MultiShiftScheduleExecutor;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.service.Cd15AutoScheduleAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15AutoSchedulePersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** CD15自动排程异步执行实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleAsyncExecutorImpl implements Cd15AutoScheduleAsyncExecutor {

    private static final String AGING_PERIOD_PARAM_CODE = "SYS0601032";
    private static final int DEFAULT_AGING_PERIOD_HOURS = 24;

    private final Cd15AutoScheduleLockService lockService;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleInputService inputService;
    private final Cd15MultiShiftScheduleExecutor multiShiftScheduleExecutor;
    private final Cd15AutoSchedulePersistService persistService;
    private final Cd15ParamsMapper paramsMapper;

    @Async
    @Override
    public void execute(String taskId, String factoryCode, Date scheduleDate) {
        LocalDate localDate = this.toLocalDate(scheduleDate);
        RLock lock = lockService.getLock(factoryCode, localDate);
        try {
            if (!lock.tryLock()) {
                log.info("[斜裁自动排程] 执行锁已被其它任务持有, taskId={}, factoryCode={}, scheduleDate={}",
                        taskId, factoryCode, localDate);
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[斜裁自动排程] 任务已被其它执行者处理, taskId={}", taskId);
                return;
            }
            this.updateProgress(taskId, 15, "LOAD_INPUT", "加载自动排程输入");
            Cd15AutoScheduleInput input = inputService.load(factoryCode, localDate, "CLASS1", "01",
                    this.resolveAgingPeriodHours(factoryCode));
            this.updateProgress(taskId, 40, "SCHEDULE_TRIAL", "执行自动排程试排");
            Cd15ScheduleProgressListener listener = (progress, stage, stageName) ->
                    this.updateProgress(taskId, progress, stage, stageName);
            Cd15MultiShiftScheduleResult output = multiShiftScheduleExecutor.execute(input, listener);
            this.updateProgress(taskId, 90, "PERSIST", "保存自动排程结果");
            persistService.persist(taskId, factoryCode, localDate, output, lock);
        } catch (Exception exception) {
            log.error("[斜裁自动排程] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, factoryCode, localDate, exception);
            taskService.markFailed(taskId, exception.getMessage());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void updateProgress(String taskId, int progress, String stage, String stageName) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("CD15自动排程任务状态已变化，停止当前执行");
        }
        if (!taskService.updateProgress(taskId, progress, stage, stageName)) {
            log.warn("[斜裁自动排程] 任务进度更新失败但继续执行, taskId={}, progress={}, stage={}",
                    taskId, progress, stage);
        }
    }

    private int resolveAgingPeriodHours(String factoryCode) {
        Cd15Params param = paramsMapper.selectOne(Wrappers.<Cd15Params>lambdaQuery()
                .eq(Cd15Params::getFactoryCode, factoryCode)
                .eq(Cd15Params::getParamCode, AGING_PERIOD_PARAM_CODE)
                .last("limit 1"));
        if (param == null || param.getParamValue() == null || param.getParamValue().trim().isEmpty()) {
            return DEFAULT_AGING_PERIOD_HOURS;
        }
        try {
            return Math.max(0, Integer.parseInt(param.getParamValue().trim()));
        } catch (NumberFormatException exception) {
            return DEFAULT_AGING_PERIOD_HOURS;
        }
    }

    private LocalDate toLocalDate(Date value) {
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}