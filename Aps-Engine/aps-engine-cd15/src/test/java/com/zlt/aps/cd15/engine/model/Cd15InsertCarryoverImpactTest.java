package com.zlt.aps.cd15.engine.model;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/** 插单跨班影响模型测试。 */
public class Cd15InsertCarryoverImpactTest {

    @Test
    public void shouldKeepTaskLevelCarryoverDetails() {
        Cd15InsertCarryoverImpact impact = Cd15InsertCarryoverImpact.builder()
                .steelStripCode("211400101")
                .affectedType("INSERT")
                .sourceClassField("CLASS1")
                .targetClassField("CLASS2")
                .carryoverQty(new BigDecimal("125.5"))
                .reasonCode("CAPACITY_LIMIT")
                .build();

        assertEquals("211400101", impact.getSteelStripCode());
        assertEquals("INSERT", impact.getAffectedType());
        assertEquals("CLASS1", impact.getSourceClassField());
        assertEquals("CLASS2", impact.getTargetClassField());
        assertEquals(0, new BigDecimal("125.5").compareTo(impact.getCarryoverQty()));
        assertEquals("CAPACITY_LIMIT", impact.getReasonCode());
    }
}
