package com.zlt.aps.cd15.itf;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.itf.mes.domain.MesCd15ScheduleResult;
import com.zlt.aps.itf.mes.mapper.Cd15ScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.impl.Cd15ScheduleResultIssueWriter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 斜裁排程结果 MES 宽表写入测试。
 */
public class Cd15ScheduleResultIssueWriterTest {

    /** 验证夜、早、中班次聚合为同一条自然日宽表。 */
    @Test
    public void shouldAggregateThreeShiftsIntoWideRow() {
        Cd15ScheduleResultIssueMapper issueMapper =
                mock(Cd15ScheduleResultIssueMapper.class);
        when(issueMapper.batchInsert(anyList()))
                .thenAnswer(invocation -> ((List<?>) invocation
                        .getArgument(0)).size());
        Cd15ScheduleResultIssueWriter writer =
                new Cd15ScheduleResultIssueWriter(issueMapper);

        Cd15ScheduleResultIssue night = this.issue("夜班", 100D, 1);
        Cd15ScheduleResultIssue day = this.issue("早班", 200D, 2);
        Cd15ScheduleResultIssue mid = this.issue("中班", 300D, 3);

        int insertedCount = writer.replace(
                Arrays.asList(night, day, mid),
                "VERSION-1", "COMPANY-1", "FACTORY-1");

        Assert.assertEquals(1, insertedCount);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesCd15ScheduleResult>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(issueMapper).batchInsert(captor.capture());
        MesCd15ScheduleResult row = captor.getValue().get(0);
        Assert.assertEquals("GROUP-1", row.getSplitBatchNo());
        Assert.assertEquals(new BigDecimal("100.0"),
                row.getNightPlanQty1());
        Assert.assertEquals(new BigDecimal("200.0"),
                row.getDayPlanQty1());
        Assert.assertEquals(new BigDecimal("300.0"),
                row.getMidPlanQty1());
        Assert.assertEquals(new BigDecimal("40.0"),
                row.getCxClass5Plan());
    }

    /** 验证宽表分批覆盖时每批均先删除旧业务键再写入新版本。 */
    @Test
    public void shouldReplaceWideRowsInBatchesBeforeNoticeStage() {
        Cd15ScheduleResultIssueMapper issueMapper =
                mock(Cd15ScheduleResultIssueMapper.class);
        when(issueMapper.batchInsert(anyList()))
                .thenAnswer(invocation -> ((List<?>) invocation
                        .getArgument(0)).size());
        Cd15ScheduleResultIssueWriter writer =
                new Cd15ScheduleResultIssueWriter(issueMapper);

        List<Cd15ScheduleResultIssue> issueList = new ArrayList<>();
        for (int index = 0; index < 21; index++) {
            Cd15ScheduleResultIssue issue = this.issue("夜班", 100D, 1);
            issue.setOrderNo("ORDER-" + index);
            issueList.add(issue);
        }

        int insertedCount = writer.replace(issueList, "VERSION-1",
                "COMPANY-1", "FACTORY-1");

        Assert.assertEquals(21, insertedCount);
        InOrder writeOrder = inOrder(issueMapper);
        writeOrder.verify(issueMapper).batchDeleteByBusinessKey(
                anyList(), eq("FACTORY-1"));
        writeOrder.verify(issueMapper).batchInsert(anyList());
        writeOrder.verify(issueMapper).batchDeleteByBusinessKey(
                anyList(), eq("FACTORY-1"));
        writeOrder.verify(issueMapper).batchInsert(anyList());
        writeOrder.verifyNoMoreInteractions();
    }

    /** 验证非分裁结果不向 MES 传分裁批次号。 */
    @Test
    public void shouldLeaveSplitBatchNoBlankForSingleCut() {
        Cd15ScheduleResultIssueMapper issueMapper =
                mock(Cd15ScheduleResultIssueMapper.class);
        when(issueMapper.batchInsert(anyList())).thenReturn(1);
        Cd15ScheduleResultIssueWriter writer =
                new Cd15ScheduleResultIssueWriter(issueMapper);
        Cd15ScheduleResultIssue issue = this.issue("夜班", 100D, 1);
        issue.setCutMode("SINGLE");

        writer.replace(Arrays.asList(issue), "VERSION-1",
                "COMPANY-1", "FACTORY-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MesCd15ScheduleResult>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(issueMapper).batchInsert(captor.capture());
        Assert.assertNull(captor.getValue().get(0).getSplitBatchNo());
    }

    /** 构造满足 MES 必填契约的班次 issue。 */
    private Cd15ScheduleResultIssue issue(
            String shiftName, Double planQty, Integer produceOrder) {
        Cd15ScheduleResultIssue issue = new Cd15ScheduleResultIssue();
        issue.setScheduleDate(Date.valueOf(LocalDate.of(2026, 8, 20)));
        issue.setCd15BatchNo("BATCH-1");
        issue.setOrderNo("ORDER-1");
        issue.setGroupNo("GROUP-1");
        issue.setCutMode("SPLIT");
        issue.setBigRollCode("ROLL-1");
        issue.setMachineCode("G1101");
        issue.setStorageLaneCode("A01");
        issue.setEmbryoSpecDesc("胎胚规格A");
        issue.setSteelStripCode("BELT-1");
        issue.setMaterialCode("BELT-1");
        issue.setUnitConsume(new BigDecimal("1.5"));
        issue.setStockQty(50D);
        issue.setCuttingAngle("15");
        issue.setShiftName(shiftName);
        issue.setPlanQty(planQty);
        issue.setProduceOrder(produceOrder);
        issue.setCxClass1Plan(10D);
        issue.setCxClass2Plan(20D);
        issue.setCxClass3Plan(30D);
        issue.setCxClass4Plan(40D);
        return issue;
    }
}
