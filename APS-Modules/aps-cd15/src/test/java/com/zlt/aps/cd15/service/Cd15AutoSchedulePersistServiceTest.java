package com.zlt.aps.cd15.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleDemandSnapshot;
import com.zlt.aps.cd15.engine.algorithm.Cd15AutoScheduleRuntimeGuard;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleOutputDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleDemandSnapshotMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleParamSnapshotMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15UnscheduleResultMapper;
import com.zlt.aps.cd15.service.impl.Cd15AutoSchedulePersistServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 斜裁自动排程最终落库测试。 */
public class Cd15AutoSchedulePersistServiceTest {

    @Test
    public void shouldPersistBigRollAndCuttingAngleFromAttemptTrace() throws Exception {
        Cd15ScheduleDemandSnapshotMapper demandSnapshotMapper =
                mock(Cd15ScheduleDemandSnapshotMapper.class);
        when(demandSnapshotMapper.insert(any(Cd15ScheduleDemandSnapshot.class)))
                .thenReturn(1);
        Cd15AutoSchedulePersistServiceImpl service = this.service(demandSnapshotMapper);
        Cd15AutoScheduleContext context = Cd15AutoScheduleContext.builder()
                .factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 7, 18))
                .shifts(Collections.emptyList())
                .inputVersionFingerprint("VERSION-1")
                .build();
        Cd15ScheduleAttemptTrace trace = Cd15ScheduleAttemptTrace.builder()
                .classField("CLASS1")
                .steelStripCode("BELT-1")
                .bigRollCode("CSS24524")
                .cuttingAngle("24")
                .netDemandQuantity(new BigDecimal("120.5"))
                .build();
        Cd15AutoScheduleOutputDraft output = Cd15AutoScheduleOutputDraft.builder()
                .demandTraces(Collections.singletonList(trace))
                .build();

        Method method = Cd15AutoSchedulePersistServiceImpl.class.getDeclaredMethod(
                "saveDemandSnapshots", Cd15AutoScheduleContext.class,
                Cd15AutoScheduleOutputDraft.class, String.class);
        method.setAccessible(true);
        method.invoke(service, context, output, "CD15-BATCH-1");

        ArgumentCaptor<Cd15ScheduleDemandSnapshot> captor =
                ArgumentCaptor.forClass(Cd15ScheduleDemandSnapshot.class);
        verify(demandSnapshotMapper).insert(captor.capture());
        Cd15ScheduleDemandSnapshot snapshot = captor.getValue();
        assertEquals("BELT-1", snapshot.getSteelStripCode());
        assertEquals("CSS24524", snapshot.getBigRollCode());
        assertEquals("24", snapshot.getCuttingAngle());
        assertEquals(0, new BigDecimal("120.5").compareTo(snapshot.getDemandQty()));
    }

    /** 构造仅用于需求快照持久化的服务实例。 */
    private Cd15AutoSchedulePersistServiceImpl service(
            Cd15ScheduleDemandSnapshotMapper demandSnapshotMapper) {
        return new Cd15AutoSchedulePersistServiceImpl(
                mock(Cd15ScheduleResultMapper.class),
                mock(Cd15ScheduleLaneAllocationMapper.class),
                mock(Cd15ScheduleResultLogMapper.class),
                mock(Cd15UnscheduleResultMapper.class),
                mock(Cd15ScheduleParamSnapshotMapper.class),
                demandSnapshotMapper,
                mock(Cd15ScheduleTaskService.class),
                mock(Cd15AutoScheduleVersionVerifier.class),
                mock(Cd15ScheduleOverwriteValidator.class),
                mock(Cd15ScheduleNumberService.class),
                mock(Cd15AutoScheduleDraftMapper.class),
                new ObjectMapper(),
                mock(Cd15AutoScheduleRuntimeGuard.class));
    }
}
