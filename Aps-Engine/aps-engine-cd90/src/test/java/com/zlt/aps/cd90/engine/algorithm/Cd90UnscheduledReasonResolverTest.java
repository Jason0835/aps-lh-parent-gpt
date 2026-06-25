package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90UnscheduledReason;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 机台筛选失败原因稳定编码测试。 */
public class Cd90UnscheduledReasonResolverTest {

    private final Cd90UnscheduledReasonResolver resolver = new Cd90UnscheduledReasonResolver();

    @Test
    public void shouldKeepMachineProhibitedAsIndependentReason() {
        Cd90UnscheduledReason reason = resolver.resolve("MACHINE_PROHIBITED");

        assertEquals("MACHINE_PROHIBITED", reason.getReasonCode());
        assertEquals("MACHINE_FILTER", reason.getFailStage());
    }

    @Test
    public void shouldMapWidthMismatchAsIndependentReason() {
        Cd90UnscheduledReason reason = resolver.resolve("WIDTH_MISMATCH");

        assertEquals("WIDTH_MISMATCH", reason.getReasonCode());
        assertEquals("MACHINE_FILTER", reason.getFailStage());
    }

    @Test
    public void shouldMapLimitReasonsToStableUnscheduledReasons() {
        Cd90UnscheduledReason tooling = resolver.resolve("TOOLING_LIMIT");
        Cd90UnscheduledReason capacity = resolver.resolve("CAPACITY_LIMIT");

        assertEquals("ROLL_TOOL_LIMIT", tooling.getReasonCode());
        assertEquals("ROLL_TOOL", tooling.getFailStage());
        assertEquals("NO_AVAILABLE_MACHINE", capacity.getReasonCode());
        assertEquals("MACHINE_FILTER", capacity.getFailStage());
    }
}