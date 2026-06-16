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
        assertEquals("大卷绑定机台均不可作业", reason.getReasonDescription());
    }

    @Test
    public void shouldKeepMissingMachineMappingAsIndependentReason() {
        Cd90UnscheduledReason reason = resolver.resolve("NO_MACHINE_MAPPING");

        assertEquals("NO_MACHINE_MAPPING", reason.getReasonCode());
        assertEquals("MACHINE_FILTER", reason.getFailStage());
    }
}
