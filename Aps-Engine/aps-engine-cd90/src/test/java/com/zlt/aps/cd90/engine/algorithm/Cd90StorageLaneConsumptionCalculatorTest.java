package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneConsumptionResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

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
                new BigDecimal("173"), new BigDecimal("87"), Arrays.asList(
                        lane("L1", 1), lane("L2", 2)
                ));

        assertEquals(1, result.getReleasedVehicleCount());
        assertEquals(new BigDecimal("86"), result.getRemainderQuantity());
        assertEquals(0, result.getLanes().get(0).getVehicleCount());
        assertEquals(2, result.getLanes().get(1).getVehicleCount());
    }

    private Cd90StorageLaneState lane(String code, int vehicleCount) {
        return Cd90StorageLaneState.builder()
                .laneCode(code)
                .clothCode("C1")
                .vehicleCount(vehicleCount)
                .build();
    }
}
