package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneConsumptionResult;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * 斜裁自动排程关键边界补充测试。
 */
public class Cd15AutoScheduleBoundaryCoverageTest {

    private final Cd15StorageLaneConsumptionCalculator laneConsumptionCalculator =
            new Cd15StorageLaneConsumptionCalculator();
    private final Cd15MachineTrialSelector machineTrialSelector =
            new Cd15MachineTrialSelector();

    /**
     * T-07：验证卷曲长度87米时86、87、173、174米四个整车释放边界。
     */
    @Test
    public void storageLaneReleaseShouldRespectWholeVehicleBoundaries() {
        assertRelease("86", 0, "86", 2);
        assertRelease("87", 1, "0", 1);
        assertRelease("173", 1, "86", 1);
        assertRelease("174", 2, "0", 0);
    }

    /**
     * T-09：定点机台无可排量时必须回退到仍有可排量的普通机台。
     */
    @Test
    public void preferredMachineWithoutCapacityShouldFallBackToAvailableMachine() {
        Cd15MachineTrial selected = machineTrialSelector.select(Arrays.asList(
                trial("M1", true, "0", 0, "CAPACITY_LIMIT"),
                trial("M2", false, "80", 1, null)
        ));

        assertEquals("M2", selected.getMachineCode());
        assertEquals(new BigDecimal("80"), selected.getFinalSchedulableQuantity());
    }

    private void assertRelease(String consumption, int releasedVehicles,
                               String remainder, int remainingVehicles) {
        Cd15StorageLaneConsumptionResult result = laneConsumptionCalculator.consume(
                Collections.singletonMap("C1", new BigDecimal(consumption)),
                Collections.emptyMap(), new BigDecimal("87"),
                Collections.singletonList(Cd15StorageLaneState.builder()
                        .laneCode("L1").steelStripCode("C1")
                        .vehicleCount(2).build()));

        assertEquals(releasedVehicles, result.getReleasedVehicleCount());
        assertEquals(new BigDecimal(remainder), result.getRemainderQuantity());
        assertEquals(remainingVehicles, result.getLanes().get(0).getVehicleCount());
    }

    private Cd15MachineTrial trial(String machineCode, boolean preferred,
                                   String quantity, int priority, String reason) {
        return Cd15MachineTrial.builder()
                .machineCode(machineCode)
                .preferredMachine(preferred)
                .finalSchedulableQuantity(new BigDecimal(quantity))
                .priorityOrder(priority)
                .remainingSeconds(0)
                .limitReason(reason)
                .build();
    }
}
