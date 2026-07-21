package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15InboundRecord;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 前序斜裁入库互斥计算测试。
 */
public class Cd15InboundResolverTest {

    private final Cd15InboundResolver resolver = new Cd15InboundResolver();

    /**
     * T-17：同一任务存在实际入库时只取实际，不叠加计划入库。
     */
    @Test
    public void actualInboundShouldReplacePlannedInboundForSameTask() {
        List<Cd15InboundRecord> result = resolver.resolve(Arrays.asList(
                record("T1", false, 2), record("T1", true, 1), record("T2", false, 3)
        ));

        assertEquals(2, result.size());
        assertEquals(1, result.stream().filter(item -> "T1".equals(item.getTaskKey()))
                .findFirst().get().getVehicleCount());
        assertEquals(4, result.stream().mapToInt(Cd15InboundRecord::getVehicleCount).sum());
    }

    private Cd15InboundRecord record(String taskKey, boolean actual, int vehicles) {
        return Cd15InboundRecord.builder()
                .taskKey(taskKey)
                .actual(actual)
                
                .laneCode("L1")
                .vehicleCount(vehicles)
                .build();
    }
}
