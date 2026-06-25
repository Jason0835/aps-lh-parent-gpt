package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleParameterService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ShiftConfigMapper;
import com.zlt.aps.cd90.model.Cd90TaskRecoveryResult;
import com.zlt.aps.cd90.service.impl.Cd90ScheduleTaskRecoveryServiceImpl;
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

/** 遗留自动排程任务补偿测试。 */
public class Cd90ScheduleTaskRecoveryServiceTest {

    @Test
    public void shouldMarkExpiredTaskFailedWhenExecutionLockIsMissing() {
        Fixture fixture = fixture(false);

        Cd90TaskRecoveryResult result = fixture.service.recover(30);

        assertEquals(1, result.getScannedCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(0, result.getSkippedCount());
        verify(fixture.taskService).markTimeoutFailed(eq("TASK-1"), any(String.class));
    }

    @Test
    public void shouldSkipExpiredTaskWhenExecutionLockStillExists() {
        Fixture fixture = fixture(true);

        Cd90TaskRecoveryResult result = fixture.service.recover(30);

        assertEquals(1, result.getScannedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getSkippedCount());
        verify(fixture.taskService, never()).markTimeoutFailed(any(String.class), any(String.class));
    }

    /** 构造只使用显式超时时间的测试环境，避免测试依赖参数表内容。 */
    private Fixture fixture(boolean locked) {
        Cd90ScheduleTaskService taskService = mock(Cd90ScheduleTaskService.class);
        Cd90AutoScheduleLockService lockService = mock(Cd90AutoScheduleLockService.class);
        Cd90AutoScheduleParameterService parameterService = mock(Cd90AutoScheduleParameterService.class);
        Cd90ShiftConfigMapper shiftConfigMapper = mock(Cd90ShiftConfigMapper.class);
        RLock lock = mock(RLock.class);

        Cd90ScheduleTask task = new Cd90ScheduleTask();
        task.setTaskId("TASK-1");
        task.setFactoryCode("116");
        task.setScheduleDate(Date.valueOf(LocalDate.of(2026, 6, 14)));
        task.setStartTime(java.util.Date.from(Instant.now().minus(40, ChronoUnit.MINUTES)));
        task.setLastHeartbeatTime(java.util.Date.from(Instant.now().minus(31, ChronoUnit.MINUTES)));

        when(taskService.findRunningTasks(500)).thenReturn(Collections.singletonList(task));
        when(lockService.getLock("116", LocalDate.of(2026, 6, 14))).thenReturn(lock);
        when(lock.isLocked()).thenReturn(locked);
        when(taskService.markTimeoutFailed(eq("TASK-1"), any(String.class))).thenReturn(true);

        Cd90ScheduleTaskRecoveryServiceImpl service = new Cd90ScheduleTaskRecoveryServiceImpl(
                taskService, lockService, parameterService, shiftConfigMapper);
        return new Fixture(service, taskService);
    }

    /** 测试依赖集合。 */
    private static class Fixture {
        private final Cd90ScheduleTaskRecoveryServiceImpl service;
        private final Cd90ScheduleTaskService taskService;

        private Fixture(Cd90ScheduleTaskRecoveryServiceImpl service,
                        Cd90ScheduleTaskService taskService) {
            this.service = service;
            this.taskService = taskService;
        }
    }
}
