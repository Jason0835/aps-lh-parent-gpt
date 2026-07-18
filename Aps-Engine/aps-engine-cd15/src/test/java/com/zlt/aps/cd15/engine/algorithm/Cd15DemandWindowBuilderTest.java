package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.constant.Cd15StopMode;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 成型需求自然窗口构建测试。
 */
public class Cd15DemandWindowBuilderTest {

    private final Cd15DemandWindowBuilder builder = new Cd15DemandWindowBuilder(new Cd15FractionalDemandWindowSelector());

    /**
     * T-21：成型停产1天时保留自然窗口并额外读取紧邻的一个班次。
     */
    @Test
    public void oneDayFormingStopShouldAppendOnlyOneShift() {
        List<Cd15DemandShift> result = builder.build(shifts(6), new BigDecimal("2.5"),
                Cd15StopMode.ONE_DAY_FORMING_STOP, null);

        assertEquals(4, result.size());
        assertEquals(new BigDecimal("0.5"), result.get(2).getWindowWeight());
        assertEquals(BigDecimal.ONE, result.get(3).getWindowWeight());
    }

    /**
     * T-03：长停产只保留停产边界之前的班次。
     */
    @Test
    public void longStopShouldCutAtBoundary() {
        List<Cd15DemandShift> result = builder.build(shifts(5), new BigDecimal("4"),
                Cd15StopMode.LONG_STOP, 2);

        assertEquals(2, result.size());
        assertFalse(result.get(0).isStopped());
        assertFalse(result.get(1).isStopped());
    }

    private List<Cd15DemandShift> shifts(int count) {
        Cd15DemandShift[] values = new Cd15DemandShift[count];
        for (int index = 0; index < count; index++) {
            values[index] = Cd15DemandShift.builder()
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
