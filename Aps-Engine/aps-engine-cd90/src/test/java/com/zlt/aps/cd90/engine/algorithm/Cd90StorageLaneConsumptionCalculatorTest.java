package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneConsumptionResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * 库排整车释放计算测试。
 */
public class Cd90StorageLaneConsumptionCalculatorTest {

    private final Cd90StorageLaneConsumptionCalculator calculator =
            new Cd90StorageLaneConsumptionCalculator();

    /**
     * T-07：173米消耗只释放一车并保留86米余量。
     */
    @Test
    public void consumptionShouldOnlyReleaseWholeVehicles() {
        Cd90StorageLaneConsumptionResult result = calculator.consume(
                Collections.singletonMap("C1", new BigDecimal("173")),
                Collections.emptyMap(), new BigDecimal("87"), Arrays.asList(
                        lane("L1", 1), lane("L2", 2)
                ));

        assertEquals(1, result.getReleasedVehicleCount());
        assertEquals(new BigDecimal("86"), result.getRemainderQuantity());
        assertEquals(0, result.getLanes().get(0).getVehicleCount());
        assertEquals(2, result.getLanes().get(1).getVehicleCount());
    }

    /**
     * C01的成型消耗只能释放C01占用库排，不能把C02的L02释放成空库排后再分配给C01。
     */
    @Test
    public void consumptionShouldOnlyReleaseSameClothLanes() {
        Cd90StorageLaneConsumptionResult result = calculator.consume(
                Collections.singletonMap("C1", new BigDecimal("100")),
                Collections.emptyMap(), new BigDecimal("100"), Arrays.asList(
                        lane("L1", "C1", 1), lane("L2", "C2", 1)
                ));

        assertEquals(1, result.getReleasedVehicleCount());
        assertEquals(0, result.getLanes().get(0).getVehicleCount());
        assertEquals(1, result.getLanes().get(1).getVehicleCount());
        assertEquals("C2", result.getLanes().get(1).getClothCode());
    }

    /**
     * ???????????????160??C1=80?????????100??????
     */
    @Test
    public void consumptionShouldPreferClothCurlLengthOverFallback() {
        Cd90StorageLaneConsumptionResult result = calculator.consume(
                Collections.singletonMap("C1", new BigDecimal("160")),
                Collections.singletonMap("C1", new BigDecimal("80")),
                new BigDecimal("100"), Arrays.asList(lane("L1", 1), lane("L2", 1)));

        assertEquals(2, result.getReleasedVehicleCount());
        assertEquals(new BigDecimal("0"), result.getRemainderQuantity());
        assertEquals(0, result.getLanes().get(0).getVehicleCount());
        assertEquals(0, result.getLanes().get(1).getVehicleCount());
    }

    private Cd90StorageLaneState lane(String code, int vehicleCount) {
        return lane(code, "C1", vehicleCount);
    }

    private Cd90StorageLaneState lane(String code, String clothCode, int vehicleCount) {
        return Cd90StorageLaneState.builder()
                .laneCode(code)
                .clothCode(clothCode)
                .vehicleCount(vehicleCount)
                .build();
    }
}