package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.constant.Cd15CutMode;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 斜裁机台模式与班产能力解析规则测试。
 */
public class Cd15MachineModeResolverTest {

    private final Cd15MachineModeResolver resolver = new Cd15MachineModeResolver();

    @Test
    public void shouldAcceptSingleAndSplitTasksForDailyOutputModeWithoutThreshold() {
        Cd15MachineResource machine = machine(Cd15CutMode.DAILY_OUTPUT);
        machine.setSingleCutSupported(true);
        machine.setSplitCutSupported(true);

        assertTrue(resolver.matches(machine, false));
        assertTrue(resolver.matches(machine, true));
    }

    @Test
    public void shouldRespectCapabilityFlagsForDailyOutputMode() {
        Cd15MachineResource machine = machine(Cd15CutMode.DAILY_OUTPUT);
        machine.setSingleCutSupported(true);
        machine.setSplitCutSupported(false);

        assertTrue(resolver.matches(machine, false));
        assertFalse(resolver.matches(machine, true));
    }

    @Test
    public void shouldMatchConfiguredSplitMachineWithoutHardcodedMachineCode() {
        Cd15MachineResource machine = machine("G1401", Cd15CutMode.SPLIT);
        machine.setSplitCutSupported(true);

        assertTrue(resolver.matches(machine, true));
        assertFalse(resolver.matches(machine, false));
    }

    @Test
    public void shouldUseTaskModeCapacityForDailyOutputMode() {
        Cd15MachineResource machine = machine(Cd15CutMode.DAILY_OUTPUT);
        machine.setSingleCutSupported(true);
        machine.setSplitCutSupported(true);
        machine.setSingleShiftCapacity(new BigDecimal("1200"));
        machine.setSplitShiftCapacity(new BigDecimal("900"));

        assertEquals(0, new BigDecimal("1200").compareTo(
                resolver.capacity(machine, false)));
        assertEquals(0, new BigDecimal("900").compareTo(
                resolver.capacity(machine, true)));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectInvalidConfiguredMode() {
        resolver.matches(machine("UNKNOWN"), false);
    }

    private Cd15MachineResource machine(String mode) {
        return this.machine("G1101", mode);
    }

    private Cd15MachineResource machine(String machineCode, String mode) {
        return Cd15MachineResource.builder()
                .machineCode(machineCode)
                .defaultCutMode(mode)
                .singleShiftCapacity(BigDecimal.ZERO)
                .splitShiftCapacity(BigDecimal.ZERO)
                .build();
    }
}