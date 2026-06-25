package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleParameterService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ShiftConfigMapper;
import com.zlt.aps.cd90.model.Cd90TaskRecoveryResult;
import com.zlt.aps.cd90.service.Cd90ScheduleTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 遗留RUNNING任务补偿实现，不配置内部定时器。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90ScheduleTaskRecoveryServiceImpl implements Cd90ScheduleTaskRecoveryService {

    private static final int ACTIVE = 1;
    private static final int SCAN_LIMIT = 500;

    private final Cd90ScheduleTaskService taskService;
    private final Cd90AutoScheduleLockService lockService;
    private final Cd90AutoScheduleParameterService parameterService;
    private final Cd90ShiftConfigMapper shiftConfigMapper;

    @Override
    public Cd90TaskRecoveryResult recover(Integer timeoutMinutes) {
        if (timeoutMinutes != null && timeoutMinutes <= 0) {
            throw new IllegalArgumentException("自动排程补偿超时分钟数必须大于0");
        }
        List<Cd90ScheduleTask> tasks = taskService.findRunningTasks(SCAN_LIMIT);
        Map<String, Integer> timeoutByFactory = new HashMap<>();
        int failed = 0;
        int skipped = 0;
        Instant now = Instant.now();
        log.info("[直裁自动排程] 遗留任务补偿扫描开始, runningCount={}, overrideTimeoutMinutes={}",
                tasks.size(), timeoutMinutes);
        for (Cd90ScheduleTask task : tasks) {
            int taskTimeout = timeoutMinutes == null
                    ? timeoutByFactory.computeIfAbsent(task.getFactoryCode(), this::loadTimeoutMinutes)
                    : timeoutMinutes;
            Date heartbeat = task.getLastHeartbeatTime() == null
                    ? task.getStartTime() : task.getLastHeartbeatTime();
            if (heartbeat == null || heartbeat.toInstant().plus(taskTimeout, ChronoUnit.MINUTES).isAfter(now)) {
                skipped++;
                continue;
            }
            LocalDate scheduleDate = toLocalDate(task.getScheduleDate());
            RLock lock = lockService.getLock(task.getFactoryCode(), scheduleDate);
            if (lock.isLocked()) {
                skipped++;
                log.info("[直裁自动排程] 遗留任务锁仍存在，跳过补偿, taskId={}, factoryCode={}, scheduleDate={}",
                        task.getTaskId(), task.getFactoryCode(), scheduleDate);
                continue;
            }
            boolean updated = taskService.markTimeoutFailed(task.getTaskId(),
                    "自动排程任务心跳超过" + taskTimeout + "分钟且执行锁不存在");
            if (updated) {
                failed++;
                log.warn("[直裁自动排程] 遗留任务已补偿为失败, taskId={}, timeoutMinutes={}",
                        task.getTaskId(), taskTimeout);
            } else {
                skipped++;
            }
        }
        log.info("[直裁自动排程] 遗留任务补偿扫描完成, scannedCount={}, failedCount={}, skippedCount={}",
                tasks.size(), failed, skipped);
        return Cd90TaskRecoveryResult.builder().scannedCount(tasks.size())
                .failedCount(failed).skippedCount(skipped).build();
    }

    private int loadTimeoutMinutes(String factoryCode) {
        Long enabledShiftCount = shiftConfigMapper.selectCount(new LambdaQueryWrapper<Cd90ShiftConfig>()
                .eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                .eq(Cd90ShiftConfig::getIsActive, ACTIVE));
        Cd90AutoScheduleParameters parameters = parameterService.load(factoryCode, enabledShiftCount.intValue());
        return parameters.getTaskTimeoutMinutes();
    }

    /**
     * 兼容MyBatis可能返回的java.sql.Date，避免调用其toInstant时抛出异常。
     *
     * @param value 排程日期
     * @return 本地日期
     */
    private LocalDate toLocalDate(Date value) {
        if (value == null) {
            throw new IllegalStateException("遗留自动排程任务缺少排程日期");
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
