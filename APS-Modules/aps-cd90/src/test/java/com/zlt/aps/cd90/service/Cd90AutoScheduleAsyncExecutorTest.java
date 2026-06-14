package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleProgressListener;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.service.impl.Cd90AutoScheduleAsyncExecutorImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 自动排程异步执行并发保护测试。 */
public class Cd90AutoScheduleAsyncExecutorTest {

    @Mock private Cd90AutoScheduleLockService lockService;
    @Mock private Cd90ScheduleTaskService taskService;
    @Mock private Cd90AutoScheduleEngineService engineService;
    @Mock private Cd90AutoSchedulePersistService persistService;
    @Mock private RLock lock;
    @InjectMocks private Cd90AutoScheduleAsyncExecutorImpl executor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldNotMarkExistingTaskFailedWhenLockIsHeldByAnotherExecutor() {
        Date scheduleDate = Date.from(LocalDate.of(2026, 6, 14)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(lockService.getLock(eq("116"), any(LocalDate.class))).thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);

        executor.execute("TASK-1", "116", scheduleDate);

        verify(taskService, never()).markFailed(eq("TASK-1"), any());
        verify(taskService, never()).start("TASK-1");
    }

    @Test
    public void shouldContinueWhenHeartbeatUpdateReturnsFalse() {
        Date scheduleDate = scheduleDate();
        Cd90ScheduleTask runningTask = new Cd90ScheduleTask();
        runningTask.setTaskStatus(Cd90ScheduleTaskStatus.RUNNING);
        Cd90AutoScheduleContext context = mock(Cd90AutoScheduleContext.class);
        Cd90AutoScheduleOutputDraft output = mock(Cd90AutoScheduleOutputDraft.class);
        when(lockService.getLock(eq("116"), any(LocalDate.class))).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(taskService.start("TASK-1")).thenReturn(true);
        when(taskService.findByTaskId("TASK-1")).thenReturn(runningTask);
        when(taskService.updateProgress(eq("TASK-1"), anyInt(), any(), any()))
                .thenReturn(false);
        when(engineService.prepare("116", scheduleDate)).thenReturn(context);
        when(engineService.execute(eq(context), any(Cd90ScheduleProgressListener.class)))
                .thenReturn(output);

        executor.execute("TASK-1", "116", scheduleDate);

        verify(persistService).persist("TASK-1", context, output, lock);
        verify(taskService, never()).markFailed(eq("TASK-1"), any());
    }

    @Test
    public void shouldStopBeforeEngineWhenTaskIsNoLongerRunning() {
        Date scheduleDate = scheduleDate();
        Cd90ScheduleTask failedTask = new Cd90ScheduleTask();
        failedTask.setTaskStatus(Cd90ScheduleTaskStatus.FAILED);
        Cd90AutoScheduleContext context = mock(Cd90AutoScheduleContext.class);
        when(lockService.getLock(eq("116"), any(LocalDate.class))).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(taskService.start("TASK-1")).thenReturn(true);
        when(engineService.prepare("116", scheduleDate)).thenReturn(context);
        when(taskService.findByTaskId("TASK-1")).thenReturn(failedTask);

        executor.execute("TASK-1", "116", scheduleDate);

        verify(engineService, never()).execute(any(Cd90AutoScheduleContext.class),
                any(Cd90ScheduleProgressListener.class));
        verify(taskService).markFailed(eq("TASK-1"), any());
    }

    private Date scheduleDate() {
        return Date.from(LocalDate.of(2026, 6, 14)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
