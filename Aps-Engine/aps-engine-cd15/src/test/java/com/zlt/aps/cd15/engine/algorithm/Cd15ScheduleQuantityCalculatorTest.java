package com.zlt.aps.cd15.engine.algorithm;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 斜裁自动排程数量计算测试。 */
public class Cd15ScheduleQuantityCalculatorTest {

    private final Cd15ScheduleQuantityCalculator calculator =
            new Cd15ScheduleQuantityCalculator();

    /** T-04：收尾规格只叠加一次损耗，不补最小起排量和卷曲整倍数。 */
    @Test
    public void closeOutQuantityShouldNotRoundOrFillMinimum() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("120"), true, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("87"),
                new BigDecimal("2000"));

        assertEquals(new BigDecimal("126"), result);
    }

    /** T-05：非收尾规格先补最小起排量，再按卷曲长度向上取整。 */
    @Test
    public void normalQuantityShouldFillMinimumAndRoundUp() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("120"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("87"),
                new BigDecimal("2000"));

        assertEquals(new BigDecimal("348"), result);
    }

    /** T-06：完整计划量超过阈值时，按所需班次数和整车步长动态均衡。 */
    @Test
    public void normalQuantityShouldBalanceByRequiredShiftCount() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("2500"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("80"),
                new BigDecimal("2000"));

        assertEquals(new BigDecimal("1360"), result);
    }

    @Test
    public void normalQuantityShouldRoundByVehiclePlanQuantity() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("1305"), false, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("31.32"),
                new BigDecimal("2000"));

        assertEquals(new BigDecimal("1315.44"), result);
    }

    /** T-07：5405.778米按2000米上限动态均衡为三个班次。 */
    @Test
    public void singleSpecSplitShouldBalanceAcrossThreeShifts() {
        BigDecimal first = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("5405.778"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("2702.889"),
                new BigDecimal("2000"), new BigDecimal("37.8"), false);
        BigDecimal remainder =
                calculator.calculateSingleSpecSplitEqualShareRemainder(
                        new BigDecimal("5405.778"), false, BigDecimal.ZERO,
                        new BigDecimal("300"), new BigDecimal("2702.889"),
                        new BigDecimal("2000"), new BigDecimal("37.8"), false);

        assertEquals(new BigDecimal("1801.926"), first);
        assertEquals(new BigDecimal("3603.852"), remainder);
        assertEquals(new BigDecimal("5405.778"), first.add(remainder));
    }

    /** T-08：5305.5384米按2000米上限均衡为三个班次，后两班继续动态计算。 */
    @Test
    public void realSingleSpecSplitQuantityShouldBeBalancedAcrossThreeShifts() {
        BigDecimal first = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("45.384"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("2000"), new BigDecimal("37.2"), false);
        BigDecimal firstRemainder =
                calculator.calculateSingleSpecSplitEqualShareRemainder(
                        new BigDecimal("45.384"), false, BigDecimal.ZERO,
                        new BigDecimal("300"), new BigDecimal("5305.5384"),
                        new BigDecimal("2000"), new BigDecimal("37.2"), false);
        BigDecimal second = calculator.calculateSingleSpecSplitActualQuantity(
                firstRemainder, false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("2000"), new BigDecimal("37.2"), true);
        BigDecimal third =
                calculator.calculateSingleSpecSplitEqualShareRemainder(
                        firstRemainder, false, new BigDecimal("5"),
                        new BigDecimal("300"), new BigDecimal("5305.5384"),
                        new BigDecimal("2000"), new BigDecimal("37.2"), true);

        assertTrue(calculator.requiresSingleSpecSplitEqualShare(
                new BigDecimal("45.384"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("2000"), false));
        assertEquals(new BigDecimal("1768.5624"), first);
        assertEquals(new BigDecimal("1768.488"), second);
        assertEquals(new BigDecimal("1768.488"), third);
        assertEquals(new BigDecimal("5305.5384"),
                first.add(second).add(third));
    }

    /** T-09：同钢带本班剩余额度会在动态均分结果之上再次硬封顶。 */
    @Test
    public void sameSteelStripRemainingShiftQuantityShouldCapCurrentCandidate() {
        BigDecimal result = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("45.384"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("2000"), new BigDecimal("37.2"), false,
                new BigDecimal("1000"));

        assertEquals(new BigDecimal("999.936"), result);
    }

    /** T-10：实际复产班改用3000米阈值时，同一需求只需均衡到两个班次。 */
    @Test
    public void restartShiftShouldUseRestartStockThresholdAsLimit() {
        BigDecimal first = calculator.calculateSingleSpecSplitActualQuantity(
                new BigDecimal("45.384"), false, BigDecimal.ZERO,
                new BigDecimal("300"), new BigDecimal("5305.5384"),
                new BigDecimal("3000"), new BigDecimal("37.2"), false);
        BigDecimal remainder =
                calculator.calculateSingleSpecSplitEqualShareRemainder(
                        new BigDecimal("45.384"), false, BigDecimal.ZERO,
                        new BigDecimal("300"), new BigDecimal("5305.5384"),
                        new BigDecimal("3000"), new BigDecimal("37.2"), false);

        assertEquals(new BigDecimal("2652.8064"), first);
        assertEquals(new BigDecimal("2652.732"), remainder);
    }
}
