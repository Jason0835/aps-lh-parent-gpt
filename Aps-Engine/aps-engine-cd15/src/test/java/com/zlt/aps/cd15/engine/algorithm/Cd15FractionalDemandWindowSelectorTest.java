package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 小数备库班数对应需求窗口测试。 */
public class Cd15FractionalDemandWindowSelectorTest {

    private final Cd15FractionalDemandWindowSelector selector =
            new Cd15FractionalDemandWindowSelector();

    @Test
    public void shouldScaleLastShiftForTwoAndHalfShiftDepth() {
        List<Cd15DemandShift> result = selector.select(Arrays.asList(
                shift(0, "100"), shift(8, "120"), shift(16, "80"), shift(24, "60")),
                new BigDecimal("2.5"));

        assertEquals(3, result.size());
        assertEquals(new BigDecimal("100"), result.get(0).getSteelStripDemandQuantity());
        assertEquals(new BigDecimal("120"), result.get(1).getSteelStripDemandQuantity());
        assertEquals(new BigDecimal("40"), result.get(2).getSteelStripDemandQuantity());
        assertEquals(new BigDecimal("0.5"), result.get(2).getWindowWeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveDepth() {
        selector.select(Arrays.asList(shift(0, "100")), BigDecimal.ZERO);
    }

    private Cd15DemandShift shift(int hours, String demand) {
        return Cd15DemandShift.builder()
                .steelStripCode("C01")
                .classField("CLASS1")
                .shiftKey("S" + hours)
                .startTime(LocalDateTime.of(2026, 6, 13, 6, 0).plusHours(hours))
                .formingQuantity(new BigDecimal("100"))
                .steelStripDemandQuantity(new BigDecimal(demand))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }
}
