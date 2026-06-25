package com.zlt.aps.cd90.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleDemandSnapshotMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleLaneAllocationMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleParamSnapshotMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultLogMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.service.impl.Cd90AutoSchedulePersistServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;

import static org.mockito.Mockito.when;

/** 自动排程最终事务前置状态测试。 */
public class Cd90AutoSchedulePersistServiceTest {

    @Mock private Cd90ScheduleResultMapper resultMapper;
    @Mock private Cd90ScheduleLaneAllocationMapper laneMapper;
    @Mock private Cd90ScheduleResultLogMapper logMapper;
    @Mock private Cd90UnscheduleResultMapper unscheduleMapper;
    @Mock private Cd90ScheduleParamSnapshotMapper paramSnapshotMapper;
    @Mock private Cd90ScheduleDemandSnapshotMapper demandSnapshotMapper;
    @Mock private Cd90ScheduleTaskService taskService;
    @Mock private Cd90AutoScheduleVersionVerifier versionVerifier;
    @Mock private Cd90ScheduleOverwriteValidator overwriteValidator;
    @Mock private Cd90ScheduleNumberService numberService;
    @Mock private Cd90AutoScheduleDraftMapper draftMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private RLock lock;
    @InjectMocks private Cd90AutoSchedulePersistServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectCommitWhenLockIsNotHeldByCurrentThread() {
        Cd90ScheduleTask task = new Cd90ScheduleTask();
        task.setTaskStatus(Cd90ScheduleTaskStatus.RUNNING);
        when(taskService.findByTaskId("TASK-1")).thenReturn(task);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        service.persist("TASK-1", Cd90AutoScheduleContext.builder().build(),
                Cd90AutoScheduleOutputDraft.builder().build(), lock);
    }
}
