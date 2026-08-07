package com.zlt.aps.cd15.itf;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.itf.mes.mapper.Cd15ScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.impl.Cd15ScheduleResultIssueWriter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 斜裁排程结果 MES 中间表写入测试。
 */
public class Cd15ScheduleResultIssueWriterTest {

    /** 验证分批覆盖时每批均先删除旧业务键再写入新版本。 */
    @Test
    public void shouldReplaceInBatchesBeforeNoticeStage() {
        Cd15ScheduleResultIssueMapper issueMapper =
                mock(Cd15ScheduleResultIssueMapper.class);
        when(issueMapper.batchInsert(anyList(), eq("VERSION-1"),
                eq("COMPANY-1"), eq("FACTORY-1")))
                .thenAnswer(invocation -> ((List<?>) invocation
                        .getArgument(0)).size());
        Cd15ScheduleResultIssueWriter writer =
                new Cd15ScheduleResultIssueWriter(issueMapper);

        List<Cd15ScheduleResultIssue> issueList = new ArrayList<>();
        for (int index = 0; index < 41; index++) {
            Cd15ScheduleResultIssue issue = new Cd15ScheduleResultIssue();
            issue.setOrderNo("ORDER-" + index);
            issueList.add(issue);
        }

        int insertedCount = writer.replace(issueList, "VERSION-1",
                "COMPANY-1", "FACTORY-1");

        Assert.assertEquals(41, insertedCount);
        InOrder writeOrder = inOrder(issueMapper);
        writeOrder.verify(issueMapper).batchDeleteByBusinessKey(
                anyList(), eq("FACTORY-1"));
        writeOrder.verify(issueMapper).batchInsert(anyList(),
                eq("VERSION-1"), eq("COMPANY-1"), eq("FACTORY-1"));
        writeOrder.verify(issueMapper).batchDeleteByBusinessKey(
                anyList(), eq("FACTORY-1"));
        writeOrder.verify(issueMapper).batchInsert(anyList(),
                eq("VERSION-1"), eq("COMPANY-1"), eq("FACTORY-1"));
        writeOrder.verifyNoMoreInteractions();
    }
}
