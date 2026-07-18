package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.service.Cd15MachineResourceService;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Cd15ExistingScheduleResourceReserverTest {

    @Test
    public void shouldReserveMachineLaneToolingAndTaskIdentityTogether() {
        Cd15MachineResourceService machineService = mock(Cd15MachineResourceService.class);
        Cd15MachineResource machine = Cd15MachineResource.builder().machineCode("M1")
                .quota(new BigDecimal("800")).build();
        when(machineService.load("F1", start(), start().plusHours(8)))
                .thenReturn(Cd15MachineResourceSnapshot.builder()
                        .machines(Collections.singletonList(machine)).build());
        Cd15ExistingScheduleResourceReserver reserver =
                new Cd15ExistingScheduleResourceReserver(
                        machineService, new Cd15MachineCapacityCalculator());

        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setId(1L);
        result.setMachineCode("M1");
        result.setSteelStripCode("C1");
        result.setBigRollCode("B1");
        result.setClass1PlanQty(100D);
        result.setClass1ProduceOrder(1);
        Cd15ScheduleLaneAllocation lane = new Cd15ScheduleLaneAllocation();
        lane.setScheduleResultId(1L);
        lane.setClassField("CLASS1");
        lane.setStorageLaneCode("L1");
        lane.setAllocatedCartCount(2);
        lane.setAllocationOrder(1);
        Cd15ShiftResourceState state = Cd15ShiftResourceState.builder()
                .lanes(new ArrayList<>(Collections.singletonList(
                        Cd15StorageLaneState.builder().laneCode("L1")
                                .vehicleCount(0).maxVehicleCount(10).build())))
                .totalToolingCount(10).occupiedToolingCount(0)
                .remainingSecondsByMachine(new HashMap<>())
                .tailSpecByMachine(new HashMap<>()).tailByMachine(new HashMap<>())
                .tasks(new ArrayList<>()).build();
        state.getRemainingSecondsByMachine().put("M1", 28800);

        reserver.reserve(context(), shift(), state,
                Collections.singletonList(result),
                Collections.singletonMap(1L, Collections.singletonList(lane)));

        assertTrue(state.getRemainingSecondsByMachine().get("M1") < 28800);
        assertEquals(2, state.getLanes().get(0).getVehicleCount());
        assertEquals(2, state.getOccupiedToolingCount());
        assertEquals(Long.valueOf(1L), state.getTasks().get(0).getSourceResultId());
    }

    private Cd15AutoScheduleContext context() {
        return Cd15AutoScheduleContext.builder().factoryCode("F1")
                .parameters(Cd15AutoScheduleParameters.builder()
                        .sameRollDiffSpecChangeMinutes(5)
                        .diffRollSameSpecChangeMinutes(10)
                        .diffRollDiffSpecChangeMinutes(15).build()).build();
    }

    private Cd15ShiftDescriptor shift() {
        return Cd15ShiftDescriptor.builder().classField("CLASS1").shiftCode("01")
                .startTime(start()).endTime(start().plusHours(8))
                .durationSeconds(28800).build();
    }

    private LocalDateTime start() {
        return LocalDateTime.of(2026, 7, 3, 6, 0);
    }
}
