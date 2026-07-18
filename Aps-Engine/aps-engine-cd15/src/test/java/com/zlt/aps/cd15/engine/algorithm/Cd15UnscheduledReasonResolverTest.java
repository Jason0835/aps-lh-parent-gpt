package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15UnscheduledReason;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 机台筛选失败原因稳定编码测试。 */
public class Cd15UnscheduledReasonResolverTest {

    private final Cd15UnscheduledReasonResolver resolver = new Cd15UnscheduledReasonResolver();

    @Test
    public void shouldKeepMachineProhibitedAsIndependentReason() {
        Cd15UnscheduledReason reason = resolver.resolve("MACHINE_PROHIBITED");

        assertEquals("MACHINE_PROHIBITED", reason.getReasonCode());
        assertEquals("MACHINE_FILTER", reason.getFailStage());
    }

    @Test
    public void shouldMapWidthMismatchAsIndependentReason() {
        Cd15UnscheduledReason reason = resolver.resolve("WIDTH_MISMATCH");

        assertEquals("WIDTH_MISMATCH", reason.getReasonCode());
        assertEquals("MACHINE_FILTER", reason.getFailStage());
    }

    @Test
    public void shouldMapLimitReasonsToStableUnscheduledReasons() {
        Cd15UnscheduledReason tooling = resolver.resolve("TOOLING_LIMIT");
        Cd15UnscheduledReason capacity = resolver.resolve("CAPACITY_LIMIT");

        assertEquals("ROLL_TOOL_LIMIT", tooling.getReasonCode());
        assertEquals("ROLL_TOOL", tooling.getFailStage());
        assertEquals("NO_AVAILABLE_MACHINE", capacity.getReasonCode());
        assertEquals("MACHINE_FILTER", capacity.getFailStage());
    }
    @Test
    public void shouldMapAgingLimitAsIndependentReason() {
        Cd15UnscheduledReason reason = resolver.resolve("AGING_PERIOD_LIMIT");

        assertEquals("AGING_PERIOD_LIMIT", reason.getReasonCode());
        assertEquals("BIG_ROLL_AGING", reason.getFailStage());
    }
}