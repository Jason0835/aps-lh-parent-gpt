package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingBuildResult;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * GDYY实际库存与计划库存转换为大卷成熟流水的规则测试。
 */
public class Cd15BigRollAgingStockBuilderTest {

    private final Cd15BigRollAgingStockBuilder builder = new Cd15BigRollAgingStockBuilder();

    @Test
    public void shouldReleaseActualStockFromStockInTime() {
        GdyyStock stock = stock("BR001", "2", "120", Timestamp.valueOf("2026-06-12 10:00:00"));
        stock.setModifyNum(new BigDecimal("1"));
        stock.setBadNum(new BigDecimal("0.5"));

        Cd15BigRollAgingBuildResult result = builder.build(
                Collections.singletonList(stock), Collections.emptyList(), 24);

        assertEquals(1, result.getStocks().size());
        assertEquals(0, new BigDecimal("300").compareTo(result.getStocks().get(0).getAvailableQuantity()));
        assertEquals(LocalDateTime.of(2026, 6, 13, 10, 0), result.getStocks().get(0).getReleaseTime());
        assertTrue(result.getDataMissingBigRollCodes().isEmpty());
    }

    @Test
    public void shouldRejectActualStockWithoutStockInTime() {
        GdyyStock stock = stock("BR001", "1", "120", null);

        Cd15BigRollAgingBuildResult result = builder.build(
                Collections.singletonList(stock), Collections.emptyList(), 24);

        assertTrue(result.getStocks().isEmpty());
        assertTrue(result.getDataMissingBigRollCodes().contains("BR001"));
    }

    @Test
    public void shouldRejectActualStockWithoutPositiveStockMeters() {
        GdyyStock stock = stock("BR001", "1", null, Timestamp.valueOf("2026-06-12 10:00:00"));

        Cd15BigRollAgingBuildResult result = builder.build(
                Collections.singletonList(stock), Collections.emptyList(), 24);

        assertTrue(result.getStocks().isEmpty());
        assertTrue(result.getDataMissingBigRollCodes().contains("BR001"));
    }

    @Test
    public void shouldRejectActualStockWithZeroStockMeters() {
        GdyyStock stock = stock("BR001", "1", "0", Timestamp.valueOf("2026-06-12 10:00:00"));

        Cd15BigRollAgingBuildResult result = builder.build(
                Collections.singletonList(stock), Collections.emptyList(), 24);

        assertTrue(result.getStocks().isEmpty());
        assertTrue(result.getDataMissingBigRollCodes().contains("BR001"));
    }

    @Test
    public void shouldReleasePlanAtShiftEndPlusAgingPeriod() {
        GdyyScheduleResult plan = plan("BR001", "2026-06-12 00:00:00", "100");

        Cd15BigRollAgingBuildResult result = builder.build(
                Collections.emptyList(), Collections.singletonList(plan), 24);

        Cd15BigRollAgingStock stock = result.getStocks().get(0);
        assertEquals("GDYY_PLAN", stock.getSourceType());
        assertEquals(0, new BigDecimal("100").compareTo(stock.getAvailableQuantity()));
        assertEquals(LocalDateTime.of(2026, 6, 13, 14, 0), stock.getReleaseTime());
    }

    @Test
    public void shouldExcludePlanShiftCoveredByActualInbound() {
        GdyyStock actual = stock("BR001", "1", "80", Timestamp.valueOf("2026-06-12 10:00:00"));
        GdyyScheduleResult plan = plan("BR001", "2026-06-12 00:00:00", "100");

        Cd15BigRollAgingBuildResult result = builder.build(
                Collections.singletonList(actual), Collections.singletonList(plan), 24);

        assertEquals(1, result.getStocks().size());
        assertFalse(result.getStocks().stream()
                .anyMatch(item -> "GDYY_PLAN".equals(item.getSourceType())));
    }

    private GdyyStock stock(String bigRollCode, String quantity, String stockMeters, Date stockInTime) {
        GdyyStock stock = new GdyyStock();
        stock.setId(1L);
        stock.setBigRollCode(bigRollCode);
        stock.setBigRollBarcode("BARCODE-1");
        stock.setStockNum(new BigDecimal(quantity));
        stock.setModifyNum(BigDecimal.ZERO);
        stock.setBadNum(BigDecimal.ZERO);
        stock.setStockMeters(stockMeters == null ? null : new BigDecimal(stockMeters));
        stock.setInboundTime(stockInTime);
        return stock;
    }

    private GdyyScheduleResult plan(String bigRollCode, String class3Date, String quantity) {
        GdyyScheduleResult plan = new GdyyScheduleResult();
        plan.setId(2L);
        plan.setBigRollCode(bigRollCode);
        plan.setClass3ScheduleDate(Timestamp.valueOf(class3Date));
        plan.setClass3PlanQty(Double.valueOf(quantity));
        return plan;
    }
}
