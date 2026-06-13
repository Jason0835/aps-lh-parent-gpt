package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 前序直裁入库互斥计算测试。
 */
public class Cd90InboundResolverTest {

    private final Cd90InboundResolver resolver = new Cd90InboundResolver();

    /**
     * T-17：同一任务存在实际入库时只取实际，不叠加计划入库。
     */
    @Test
    public void actualInboundShouldReplacePlannedInboundForSameTask() {
        List<Cd90InboundRecord> result = resolver.resolve(Arrays.asList(
                record("T1", false, 2), record("T1", true, 1), record("T2", false, 3)
        ));

        assertEquals(2, result.size());
        assertEquals(1, result.stream().filter(item -> "T1".equals(item.getTaskKey()))
                .findFirst().get().getVehicleCount());
        assertEquals(4, result.stream().mapToInt(Cd90InboundRecord::getVehicleCount).sum());
    }

    private Cd90InboundRecord record(String taskKey, boolean actual, int vehicles) {
        return Cd90InboundRecord.builder()
                .taskKey(taskKey)
                .actual(actual)
                .clothCode("C1")
                .laneCode("L1")
                .vehicleCount(vehicles)
                .build();
    }
}
