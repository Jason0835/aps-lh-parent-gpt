package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90MachineCandidate;
import com.zlt.aps.cd90.engine.model.Cd90MachineResource;
import com.zlt.aps.cd90.engine.model.Cd90MachineRestriction;
import com.zlt.aps.cd90.engine.model.Cd90MachineRollBinding;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 候选机台硬约束和定点优先规则测试。
 */
public class Cd90MachineCandidateResolverTest {

    private final Cd90MachineCandidateResolver resolver = new Cd90MachineCandidateResolver();

    @Test
    public void shouldApplyBindingStatusAndOpenShiftConstraints() {
        List<Cd90MachineCandidate> result = resolver.resolve("CF001", "BR001", "NIGHT",
                shiftStart(), shiftStart().plusHours(8), Arrays.asList(
                        machine("M1", "0", "NIGHT"), machine("M2", "1", "NIGHT"),
                        machine("M3", "0", "MORNING")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2"), binding("BR001", "M3")),
                Collections.emptyList(), Arrays.asList("M3", "M1"));

        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).getMachineCode());
        assertEquals(1, result.get(0).getPriorityOrder());
    }

    @Test
    public void shouldExcludeProhibitedAndOnlyPreferSpecifiedMachine() {
        List<Cd90MachineCandidate> result = resolver.resolve("CF001", "BR001", "NIGHT",
                shiftStart(), shiftStart().plusHours(8), Arrays.asList(
                        machine("M1", "0", "NIGHT"), machine("M2", "0", "NIGHT"),
                        machine("M3", "0", "NIGHT")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2"), binding("BR001", "M3")),
                Arrays.asList(restriction("M1", "1"), restriction("M2", "0")),
                Collections.emptyList());

        assertEquals(2, result.size());
        assertEquals("M2", result.get(0).getMachineCode());
        assertTrue(result.get(0).isPreferredMachine());
        assertEquals("M3", result.get(1).getMachineCode());
        assertFalse(result.get(1).isPreferredMachine());
    }

    @Test
    public void shouldExcludeMaintenanceOverlap() {
        Cd90MachineResource machine = machine("M1", "0", "NIGHT");
        machine.setMaintenanceStart(shiftStart().plusHours(2));
        machine.setMaintenanceEnd(shiftStart().plusHours(4));

        List<Cd90MachineCandidate> result = resolver.resolve("CF001", "BR001", "NIGHT",
                shiftStart(), shiftStart().plusHours(8), Collections.singletonList(machine),
                Collections.singletonList(binding("BR001", "M1")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(0, result.size());
    }

    private LocalDateTime shiftStart() {
        return LocalDateTime.of(2026, 6, 12, 22, 0);
    }

    private Cd90MachineResource machine(String code, String status, String openShift) {
        return Cd90MachineResource.builder().machineCode(code).status(status)
                .openMachineClass(openShift).quota(new BigDecimal("1000")).build();
    }

    private Cd90MachineRollBinding binding(String bigRollCode, String machineCode) {
        return Cd90MachineRollBinding.builder().bigRollCode(bigRollCode).machineCode(machineCode).build();
    }

    private Cd90MachineRestriction restriction(String machineCode, String jobType) {
        return Cd90MachineRestriction.builder().clothCode("CF001")
                .machineCode(machineCode).jobType(jobType).build();
    }
}
