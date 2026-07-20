package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleParameterService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ShiftConfigMapper;
import com.zlt.aps.cd15.model.Cd15TaskRecoveryResult;
import com.zlt.aps.cd15.service.Cd15ScheduleTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 遗留运行中任务补偿实现；调度频率由外部 Job 服务管理。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleTaskRecoveryServiceImpl
        implements Cd15ScheduleTaskRecoveryService {

    private static final int ACTIVE = 1;
    private static final int SCAN_LIMIT = 500;

    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleLockService lockService;
    private final Cd15AutoScheduleParameterService parameterService;
    private final Cd15ShiftConfigMapper shiftConfigMapper;

    @Override
    public Cd15TaskRecoveryResult recover(Integer timeoutMinutes) {
        if (timeoutMinutes != null && timeoutMinutes <= 0) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.cd15.taskRecovery.invalidTimeout"));
        }
        List<Cd15ScheduleTask> tasks = taskService.findRunningTasks(SCAN_LIMIT);
        Map<String, Integer> timeoutByFactory = new HashMap<>();
        int failedCount = 0;
        int skippedCount = 0;
        Instant currentTime = Instant.now();
        log.info("[斜裁自动排程] 遗留任务补偿扫描开始, runningCount={}, overrideTimeoutMinutes={}",
                tasks.size(), timeoutMinutes);
        for (Cd15ScheduleTask task : tasks) {
            int taskTimeoutMinutes = timeoutMinutes == null
                    ? timeoutByFactory.computeIfAbsent(
                            task.getFactoryCode(), this::loadTimeoutMinutes)
                    : timeoutMinutes;
            Date heartbeatTime = task.getLastHeartbeatTime() == null
                    ? task.getStartTime() : task.getLastHeartbeatTime();
            if (heartbeatTime == null || heartbeatTime.toInstant()
                    .plus(taskTimeoutMinutes, ChronoUnit.MINUTES)
                    .isAfter(currentTime)) {
                skippedCount++;
                continue;
            }
            LocalDate scheduleDate = this.toLocalDate(task.getScheduleDate());
            RLock executionLock = lockService.getLock(
                    task.getFactoryCode(), scheduleDate);
            if (executionLock.isLocked()) {
                skippedCount++;
                log.info("[斜裁自动排程] 遗留任务锁仍存在，跳过补偿, taskId={}, "
                                + "factoryCode={}, scheduleDate={}",
                        task.getTaskId(), task.getFactoryCode(), scheduleDate);
                continue;
            }
            String timeoutReason = MessageFormat.format(
                    I18nUtil.getMessage("ui.cd15.taskRecovery.timeoutReason"),
                    taskTimeoutMinutes);
            if (taskService.markTimeoutFailed(task.getTaskId(), timeoutReason)) {
                failedCount++;
                log.warn("[斜裁自动排程] 遗留任务已补偿为失败, taskId={}, timeoutMinutes={}",
                        task.getTaskId(), taskTimeoutMinutes);
            } else {
                skippedCount++;
            }
        }
        log.info("[斜裁自动排程] 遗留任务补偿扫描完成, scannedCount={}, "
                        + "failedCount={}, skippedCount={}",
                tasks.size(), failedCount, skippedCount);
        return Cd15TaskRecoveryResult.builder()
                .scannedCount(tasks.size())
                .failedCount(failedCount)
                .skippedCount(skippedCount)
                .build();
    }

    /** 按工厂参数读取任务超时分钟数。 */
    private int loadTimeoutMinutes(String factoryCode) {
        Long enabledShiftCount = shiftConfigMapper.selectCount(
                new LambdaQueryWrapper<Cd15ShiftConfig>()
                        .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd15ShiftConfig::getIsActive, ACTIVE));
        Cd15AutoScheduleParameters parameters = parameterService.load(
                factoryCode, enabledShiftCount.intValue());
        return parameters.getTaskTimeoutMinutes();
    }

    /** 转换任务排程日期，并兼容 MyBatis 返回的 java.sql.Date。 */
    private LocalDate toLocalDate(Date value) {
        if (value == null) {
            throw new IllegalStateException(I18nUtil.getMessage(
                    "ui.cd15.taskRecovery.missingScheduleDate"));
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
