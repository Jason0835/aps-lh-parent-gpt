package com.zlt.aps.tc.component;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 胎侧六班排程MES下发组装测试。
 */
public class TcScheduleResultIssueAssemblerTest {

    /**
     * 验证六班按前一日、当日、后一日拆分，并完整携带版本、幂等键和组织字段。
     */
    @Test
    public void shouldAssembleSixShiftsIntoThreeMesBusinessDates() {
        TcScheduleResult result = new TcScheduleResult();
        result.setId(1001L);
        result.setFactoryCode("116");
        result.setScheduleDate(DateUtil.parseDate("2026-07-15"));
        result.setBatchNo("TC-20260715-001");
        result.setOrderNo("TC-20260715-001-0001");
        result.setSidewallCode("TC001");
        result.setMachineCode("TC-M01");
        result.setTaskVersion(3L);
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            result.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder),
                    BigDecimal.valueOf(shiftOrder * 100L));
            result.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), shiftOrder);
            result.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder), "SHIFT-" + shiftOrder);
        }

        TcScheduleResultIssueAssembler assembler = new TcScheduleResultIssueAssembler();
        List<TcScheduleResultIssue> issueList = assembler.assemble(
                Collections.singletonList(result), "TCREL-001");

        Assert.assertEquals(3, issueList.size());
        TcScheduleResultIssue previousDay = issueList.get(0);
        Assert.assertEquals(LocalDate.of(2026, 7, 14), previousDay.getScheduleDate());
        Assert.assertEquals(new BigDecimal("100"), previousDay.getMidPlanQty());
        Assert.assertEquals(Integer.valueOf(1), previousDay.getMidProduceOrder());

        TcScheduleResultIssue scheduleDay = issueList.get(1);
        Assert.assertEquals(LocalDate.of(2026, 7, 15), scheduleDay.getScheduleDate());
        Assert.assertEquals(new BigDecimal("200"), scheduleDay.getNightPlanQty());
        Assert.assertEquals(new BigDecimal("300"), scheduleDay.getDayPlanQty());
        Assert.assertEquals(new BigDecimal("400"), scheduleDay.getMidPlanQty());

        TcScheduleResultIssue nextDay = issueList.get(2);
        Assert.assertEquals(LocalDate.of(2026, 7, 16), nextDay.getScheduleDate());
        Assert.assertEquals(new BigDecimal("500"), nextDay.getNightPlanQty());
        Assert.assertEquals(new BigDecimal("600"), nextDay.getDayPlanQty());
        Assert.assertEquals("TC-20260715-001|TC-20260715-001-0001|3",
                nextDay.getIdempotencyKey());
        Assert.assertEquals("TCREL-001", nextDay.getDataVersion());
        Assert.assertEquals("116", nextDay.getFactoryCode());
        Assert.assertEquals("116", nextDay.getCompanyCode());
    }
}
