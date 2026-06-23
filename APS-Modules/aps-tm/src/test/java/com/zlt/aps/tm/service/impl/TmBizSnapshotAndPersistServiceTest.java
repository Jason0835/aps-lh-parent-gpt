package com.zlt.aps.tm.service.impl;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.impl.TmPersistService;
import com.zlt.aps.tm.engine.service.impl.TmSnapshotBuildService;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 胎面自动排程快照落库服务测试。
 *
 * <p>验证落库逐条失败时会记录错误信息，并继续处理后续可写入数据。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TmBizSnapshotAndPersistServiceTest {

    @Mock
    private TmSnapshotBuildService snapshotBuildService;

    @Mock
    private TmPersistService persistService;

    @Mock
    private TmScheduleResultMapper scheduleResultMapper;

    @Mock
    private TmScheduleResultExplainMapper scheduleResultExplainMapper;

    /**
     * 测试内容：验证快照落库服务拒绝空上下文。
     * 测试场景：直接调用 snapshotAndPersist(null)。
     * 预期结果：抛出 IllegalArgumentException，避免后续读取任务列表时报空指针。
     */
    @Test(expected = IllegalArgumentException.class)
    public void snapshotAndPersistShouldRejectNullContext() {
        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(null);
    }

    /**
     * 测试内容：验证快照落库服务遇到空任务列表时直接返回空汇总。
     * 测试场景：上下文存在，但 taskDraftList 为空。
     * 预期结果：结果数、解释数、错误数均为 0，且不调用结果表和解释表 mapper。
     */
    @Test
    public void snapshotAndPersistShouldReturnEmptyPersistResultWhenTaskListEmpty() {
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.emptyList());

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);

        TmPersistResult persistResult = context.getPersistResult();
        assertEquals(0, persistResult.getResultCount());
        assertEquals(0, persistResult.getExplainCount());
        assertEquals(0, persistResult.getErrorCount());
        verify(scheduleResultMapper, never()).insert(any(TmScheduleResult.class));
        verify(scheduleResultExplainMapper, never()).insert(any(TmScheduleResultExplain.class));
    }

    /**
     * 测试内容：验证结果表写入失败时记录错误并继续写解释表。
     * 测试场景：结果表 mapper 抛出运行时异常，解释表 mapper 正常写入。
     * 预期结果：结果数为 0，解释数为 1，错误数为 1，错误信息包含订单号和失败原因。
     */
    @Test
    public void snapshotAndPersistShouldRecordResultInsertErrorAndContinueExplainInsert() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = new TmScheduleResult();
        result.setOrderNo(task.getOrderNo());
        result.setTreadCode(task.getTreadCode());
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertUnplanned(task, context)).thenReturn(result);
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultMapper.insert(result)).thenThrow(new RuntimeException("结果写入失败"));
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);

        TmPersistResult persistResult = context.getPersistResult();
        assertEquals(0, persistResult.getResultCount());
        assertEquals(1, persistResult.getExplainCount());
        assertEquals(1, persistResult.getErrorCount());
        assertTrue(persistResult.getLastErrorMsg().contains("ORD-001"));
        assertTrue(persistResult.getErrorMsgList().get(0).contains("结果写入失败"));
    }

    /**
     * 测试内容：验证解释表写入失败时计入落库错误。
     * 测试场景：结果表 mapper 正常写入，解释表 mapper 抛出运行时异常。
     * 预期结果：结果数为 1，解释数为 0，错误数为 1，最后错误包含解释表和订单号。
     */
    @Test
    public void snapshotAndPersistShouldRecordExplainInsertError() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = new TmScheduleResult();
        result.setOrderNo(task.getOrderNo());
        result.setTreadCode(task.getTreadCode());
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertUnplanned(task, context)).thenReturn(result);
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultMapper.insert(result)).thenReturn(1);
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class)))
                .thenThrow(new RuntimeException("解释写入失败"));

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);

        TmPersistResult persistResult = context.getPersistResult();
        assertEquals(1, persistResult.getResultCount());
        assertEquals(0, persistResult.getExplainCount());
        assertEquals(1, persistResult.getErrorCount());
        assertTrue(persistResult.getLastErrorMsg().contains("解释表写入失败"));
        assertTrue(persistResult.getLastErrorMsg().contains("ORD-001"));
    }

    /**
     * 构造未排任务草稿。
     *
     * @return 未排任务草稿
     */
    private TmTaskDraft buildTask() {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo("ORD-001");
        task.setTreadCode("TR-001");
        task.setPlanQty(BigDecimal.TEN);
        task.setUnplannedReasonCode("NO_MACHINE");
        return task;
    }
}
