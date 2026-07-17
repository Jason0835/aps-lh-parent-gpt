package com.zlt.aps.tc.service.mes;

import cn.hutool.core.date.DateUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * 胎侧六班MES业务日期映射测试。
 */
public class TcShiftBusinessDateResolverTest {

    /**
     * 验证六班与MES前一日、当日、后一日三班的固定映射。
     */
    @Test
    public void shouldResolveSixShiftBusinessDateMapping() {
        Date resultDate = DateUtil.parseDate("2026-07-15");
        Assert.assertEquals(Integer.valueOf(1), TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-14"), "MID"));
        Assert.assertEquals(Integer.valueOf(2), TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-15"), "NIGHT"));
        Assert.assertEquals(Integer.valueOf(3), TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-15"), "DAY"));
        Assert.assertEquals(Integer.valueOf(4), TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-15"), "MID"));
        Assert.assertEquals(Integer.valueOf(5), TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-16"), "NIGHT"));
        Assert.assertEquals(Integer.valueOf(6), TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-16"), "DAY"));
        Assert.assertNull(TcShiftBusinessDateResolver.resolveShiftOrder(
                resultDate, DateUtil.parseDate("2026-07-16"), "MID"));
    }

    /**
     * 验证按班次解析MES业务日期和班别，不依赖六班分支硬编码。
     */
    @Test
    public void shouldResolveMesDateAndShiftCode() {
        Date resultDate = DateUtil.parseDate("2026-07-15");
        Assert.assertEquals("2026-07-14", DateUtil.formatDate(
                TcShiftBusinessDateResolver.resolveMesBusinessDate(resultDate, 1)));
        Assert.assertEquals("MID", TcShiftBusinessDateResolver.resolveMesShiftCode(1));
        Assert.assertEquals("NIGHT", TcShiftBusinessDateResolver.resolveMesShiftCode(2));
        Assert.assertEquals("DAY", TcShiftBusinessDateResolver.resolveMesShiftCode(3));
        Assert.assertEquals("MID", TcShiftBusinessDateResolver.resolveMesShiftCode(4));
        Assert.assertEquals("2026-07-16", DateUtil.formatDate(
                TcShiftBusinessDateResolver.resolveMesBusinessDate(resultDate, 6)));
    }
}
