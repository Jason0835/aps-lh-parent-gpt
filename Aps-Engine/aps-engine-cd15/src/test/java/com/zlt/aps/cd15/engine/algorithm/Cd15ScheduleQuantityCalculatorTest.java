package com.zlt.aps.cd15.engine.algorithm;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 斜裁自动排程数量计算测试。
 */
public class Cd15ScheduleQuantityCalculatorTest {

    private final Cd15ScheduleQuantityCalculator calculator = new Cd15ScheduleQuantityCalculator();

    /**
     * T-04：收尾规格只叠加一次损耗，不补最小起排量和卷曲整倍数。
     */
    @Test
    public void closeOutQuantityShouldNotRoundOrFillMinimum() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("120"), true, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("87"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("126"), result);
    }

    /**
     * T-05：非收尾规格先补最小起排量，再按卷曲长度向上取整。
     */
    @Test
    public void normalQuantityShouldFillMinimumAndRoundUp() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("120"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("87"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("348"), result);
    }
    /**
     * T-06：非收尾规格先加损耗并按整车取整，再按完整计划量判断阈值并均分。
     */
    @Test
    public void normalQuantityShouldShareWhenNetDemandExceedsThreshold() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("2500"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("80"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("1320"), result);
    }
    @Test
    public void normalQuantityShouldRoundByVehiclePlanQuantity() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("1305"), false, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("31.32"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("1315.44"), result);
    }

    /** T-07：单规格一出二按均分前完整双路计划量拆分，并按双片宽度步长向上取整。 */
    @Test
    public void singleSpecSplitShouldShareFinalPlanQuantityAcrossShifts() {
        BigDecimal result = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("5405.778"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("2702.889"),
                new BigDecimal("2000"), new BigDecimal("37.8"), false);

        assertEquals(new BigDecimal("2702.9268"), result);
    }

    /** T-08：真实单规格分裁计划量超过阈值时，按双片步长拆为相邻两班且合计不变。 */
    @Test
    public void realSingleSpecSplitQuantityShouldBeSharedAcrossTwoShifts() {
        BigDecimal firstShiftQuantity = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("45.384"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("2000"), new BigDecimal("37.2"), false);
        BigDecimal remainderQuantity =
                calculator.calculateSingleSpecSplitEqualShareRemainder(
                        new BigDecimal("45.384"), false, BigDecimal.ZERO,
                        new BigDecimal("300"), new BigDecimal("5305.5384"),
                        new BigDecimal("2000"), new BigDecimal("37.2"), false);

        assertTrue(calculator.requiresSingleSpecSplitEqualShare(
                new BigDecimal("45.384"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("2000"), false));
        assertEquals(new BigDecimal("2652.8064"), firstShiftQuantity);
        assertEquals(new BigDecimal("2652.732"), remainderQuantity);
        assertEquals(new BigDecimal("5305.5384"),
                firstShiftQuantity.add(remainderQuantity));
    }

    /** T-09：下一班承接均分余量时不再均分或重复叠加损耗。 */
    @Test
    public void equalShareRemainderShouldNotBeSharedAgain() {
        BigDecimal result = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("2702.8512"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("2702.889"),
                new BigDecimal("2000"), new BigDecimal("37.8"), true);

        assertEquals(new BigDecimal("2702.8512"), result);
    }
}
