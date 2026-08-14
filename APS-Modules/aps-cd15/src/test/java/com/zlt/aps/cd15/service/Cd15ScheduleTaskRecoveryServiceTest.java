package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleParameterService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ShiftConfigMapper;
import com.zlt.aps.cd15.model.Cd15TaskRecoveryResult;
import com.zlt.aps.cd15.service.impl.Cd15ScheduleTaskRecoveryServiceImpl;
import org.junit.Test;
import org.redisson.api.RLock;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 斜裁遗留自动排程任务补偿测试。 */
public class Cd15ScheduleTaskRecoveryServiceTest {

    @Test
    public void shouldMarkExpiredTaskFailedWhenExecutionLockIsMissing() {
        Fixture fixture = this.fixture(false);

        Cd15TaskRecoveryResult result = fixture.service.recover(30);

        assertEquals(1, result.getScannedCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(0, result.getSkippedCount());
        verify(fixture.taskService).markTimeoutFailed(
                eq("TASK-1"), any(String.class));
    }

    @Test
    public void shouldSkipExpiredTaskWhenExecutionLockStillExists() {
        Fixture fixture = this.fixture(true);

        Cd15TaskRecoveryResult result = fixture.service.recover(30);

        assertEquals(1, result.getScannedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getSkippedCount());
        verify(fixture.taskService, never()).markTimeoutFailed(
                any(String.class), any(String.class));
    }

    @Test
    public void shouldRecoverExpiredPendingTaskBasedOnCreateTime() {
        Fixture fixture = this.fixture(false, Cd15ScheduleTaskStatus.PENDING);

        Cd15TaskRecoveryResult result = fixture.service.recover(30);

        assertEquals(1, result.getScannedCount());
        assertEquals(1, result.getFailedCount());
        verify(fixture.taskService).markTimeoutFailed(
                eq("TASK-1"), any(String.class));
    }

    /** 使用显式超时时间构造测试环境，避免依赖参数表。 */
    private Fixture fixture(boolean locked) {
        return this.fixture(locked, Cd15ScheduleTaskStatus.RUNNING);
    }

    /** 按任务状态构造活动时间，覆盖等待派发和执行中两类遗留任务。 */
    private Fixture fixture(boolean locked, String taskStatus) {
        Cd15ScheduleTaskService taskService =
                mock(Cd15ScheduleTaskService.class);
        Cd15AutoScheduleLockService lockService =
                mock(Cd15AutoScheduleLockService.class);
        Cd15AutoScheduleParameterService parameterService =
                mock(Cd15AutoScheduleParameterService.class);
        Cd15ShiftConfigMapper shiftConfigMapper =
                mock(Cd15ShiftConfigMapper.class);
        RLock lock = mock(RLock.class);

        Cd15ScheduleTask task = new Cd15ScheduleTask();
        task.setTaskId("TASK-1");
        task.setFactoryCode("116");
        task.setScheduleDate(Date.valueOf(LocalDate.of(2026, 7, 18)));
        task.setCreateTime(java.util.Date.from(
                Instant.now().minus(40, ChronoUnit.MINUTES)));
        if (Cd15ScheduleTaskStatus.RUNNING.equals(taskStatus)) {
            task.setStartTime(java.util.Date.from(
                    Instant.now().minus(40, ChronoUnit.MINUTES)));
            task.setLastHeartbeatTime(java.util.Date.from(
                    Instant.now().minus(31, ChronoUnit.MINUTES)));
        }
        task.setTaskStatus(taskStatus);
        when(taskService.findRecoverableTasks(500))
                .thenReturn(Collections.singletonList(task));
        when(lockService.getLock("116", LocalDate.of(2026, 7, 18)))
                .thenReturn(lock);
        when(lock.tryLock()).thenReturn(!locked);
        when(lock.isHeldByCurrentThread()).thenReturn(!locked);
        when(taskService.markTimeoutFailed(
                eq("TASK-1"), any(String.class))).thenReturn(true);

        Cd15ScheduleTaskRecoveryServiceImpl service =
                new Cd15ScheduleTaskRecoveryServiceImpl(
                        taskService, lockService,
                        parameterService, shiftConfigMapper);
        return new Fixture(service, taskService);
    }

    /** 测试依赖集合。 */
    private static class Fixture {
        private final Cd15ScheduleTaskRecoveryServiceImpl service;
        private final Cd15ScheduleTaskService taskService;

        private Fixture(Cd15ScheduleTaskRecoveryServiceImpl service,
                        Cd15ScheduleTaskService taskService) {
            this.service = service;
            this.taskService = taskService;
        }
    }
}
