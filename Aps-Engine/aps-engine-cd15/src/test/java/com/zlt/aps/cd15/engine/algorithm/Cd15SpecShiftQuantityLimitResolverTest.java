package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15SpecShiftQuantityLimit;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 同钢带班内累计计划量限制测试。 */
public class Cd15SpecShiftQuantityLimitResolverTest {

    private final Cd15SpecShiftQuantityLimitResolver resolver =
            new Cd15SpecShiftQuantityLimitResolver();

    @Test
    public void normalShiftShouldAggregateSameSteelStripAcrossMachines() {
        Cd15ShiftResourceState state = Cd15ShiftResourceState.builder()
                .tasks(Arrays.asList(
                        task("211500012", "G1101", "700.25"),
                        task("211500012", "G1501", "799.75"),
                        task("211500015", "G1101", "900")))
                .build();
        Cd15SpecShiftQuantityLimit result = resolver.resolve(
                Cd15ShiftDescriptor.builder().restartStockMode(false).build(),
                state, parameters(), "211500012");

        assertFalse(result.isRestartStockMode());
        assertEquals(new BigDecimal("2000"), result.getShiftLimit());
        assertEquals(new BigDecimal("1500.00"), result.getScheduledQuantity());
        assertEquals(new BigDecimal("500.00"), result.getRemainingQuantity());
    }

    @Test
    public void actualRestartShiftShouldUseRestartStockThreshold() {
        Cd15ShiftResourceState state = Cd15ShiftResourceState.builder()
                .tasks(Arrays.asList(task("211500012", "G1101", "1500")))
                .build();
        Cd15SpecShiftQuantityLimit result = resolver.resolve(
                Cd15ShiftDescriptor.builder().restartStockMode(true).build(),
                state, parameters(), "211500012");

        assertTrue(result.isRestartStockMode());
        assertEquals(new BigDecimal("3000"), result.getShiftLimit());
        assertEquals(new BigDecimal("1500"), result.getRemainingQuantity());
    }

    private Cd15AutoScheduleParameters parameters() {
        return Cd15AutoScheduleParameters.builder()
                .equalShareThreshold(new BigDecimal("2000"))
                .restartStockThreshold(new BigDecimal("3000"))
                .build();
    }

    private Cd15ShiftScheduleTask task(
            String steelStripCode, String machineCode, String quantity) {
        return Cd15ShiftScheduleTask.builder()
                .steelStripCode(steelStripCode)
                .machineCode(machineCode)
                .planQuantity(new BigDecimal(quantity))
                .build();
    }
}
