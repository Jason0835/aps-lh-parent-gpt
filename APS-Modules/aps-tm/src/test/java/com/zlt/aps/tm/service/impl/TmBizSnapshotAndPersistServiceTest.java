package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 胎面自动排程快照落库服务测试。
 *
 * <p>验证落库失败时中断本次写入，并保证解释表能关联已插入的排程结果。</p>
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
     * 测试内容：验证结果表写入失败时直接抛出业务异常。
     * 测试场景：结果表 mapper 抛出运行时异常，解释表 mapper 正常写入。
     * 预期结果：抛出 ServiceException，错误信息包含订单号和失败原因，解释表不继续写入。
     */
    @Test(expected = ServiceException.class)
    public void snapshotAndPersistShouldThrowWhenResultInsertError() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = new TmScheduleResult();
        result.setOrderNo(task.getOrderNo());
        result.setTreadCode(task.getTreadCode());

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertUnplanned(task, context)).thenReturn(result);
        when(scheduleResultMapper.insert(result)).thenThrow(new RuntimeException("结果写入失败"));

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);
    }

    /**
     * 测试内容：验证解释表写入失败时直接抛出业务异常。
     * 测试场景：结果表 mapper 正常写入，解释表 mapper 抛出运行时异常。
     * 预期结果：抛出 ServiceException，错误信息包含解释表和订单号。
     */
    @Test(expected = ServiceException.class)
    public void snapshotAndPersistShouldThrowWhenExplainInsertError() {
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
        when(scheduleResultMapper.insert(result)).thenAnswer(invocation -> {
            result.setId(301L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class)))
                .thenThrow(new RuntimeException("解释写入失败"));

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);
    }

    /**
     * 测试内容：验证未排任务解释表写入前会回填结果表主键。
     * 测试场景：未排任务先转换并插入 T_TM_SCHEDULE_RESULT，Mapper 模拟回填 result.id。
     * 预期结果：写入 T_TM_SCHEDULE_RESULT_EXPLAIN 时 resultId 等于结果表回填主键。
     */
    @Test
    public void snapshotAndPersistShouldFillExplainResultIdForUnplannedTask() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = buildResult(task);
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertUnplanned(task, context)).thenReturn(result);
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultMapper.insert(result)).thenAnswer(invocation -> {
            result.setId(101L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResultExplain> explainCaptor = ArgumentCaptor.forClass(TmScheduleResultExplain.class);
        verify(scheduleResultExplainMapper).insert(explainCaptor.capture());
        assertEquals(Long.valueOf(101L), explainCaptor.getValue().getResultId());
    }

    /**
     * 测试内容：验证已分配任务解释表写入前会回填结果表主键。
     * 测试场景：任务已进入机台任务链，由 convertChainToResult 生成结果并插入。
     * 预期结果：解释表 resultId 使用同一业务键对应的结果表主键。
     */
    @Test
    public void snapshotAndPersistShouldFillExplainResultIdForAssignedTask() {
        TmTaskDraft task = buildAssignedTask();
        TmScheduleContext context = buildAssignedContext(task);
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = buildResult(task);
        result.setMachineCode("TM-01");
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertChainToResult(any(), any())).thenReturn(Collections.singletonList(result));
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultMapper.insert(result)).thenAnswer(invocation -> {
            result.setId(202L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        TmPersistResult persistResult = context.getPersistResult();
        assertEquals(null, persistResult);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResultExplain> explainCaptor = ArgumentCaptor.forClass(TmScheduleResultExplain.class);
        verify(scheduleResultExplainMapper).insert(explainCaptor.capture());
        assertEquals(Long.valueOf(202L), explainCaptor.getValue().getResultId());
    }

    /**
     * 测试内容：验证同一机台同一胎面结果会横向合并，并生成胎面专用工单号。
     * 测试场景：两个来源成型工单对应同一胎面、同一机台的 1 班和 2 班任务。
     * 预期结果：结果表只插入一行，orderNo 为 batchNo-序号，不包含来源成型工单号，解释表仍能关联同一结果。
     */
    @Test
    public void snapshotAndPersistShouldMergeSameTreadRowsAndGenerateTmOrderNo() {
        TmTaskDraft firstTask = buildTask();
        firstTask.setOrderNo("CX-ORD-001");
        firstTask.setSourceOrderNos("CX-ORD-001");
        firstTask.setShiftOrder(1);
        firstTask.setPlanQty(new BigDecimal("100"));
        TmTaskDraft secondTask = buildTask();
        secondTask.setOrderNo("CX-ORD-002");
        secondTask.setSourceOrderNos("CX-ORD-002");
        secondTask.setShiftOrder(2);
        secondTask.setPlanQty(new BigDecimal("200"));
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TM20260625143025123");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 6, 25)));
        context.setTaskDraftList(Arrays.asList(firstTask, secondTask));

        TmSnapshotBuildResult firstSnapshot = new TmSnapshotBuildResult();
        TmSnapshotBuildResult secondSnapshot = new TmSnapshotBuildResult();
        TmScheduleResult firstResult = buildResult(firstTask);
        firstResult.setFactoryCode(context.getFactoryCode());
        firstResult.setBatchNo(context.getBatchNo());
        firstResult.setScheduleDate(context.getScheduleDate());
        firstResult.setClass1PlanQty(new BigDecimal("100"));
        TmScheduleResult secondResult = buildResult(secondTask);
        secondResult.setFactoryCode(context.getFactoryCode());
        secondResult.setBatchNo(context.getBatchNo());
        secondResult.setScheduleDate(context.getScheduleDate());
        secondResult.setClass2PlanQty(new BigDecimal("200"));

        when(snapshotBuildService.buildTaskExplain(firstTask, context)).thenReturn(firstSnapshot);
        when(snapshotBuildService.buildTaskExplain(secondTask, context)).thenReturn(secondSnapshot);
        when(persistService.convertUnplanned(firstTask, context)).thenReturn(firstResult);
        when(persistService.convertUnplanned(secondTask, context)).thenReturn(secondResult);
        when(persistService.convertExplain(firstTask, firstSnapshot, context)).thenReturn(buildExplain(firstTask));
        when(persistService.convertExplain(secondTask, secondSnapshot, context)).thenReturn(buildExplain(secondTask));
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            TmScheduleResult result = invocation.getArgument(0);
            result.setId(501L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper, times(1)).insert(resultCaptor.capture());
        TmScheduleResult mergedResult = resultCaptor.getValue();
        assertEquals("TM20260625143025123-0001", mergedResult.getOrderNo());
        assertEquals(false, mergedResult.getOrderNo().contains("CX-ORD"));
        assertEquals(new BigDecimal("100"), mergedResult.getClass1PlanQty());
        assertEquals(new BigDecimal("200"), mergedResult.getClass2PlanQty());

        ArgumentCaptor<TmScheduleResultExplain> explainCaptor = ArgumentCaptor.forClass(TmScheduleResultExplain.class);
        verify(scheduleResultExplainMapper, times(2)).insert(explainCaptor.capture());
        List<TmScheduleResultExplain> explainList = explainCaptor.getAllValues();
        assertEquals(Long.valueOf(501L), explainList.get(0).getResultId());
        assertEquals(Long.valueOf(501L), explainList.get(1).getResultId());
        assertEquals(firstTask.getBusinessKey(), explainList.get(0).getTaskBusinessKey());
        assertEquals("CX-ORD-001", explainList.get(0).getTaskOrderNo());
        assertEquals("CX-ORD-001", explainList.get(0).getSourceOrderNos());
        assertEquals(Integer.valueOf(1), explainList.get(0).getShiftOrder());
        assertEquals(secondTask.getBusinessKey(), explainList.get(1).getTaskBusinessKey());
        assertEquals("CX-ORD-002", explainList.get(1).getTaskOrderNo());
        assertEquals("CX-ORD-002", explainList.get(1).getSourceOrderNos());
        assertEquals(Integer.valueOf(2), explainList.get(1).getShiftOrder());
    }

    /**
     * 测试内容：验证解释表写入前找不到结果表主键时中断落库。
     * 测试场景：结果表 insert 返回成功但未回填 id。
     * 预期结果：抛出 ServiceException，避免写入 resultId 为空的解释记录。
     */
    @Test(expected = ServiceException.class)
    public void snapshotAndPersistShouldThrowWhenResultIdMissingBeforeExplainInsert() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = buildResult(task);
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertUnplanned(task, context)).thenReturn(result);
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultMapper.insert(result)).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper).snapshotAndPersist(context);
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
        task.setGlueCode("GL-001");
        task.setMouthPlateCode("MP-001");
        task.setPlanQty(BigDecimal.TEN);
        task.setUnplannedReasonCode("NO_MACHINE");
        return task;
    }

    /**
     * 构造已分配任务草稿。
     *
     * @return 已分配任务草稿
     */
    private TmTaskDraft buildAssignedTask() {
        TmTaskDraft task = buildTask();
        task.setUnplannedReasonCode(null);
        task.setMachineCode("TM-01");
        return task;
    }

    /**
     * 构造排程结果实体。
     *
     * @param task 任务草稿
     * @return 排程结果实体
     */
    private TmScheduleResult buildResult(TmTaskDraft task) {
        TmScheduleResult result = new TmScheduleResult();
        result.setOrderNo(task.getOrderNo());
        result.setTreadCode(task.getTreadCode());
        result.setGlueCode(task.getGlueCode());
        result.setMouthPlateCode(task.getMouthPlateCode());
        return result;
    }

    /**
     * 构造任务级解释记录。
     *
     * @param task 任务草稿
     * @return 带任务定位字段的解释记录
     */
    private TmScheduleResultExplain buildExplain(TmTaskDraft task) {
        TmScheduleResultExplain explain = new TmScheduleResultExplain();
        explain.setTaskBusinessKey(task.getBusinessKey());
        explain.setTaskOrderNo(task.getOrderNo());
        explain.setSourceOrderNos(task.getSourceOrderNos());
        explain.setShiftOrder(task.getShiftOrder());
        return explain;
    }

    /**
     * 构造包含已分配任务链的排程上下文。
     *
     * @param task 已分配任务草稿
     * @return 排程上下文
     */
    private TmScheduleContext buildAssignedContext(TmTaskDraft task) {
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate("TM-01", LocalDate.of(2026, 6, 23), 1);
        chain.append(new ScheduleTaskNode<>(task.getBusinessKey(), task, "TM-01", LocalDate.of(2026, 6, 23),
                "CLASS1", 1, BigDecimal.TEN), new ScheduleOperationContext("tester", "append", "TRACE-1"));
        return context;
    }
}
