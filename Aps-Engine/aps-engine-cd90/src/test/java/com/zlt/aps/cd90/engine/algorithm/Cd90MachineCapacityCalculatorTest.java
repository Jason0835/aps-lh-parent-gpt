package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90MachineCapacityTrial;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 直裁机台班产能增量试算测试。
 */
public class Cd90MachineCapacityCalculatorTest {

    private final Cd90MachineCapacityCalculator calculator = new Cd90MachineCapacityCalculator();

    /**
     * T-10：满班定额推导速度后，扣减检修和一次规格切换耗时。
     */
    @Test
    public void shouldDeductMaintenanceAndSpecChangeTime() {
        Cd90MachineCapacityTrial result = calculator.calculateInitial(
                new BigDecimal("2400"), 8, 3600,
                tail("R1", "A"), tail("R1", "B"),
                5, 7, 9, new BigDecimal("3000"));

        assertEquals(new BigDecimal("2075"), result.getCapacityQuantity());
        assertEquals(300, result.getChangeSeconds());
        assertEquals(0, result.getRemainingSeconds());
    }

    /**
     * 相同规格连续生产时不扣规格切换耗时。
     */
    @Test
    public void sameSpecShouldNotDeductChangeTime() {
        Cd90MachineCapacityTrial result = calculator.calculateWithRemainingSeconds(
                new BigDecimal("2400"), 8, 3600,
                tail("R1", "A"), tail("R1", "A"),
                5, 7, 9, new BigDecimal("300"));

        assertEquals(new BigDecimal("300"), result.getCapacityQuantity());
        assertEquals(0, result.getChangeSeconds());
        assertEquals(0, result.getRemainingSeconds());
    }

    /**
     * 不同大卷但相同规格使用异卷同规格参数。
     */
    @Test
    public void differentRollSameSpecShouldUseConfiguredMinutes() {
        Cd90MachineCapacityTrial result = calculator.calculateWithRemainingSeconds(
                new BigDecimal("2400"), 8, 3600,
                tail("R1", "A"), tail("R2", "A"),
                5, 7, 9, new BigDecimal("100"));

        assertEquals(420, result.getChangeSeconds());
    }

    private com.zlt.aps.cd90.engine.model.Cd90MachineTailState tail(String roll, String spec) {
        return com.zlt.aps.cd90.engine.model.Cd90MachineTailState.builder()
                .bigRollCode(roll).clothCode(spec).build();
    }
}
