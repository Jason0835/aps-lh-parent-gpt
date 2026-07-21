package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15MachineCandidate;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineRestriction;
import com.zlt.aps.cd15.engine.model.Cd15MachineRollBinding;
import com.zlt.aps.cd15.engine.model.Cd15MachineCandidateResolution;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 候选机台硬约束和定点优先规则测试。
 */
public class Cd15MachineCandidateResolverTest {

    private final Cd15MachineCandidateResolver resolver = new Cd15MachineCandidateResolver();

    @Test
    public void shouldApplyBindingStatusAndOpenShiftConstraints() {
        List<Cd15MachineCandidate> result = resolver.resolve("CF001", "BR001", "NIGHT",
                shiftStart(), shiftStart().plusHours(8), Arrays.asList(
                        machine("M1", "1", "NIGHT"), machine("M2", "0", "NIGHT"),
                        machine("M3", "1", "MORNING")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2"), binding("BR001", "M3")),
                Collections.emptyList(), Arrays.asList("M3", "M1"));

        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).getMachineCode());
        assertEquals(1, result.get(0).getPriorityOrder());
    }

    @Test
    public void shouldExcludeProhibitedAndOnlyPreferSpecifiedMachine() {
        List<Cd15MachineCandidate> result = resolver.resolve("CF001", "BR001", "NIGHT",
                shiftStart(), shiftStart().plusHours(8), Arrays.asList(
                        machine("M1", "1", "NIGHT"), machine("M2", "1", "NIGHT"),
                        machine("M3", "1", "NIGHT")),
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
    public void shouldKeepMaintenanceMachineForCapacityDeduction() {
        Cd15MachineResource machine = machine("M1", "1", "NIGHT");
        machine.setMaintenanceStart(shiftStart().plusHours(2));
        machine.setMaintenanceEnd(shiftStart().plusHours(4));

        List<Cd15MachineCandidate> result = resolver.resolve("CF001", "BR001", "NIGHT",
                shiftStart(), shiftStart().plusHours(8), Collections.singletonList(machine),
                Collections.singletonList(binding("BR001", "M1")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).getMachineCode());
    }

    @Test
    public void shouldMatchOpenMachineClassByCommaSeparatedShiftCodes() {
        List<Cd15MachineCandidate> result = resolver.resolve("CF001", "BR001", "02",
                shiftStart(), shiftStart().plusHours(8), Arrays.asList(
                        machine("M1", "1", "01,02,03"),
                        machine("M2", "1", "010,03"),
                        machine("M3", "1", "03")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2"), binding("BR001", "M3")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).getMachineCode());
    }

    @Test
    public void shouldOnlyKeepMachinesMatchedByCraftWidth() {
        List<Cd15MachineCandidate> result = resolver.resolve("CF001", "BR001",
                new BigDecimal("50"), "NIGHT", shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(
                        machine("M1", "1", "NIGHT", "40", "60"),
                        machine("M2", "1", "NIGHT", "50.1", "70"),
                        machine("M3", "1", "NIGHT", "30", "49.9")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2"), binding("BR001", "M3")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).getMachineCode());
    }

    @Test
    public void shouldRejectCraftWidthAboveCurrentAngleMaximum() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", new BigDecimal("321"), "24",
                Collections.singletonMap("24", new BigDecimal("320")),
                "NIGHT", shiftStart(), shiftStart().plusHours(8),
                Collections.singletonList(machine("M1", "1", "NIGHT", "250", "400")),
                Collections.singletonList(binding("BR001", "M1")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(0, result.getCandidates().size());
        assertEquals("ANGLE_WIDTH_MISMATCH", result.getFailureReason());
    }

    @Test
    public void shouldReturnWidthMismatchWhenAllBoundMachinesWidthExceeded() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", new BigDecimal("894"), "NIGHT",
                shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(
                        machine("M1", "1", "NIGHT", "250", "320"),
                        machine("M2", "1", "NIGHT", "250", "320")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2")),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(0, result.getCandidates().size());
        assertEquals("WIDTH_MISMATCH", result.getFailureReason());
    }

    @Test
    public void shouldReturnWidthMismatchWhenNoBindingAndAllMachinesWidthExceeded() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", new BigDecimal("894"), "NIGHT",
                shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(
                        machine("M1", "1", "NIGHT", "250", "320"),
                        machine("M2", "1", "NIGHT", "250", "320")),
                Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(0, result.getCandidates().size());
        assertEquals("WIDTH_MISMATCH", result.getFailureReason());
    }

    @Test
    public void shouldReturnNoAvailableMachineWhenMultipleConstraintsFail() {
        // 宽度不匹配 + 开机班次不匹配同时存在时，不归因于宽度，按 NO_AVAILABLE_MACHINE 兜底。
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", new BigDecimal("894"), "NIGHT",
                shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(
                        machine("M1", "1", "MORNING", "250", "320"),
                        machine("M2", "0", "NIGHT", "250", "320")),
                Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(0, result.getCandidates().size());
        assertEquals("NO_AVAILABLE_MACHINE", result.getFailureReason());
    }

    @Test
    public void allBoundMachinesProhibitedShouldReturnStableReason() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", "NIGHT", shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(machine("M1", "1", "NIGHT"), machine("M2", "1", "NIGHT")),
                Arrays.asList(binding("BR001", "M1"), binding("BR001", "M2")),
                Arrays.asList(restriction("M1", "1"), restriction("M2", "1")),
                Collections.emptyList());

        assertEquals(0, result.getCandidates().size());
        assertEquals("MACHINE_PROHIBITED", result.getFailureReason());
        assertEquals(Arrays.asList("M1", "M2"), result.getBoundMachineCodes());
    }

    @Test
    public void shouldFallBackToAllEnabledMachinesWhenNoBinding() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", "NIGHT", shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(machine("M1", "1", "NIGHT"), machine("M2", "1", "NIGHT"),
                        machine("M3", "1", "NIGHT")),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("M3", "M1"));

        assertEquals(3, result.getCandidates().size());
        assertEquals(Arrays.asList("M3", "M1", "M2"),
                result.getCandidates().stream().map(Cd15MachineCandidate::getMachineCode)
                        .collect(Collectors.toList()));
        assertNull(result.getFailureReason());
        assertTrue(result.getBoundMachineCodes().isEmpty());
    }

    @Test
    public void shouldFallBackToAllEnabledWhenBindingEmptyAndSomeProhibited() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", "NIGHT", shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(machine("M1", "1", "NIGHT"), machine("M2", "1", "NIGHT"),
                        machine("M3", "1", "NIGHT")),
                Collections.emptyList(),
                Arrays.asList(restriction("M1", "1")),
                Collections.emptyList());

        assertEquals(2, result.getCandidates().size());
        assertEquals(Arrays.asList("M2", "M3"),
                result.getCandidates().stream().map(Cd15MachineCandidate::getMachineCode)
                        .collect(Collectors.toList()));
        assertNull(result.getFailureReason());
    }

    @Test
    public void shouldNotEmitMachineProhibitedWhenNoBinding() {
        Cd15MachineCandidateResolution result = resolver.resolveDetailed(
                "CF001", "BR001", "NIGHT", shiftStart(), shiftStart().plusHours(8),
                Arrays.asList(machine("M1", "1", "NIGHT"), machine("M2", "1", "NIGHT")),
                Collections.emptyList(),
                Arrays.asList(restriction("M1", "1"), restriction("M2", "1")),
                Collections.emptyList());

        assertEquals(0, result.getCandidates().size());
        // 无绑定时不存在"绑定机台全部被排除"的语义，不应返回 MACHINE_PROHIBITED。
        assertEquals("NO_AVAILABLE_MACHINE", result.getFailureReason());
    }

    private LocalDateTime shiftStart() {
        return LocalDateTime.of(2026, 6, 12, 22, 0);
    }

    private Cd15MachineResource machine(String code, String status, String openShift) {
        return Cd15MachineResource.builder().machineCode(code).status(status)
                .openMachineClass(openShift).build();
    }

    private Cd15MachineResource machine(String code, String status, String openShift,
                                        String clothWidthMin, String clothWidthMax) {
        return Cd15MachineResource.builder().machineCode(code).status(status)
                .openMachineClass(openShift)
                .clothWidthMin(new BigDecimal(clothWidthMin))
                .clothWidthMax(new BigDecimal(clothWidthMax)).build();
    }

    private Cd15MachineRollBinding binding(String bigRollCode, String machineCode) {
        return Cd15MachineRollBinding.builder().bigRollCode(bigRollCode)
                .machineCode(machineCode).shiftCode("01,02,03,NIGHT").build();
    }

    private Cd15MachineRestriction restriction(String machineCode, String jobType) {
        return Cd15MachineRestriction.builder().steelStripCode("CF001")
                .machineCode(machineCode).jobType(jobType).build();
    }
}
