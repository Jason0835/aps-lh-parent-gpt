package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.constant.Cd90StopMode;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 成型需求自然窗口构建测试。
 */
public class Cd90DemandWindowBuilderTest {

    private final Cd90DemandWindowBuilder builder = new Cd90DemandWindowBuilder();

    /**
     * T-21：成型停产1天时保留自然窗口并额外读取紧邻的一个班次。
     */
    @Test
    public void oneDayFormingStopShouldAppendOnlyOneShift() {
        List<Cd90DemandShift> result = builder.build(shifts(6), 3,
                Cd90StopMode.ONE_DAY_FORMING_STOP, null);

        assertEquals(4, result.size());
    }

    /**
     * T-03：长停产只保留停产边界之前的班次。
     */
    @Test
    public void longStopShouldCutAtBoundary() {
        List<Cd90DemandShift> result = builder.build(shifts(5), 4,
                Cd90StopMode.LONG_STOP, 2);

        assertEquals(2, result.size());
        assertFalse(result.get(0).isStopped());
        assertFalse(result.get(1).isStopped());
    }

    private List<Cd90DemandShift> shifts(int count) {
        Cd90DemandShift[] values = new Cd90DemandShift[count];
        for (int index = 0; index < count; index++) {
            values[index] = Cd90DemandShift.builder()
                    .shiftKey("S" + (index + 1))
                    .formingQuantity(new BigDecimal(index == 2 ? "0" : "100"))
                    .shiftHours(new BigDecimal("8"))
                    .included(true)
                    .stopped(index == 2)
                    .build();
        }
        return Arrays.asList(values);
    }
}
