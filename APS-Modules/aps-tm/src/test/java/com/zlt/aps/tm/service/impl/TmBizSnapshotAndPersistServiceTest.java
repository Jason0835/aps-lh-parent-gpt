package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.impl.TmPersistService;
import com.zlt.aps.tm.engine.service.impl.TmSnapshotBuildService;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
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

    @Mock
    private TmScheduleUnplannedMapper scheduleUnplannedMapper;

    /**
     * 测试内容：验证快照落库服务拒绝空上下文。
     * 测试场景：直接调用 snapshotAndPersist(null)。
     * 预期结果：抛出 IllegalArgumentException，避免后续读取任务列表时报空指针。
     */
    @Test(expected = IllegalArgumentException.class)
    public void snapshotAndPersistShouldRejectNullContext() {
        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(null);
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
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

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
        TmTaskDraft task = buildAssignedTask();
        TmScheduleContext context = buildAssignedContext(task);
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = new TmScheduleResult();
        result.setOrderNo(task.getOrderNo());
        result.setTreadCode(task.getTreadCode());

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertChainToResult(any(), any())).thenReturn(Collections.singletonList(result));
        when(scheduleResultMapper.insert(result)).thenThrow(new RuntimeException("结果写入失败"));

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);
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
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class)))
                .thenThrow(new RuntimeException("解释写入失败"));

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);
    }

    /**
     * 测试内容：验证未排任务解释表不再强依赖结果表主键。
     * 测试场景：未排任务写入 T_TM_SCHEDULE_UNPLANNED，解释表继续保留任务追溯。
     * 预期结果：写入 T_TM_SCHEDULE_RESULT_EXPLAIN 时 resultId 为空。
     */
    @Test
    public void snapshotAndPersistShouldKeepExplainResultIdNullForUnplannedTask() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleUnplannedMapper.insert(any(TmScheduleUnplanned.class))).thenReturn(1);
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResultExplain> explainCaptor = ArgumentCaptor.forClass(TmScheduleResultExplain.class);
        verify(scheduleResultExplainMapper).insert(explainCaptor.capture());
        verify(scheduleResultMapper, never()).insert(any(TmScheduleResult.class));
        verify(scheduleUnplannedMapper).insert(any(TmScheduleUnplanned.class));
        assertEquals(null, explainCaptor.getValue().getResultId());
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
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

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
        appendAssignedTask(context, firstTask, "TM001", 1, new BigDecimal("100"));
        appendAssignedTask(context, secondTask, "TM001", 2, new BigDecimal("200"));

        TmSnapshotBuildResult firstSnapshot = new TmSnapshotBuildResult();
        TmSnapshotBuildResult secondSnapshot = new TmSnapshotBuildResult();
        TmScheduleResult firstResult = buildResult(firstTask);
        firstResult.setFactoryCode(context.getFactoryCode());
        firstResult.setBatchNo(context.getBatchNo());
        firstResult.setScheduleDate(context.getScheduleDate());
        firstResult.setMachineCode("TM001");
        firstResult.setClass1PlanQty(new BigDecimal("100"));
        TmScheduleResult secondResult = buildResult(secondTask);
        secondResult.setFactoryCode(context.getFactoryCode());
        secondResult.setBatchNo(context.getBatchNo());
        secondResult.setScheduleDate(context.getScheduleDate());
        secondResult.setMachineCode("TM001");
        secondResult.setClass2PlanQty(new BigDecimal("200"));

        when(snapshotBuildService.buildTaskExplain(firstTask, context)).thenReturn(firstSnapshot);
        when(snapshotBuildService.buildTaskExplain(secondTask, context)).thenReturn(secondSnapshot);
        when(persistService.convertChainToResult(any(), any()))
                .thenReturn(Collections.singletonList(firstResult))
                .thenReturn(Collections.singletonList(secondResult));
        when(persistService.convertExplain(firstTask, firstSnapshot, context)).thenReturn(buildExplain(firstTask));
        when(persistService.convertExplain(secondTask, secondSnapshot, context)).thenReturn(buildExplain(secondTask));
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            TmScheduleResult result = invocation.getArgument(0);
            result.setId(501L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

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
     * 测试内容：验证产能顺延任务进入下一班后，仍按同机台同胎面结果行归并计划量。
     * 测试场景：顺延任务和已有任务都落在 TM001 的 2 班，胎面规格相同但来源业务键不同。
     * 预期结果：结果表只插入一行，class2_plan_qty 等于两条任务计划量之和。
     */
    @Test
    public void snapshotAndPersistShouldMergeOverflowTaskWithExistingNextShiftTreadRow() {
        TmTaskDraft overflowTask = buildTask();
        overflowTask.setOrderNo("CX-ORD-OVERFLOW");
        overflowTask.setSourceOrderNos("CX-ORD-OVERFLOW");
        overflowTask.setShiftOrder(2);
        overflowTask.setPlanQty(new BigDecimal("3844"));
        TmTaskDraft existingNextShiftTask = buildTask();
        existingNextShiftTask.setOrderNo("CX-ORD-NEXT");
        existingNextShiftTask.setSourceOrderNos("CX-ORD-NEXT");
        existingNextShiftTask.setShiftOrder(2);
        existingNextShiftTask.setMouthPlateCode("MP-002");
        existingNextShiftTask.setPlanQty(new BigDecimal("500"));
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TM20260625143025123");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 6, 25)));
        context.setTaskDraftList(Arrays.asList(overflowTask, existingNextShiftTask));
        appendAssignedTask(context, overflowTask, "TM001", 2, new BigDecimal("3844"));
        appendAssignedTask(context, existingNextShiftTask, "TM001", 2, new BigDecimal("500"));

        TmScheduleResult overflowResult = buildResult(overflowTask);
        overflowResult.setFactoryCode(context.getFactoryCode());
        overflowResult.setBatchNo(context.getBatchNo());
        overflowResult.setScheduleDate(context.getScheduleDate());
        overflowResult.setMachineCode("TM001");
        overflowResult.setClass2PlanQty(new BigDecimal("3844"));
        TmScheduleResult existingResult = buildResult(existingNextShiftTask);
        existingResult.setFactoryCode(context.getFactoryCode());
        existingResult.setBatchNo(context.getBatchNo());
        existingResult.setScheduleDate(context.getScheduleDate());
        existingResult.setMachineCode("TM001");
        existingResult.setClass2PlanQty(new BigDecimal("500"));

        when(snapshotBuildService.buildTaskExplain(eq(overflowTask), eq(context))).thenReturn(new TmSnapshotBuildResult());
        when(snapshotBuildService.buildTaskExplain(eq(existingNextShiftTask), eq(context))).thenReturn(new TmSnapshotBuildResult());
        when(persistService.convertChainToResult(any(), any()))
                .thenReturn(Arrays.asList(overflowResult, existingResult));
        when(persistService.convertExplain(eq(overflowTask), any(), eq(context))).thenReturn(buildExplain(overflowTask));
        when(persistService.convertExplain(eq(existingNextShiftTask), any(), eq(context))).thenReturn(buildExplain(existingNextShiftTask));
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            TmScheduleResult result = invocation.getArgument(0);
            result.setId(601L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper, times(1)).insert(resultCaptor.capture());
        TmScheduleResult mergedResult = resultCaptor.getValue();
        assertEquals("TM001", mergedResult.getMachineCode());
        assertEquals(overflowTask.getTreadCode(), mergedResult.getTreadCode());
        assertEquals(new BigDecimal("4344"), mergedResult.getClass2PlanQty());
    }

    /**
     * 测试内容：验证同胎面拆分到不同机台后，不会被结果落库逻辑合并成同一行。
     * 测试场景：同一胎面同一班分别落到 TM001 和 TM002。
     * 预期结果：结果表插入两行，保持不同机台各自的班次计划量。
     */
    @Test
    public void snapshotAndPersistShouldNotMergeSameTreadRowsAcrossDifferentMachines() {
        TmTaskDraft tm001Task = buildTask();
        tm001Task.setOrderNo("CX-ORD-TM001");
        tm001Task.setSourceOrderNos("CX-ORD-TM001");
        tm001Task.setShiftOrder(1);
        tm001Task.setPlanQty(new BigDecimal("2300"));
        TmTaskDraft tm002Task = buildTask();
        tm002Task.setOrderNo("CX-ORD-TM002");
        tm002Task.setSourceOrderNos("CX-ORD-TM002");
        tm002Task.setShiftOrder(1);
        tm002Task.setPlanQty(new BigDecimal("3844"));
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TM20260625143025123");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 6, 25)));
        context.setTaskDraftList(Arrays.asList(tm001Task, tm002Task));
        appendAssignedTask(context, tm001Task, "TM001", 1, new BigDecimal("2300"));
        appendAssignedTask(context, tm002Task, "TM002", 1, new BigDecimal("3844"));

        TmScheduleResult tm001Result = buildResult(tm001Task);
        tm001Result.setFactoryCode(context.getFactoryCode());
        tm001Result.setBatchNo(context.getBatchNo());
        tm001Result.setScheduleDate(context.getScheduleDate());
        tm001Result.setMachineCode("TM001");
        tm001Result.setClass1PlanQty(new BigDecimal("2300"));
        TmScheduleResult tm002Result = buildResult(tm002Task);
        tm002Result.setFactoryCode(context.getFactoryCode());
        tm002Result.setBatchNo(context.getBatchNo());
        tm002Result.setScheduleDate(context.getScheduleDate());
        tm002Result.setMachineCode("TM002");
        tm002Result.setClass1PlanQty(new BigDecimal("3844"));

        when(snapshotBuildService.buildTaskExplain(eq(tm001Task), eq(context))).thenReturn(new TmSnapshotBuildResult());
        when(snapshotBuildService.buildTaskExplain(eq(tm002Task), eq(context))).thenReturn(new TmSnapshotBuildResult());
        when(persistService.convertChainToResult(any(), any()))
                .thenReturn(Collections.singletonList(tm001Result))
                .thenReturn(Collections.singletonList(tm002Result));
        when(persistService.convertExplain(eq(tm001Task), any(), eq(context))).thenReturn(buildExplain(tm001Task));
        when(persistService.convertExplain(eq(tm002Task), any(), eq(context))).thenReturn(buildExplain(tm002Task));
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            TmScheduleResult result = invocation.getArgument(0);
            result.setId("TM001".equals(result.getMachineCode()) ? 701L : 702L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper, times(2)).insert(resultCaptor.capture());
        List<TmScheduleResult> resultList = resultCaptor.getAllValues();
        assertEquals("TM001", resultList.get(0).getMachineCode());
        assertEquals(new BigDecimal("2300"), resultList.get(0).getClass1PlanQty());
        assertEquals("TM002", resultList.get(1).getMachineCode());
        assertEquals(new BigDecimal("3844"), resultList.get(1).getClass1PlanQty());
    }
    /**
     * 测试内容：验证未排任务只写未排表，不写排程结果表。
     * 测试场景：任务没有可用机台，快照中存在未排证据。
     * 预期结果：未排表写入一条，结果表不写入，解释表保留未排任务且 resultId 为空。
     */
    @Test
    public void snapshotAndPersistShouldPersistUnplannedTaskWithoutScheduleResult() {
        TmTaskDraft task = buildTask();
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TM20260625143025123");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 6, 25)));
        context.setTaskDraftList(Collections.singletonList(task));
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setUnplannedEvidenceJson("{\"reason\":\"NO_MACHINE\"}");
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleUnplannedMapper.insert(any(TmScheduleUnplanned.class))).thenAnswer(invocation -> {
            TmScheduleUnplanned unplanned = invocation.getArgument(0);
            unplanned.setId(801L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        verify(persistService, never()).convertUnplanned(any(), any());
        verify(scheduleResultMapper, never()).insert(any(TmScheduleResult.class));
        ArgumentCaptor<TmScheduleUnplanned> unplannedCaptor = ArgumentCaptor.forClass(TmScheduleUnplanned.class);
        verify(scheduleUnplannedMapper).insert(unplannedCaptor.capture());
        TmScheduleUnplanned unplanned = unplannedCaptor.getValue();
        assertEquals("116", unplanned.getFactoryCode());
        assertEquals("TM20260625143025123", unplanned.getBatchNo());
        assertEquals(context.getScheduleDate(), unplanned.getScheduleDate());
        assertEquals(task.getTreadCode(), unplanned.getTreadCode());
        assertEquals(task.getGlueCode(), unplanned.getGlueCode());
        assertEquals(task.getMouthPlateCode(), unplanned.getMouthPlateCode());
        assertEquals(task.getUnplannedReasonCode(), unplanned.getUnplannedReasonCode());
        assertEquals("{\"reason\":\"NO_MACHINE\"}", unplanned.getUnplannedEvidenceJson());

        ArgumentCaptor<TmScheduleResultExplain> explainCaptor = ArgumentCaptor.forClass(TmScheduleResultExplain.class);
        verify(scheduleResultExplainMapper).insert(explainCaptor.capture());
        assertEquals(null, explainCaptor.getValue().getResultId());
        assertEquals(0, context.getPersistResult().getResultCount());
        assertEquals(1, context.getPersistResult().getUnplannedCount());
        assertEquals(1, context.getPersistResult().getExplainCount());
    }

    /**
     * 测试内容：验证已排结果中计划量为 0 的班次不会保留顺序字段。
     * 测试场景：任务已分配机台，但转换出的 1 班计划量为 0 且带有顺序。
     * 预期结果：结果表插入前清空 1 班顺序和起止时间，避免看板展示空计划顺序。
     */
    @Test
    public void snapshotAndPersistShouldClearSequenceWhenAssignedShiftPlanQtyIsZero() {
        TmTaskDraft task = buildAssignedTask();
        TmScheduleContext context = buildAssignedContext(task);
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        TmScheduleResult result = buildResult(task);
        result.setFactoryCode("116");
        result.setBatchNo("TM20260625143025123");
        result.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 6, 25)));
        result.setMachineCode("TM-01");
        result.setClass1PlanQty(BigDecimal.ZERO);
        result.setClass1Sequence(1);
        result.setClass1StartTime(new java.util.Date());
        result.setClass1EndTime(new java.util.Date());
        TmScheduleResultExplain explain = new TmScheduleResultExplain();

        when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(snapshot);
        when(persistService.convertChainToResult(any(), any())).thenReturn(Collections.singletonList(result));
        when(persistService.convertExplain(task, snapshot, context)).thenReturn(explain);
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            TmScheduleResult inserted = invocation.getArgument(0);
            inserted.setId(901L);
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper).insert(resultCaptor.capture());
        TmScheduleResult insertedResult = resultCaptor.getValue();
        assertEquals(null, insertedResult.getClass1Sequence());
        assertEquals(null, insertedResult.getClass1StartTime());
        assertEquals(null, insertedResult.getClass1EndTime());
    }

    /**
     * 测试内容：验证结果表可见行按机台和班次重新生成连续顺序。
     * 测试场景：同一机台 1 班可见结果的原始顺序为 2、3、6、10。
     * 预期结果：结果表插入前重排为 1、2、3、4，解释表仍按原任务业务键追溯。
     */
    @Test
    public void snapshotAndPersistShouldResequenceVisibleRowsByMachineAndShift() {
        TmTaskDraft firstTask = buildAssignedTask("TR-001", 1);
        TmTaskDraft secondTask = buildAssignedTask("TR-002", 1);
        TmTaskDraft thirdTask = buildAssignedTask("TR-003", 1);
        TmTaskDraft fourthTask = buildAssignedTask("TR-004", 1);
        TmScheduleContext context = buildVisibleSequenceContext(Arrays.asList(firstTask, secondTask, thirdTask, fourthTask));
        List<TmScheduleResult> resultList = Arrays.asList(
                buildClass1Result(context, firstTask, 2, "100"),
                buildClass1Result(context, secondTask, 3, "100"),
                buildClass1Result(context, thirdTask, 6, "100"),
                buildClass1Result(context, fourthTask, 10, "100"));

        mockVisibleSequencePersist(context, Arrays.asList(firstTask, secondTask, thirdTask, fourthTask), resultList);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper, times(4)).insert(resultCaptor.capture());
        List<TmScheduleResult> insertedList = resultCaptor.getAllValues();
        assertEquals(Integer.valueOf(1), insertedList.get(0).getClass1Sequence());
        assertEquals(Integer.valueOf(2), insertedList.get(1).getClass1Sequence());
        assertEquals(Integer.valueOf(3), insertedList.get(2).getClass1Sequence());
        assertEquals(Integer.valueOf(4), insertedList.get(3).getClass1Sequence());
    }

    /**
     * 测试内容：验证无计划量的结果行不参与可见顺位重排。
     * 测试场景：同一机台 1 班存在 sequence=1 但计划量为 0 的行，以及 sequence=2、3 的可见行。
     * 预期结果：0 计划量行顺序清空，可见行重排为 1、2。
     */
    @Test
    public void snapshotAndPersistShouldSkipZeroPlanQtyRowsWhenResequencingVisibleRows() {
        TmTaskDraft zeroTask = buildAssignedTask("TR-ZERO", 1);
        TmTaskDraft firstVisibleTask = buildAssignedTask("TR-001", 1);
        TmTaskDraft secondVisibleTask = buildAssignedTask("TR-002", 1);
        TmScheduleContext context = buildVisibleSequenceContext(Arrays.asList(zeroTask, firstVisibleTask, secondVisibleTask));
        List<TmScheduleResult> resultList = Arrays.asList(
                buildClass1Result(context, zeroTask, 1, "0"),
                buildClass1Result(context, firstVisibleTask, 2, "100"),
                buildClass1Result(context, secondVisibleTask, 3, "100"));

        mockVisibleSequencePersist(context, Arrays.asList(zeroTask, firstVisibleTask, secondVisibleTask), resultList);

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper, times(3)).insert(resultCaptor.capture());
        List<TmScheduleResult> insertedList = resultCaptor.getAllValues();
        assertEquals(null, insertedList.get(0).getClass1Sequence());
        assertEquals(Integer.valueOf(1), insertedList.get(1).getClass1Sequence());
        assertEquals(Integer.valueOf(2), insertedList.get(2).getClass1Sequence());
    }

    /**
     * 测试内容：验证可见顺位按班次和机台分别重排。
     * 测试场景：TM001 的 1 班、2 班以及 TM002 的 1 班都存在非连续原始顺序。
     * 预期结果：每个机台和班次组合都从 1 独立编号，互不影响。
     */
    @Test
    public void snapshotAndPersistShouldResequenceVisibleRowsIndependentlyByMachineAndShift() {
        TmTaskDraft tm001Class1Task = buildAssignedTask("TR-001", 1);
        TmTaskDraft tm001Class2Task = buildAssignedTask("TR-002", 2);
        TmTaskDraft tm002Class1Task = buildAssignedTask("TR-003", 1);
        TmScheduleContext context = buildVisibleSequenceContext(Arrays.asList(tm001Class1Task, tm001Class2Task, tm002Class1Task));
        List<TmScheduleResult> resultList = Arrays.asList(
                buildClass1Result(context, tm001Class1Task, 5, "100"),
                buildClass2Result(context, tm001Class2Task, 8, "100"),
                buildClass1Result(context, tm002Class1Task, 7, "100"));
        resultList.get(2).setMachineCode("TM002");

        mockVisibleSequencePersist(context, Arrays.asList(tm001Class1Task, tm001Class2Task, tm002Class1Task), resultList);
        when(persistService.convertChainToResult(any(), eq(context)))
                .thenReturn(Arrays.asList(resultList.get(0), resultList.get(2)))
                .thenReturn(Collections.singletonList(resultList.get(1)));

        new TmBizSnapshotAndPersistService(snapshotBuildService, persistService, scheduleResultMapper,
                scheduleResultExplainMapper, scheduleUnplannedMapper).snapshotAndPersist(context);

        ArgumentCaptor<TmScheduleResult> resultCaptor = ArgumentCaptor.forClass(TmScheduleResult.class);
        verify(scheduleResultMapper, times(3)).insert(resultCaptor.capture());
        List<TmScheduleResult> insertedList = resultCaptor.getAllValues();
        TmScheduleResult tm001Class1Result = findResult(insertedList, "TM001", "TR-001");
        TmScheduleResult tm001Class2Result = findResult(insertedList, "TM001", "TR-002");
        TmScheduleResult tm002Class1Result = findResult(insertedList, "TM002", "TR-003");
        assertEquals(Integer.valueOf(1), tm001Class1Result.getClass1Sequence());
        assertEquals(Integer.valueOf(1), tm001Class2Result.getClass2Sequence());
        assertEquals(Integer.valueOf(1), tm002Class1Result.getClass1Sequence());
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
     * 构造指定胎面和班次的已分配任务草稿。
     *
     * @param treadCode 胎面编码
     * @param shiftOrder 班次顺序
     * @return 已分配任务草稿
     */
    private TmTaskDraft buildAssignedTask(String treadCode, Integer shiftOrder) {
        TmTaskDraft task = buildAssignedTask();
        task.setOrderNo("ORD-" + treadCode + "-" + shiftOrder);
        task.setTreadCode(treadCode);
        task.setShiftOrder(shiftOrder);
        task.setPlanQty(new BigDecimal("100"));
        task.setSourceOrderNos(task.getOrderNo());
        return task;
    }

    /**
     * 构造可见顺位测试上下文，并创建一条任务链用于关联结果和解释。
     *
     * @param taskList 已分配任务列表
     * @return 排程上下文
     */
    private TmScheduleContext buildVisibleSequenceContext(List<TmTaskDraft> taskList) {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("116");
        context.setBatchNo("TM20260625143025123");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 6, 25)));
        context.setTaskDraftList(taskList);
        for (TmTaskDraft task : taskList) {
            appendAssignedTask(context, task, "TM001", task.getShiftOrder(), task.getPlanQty());
        }
        return context;
    }

    /**
     * 模拟可见顺位测试所需的快照、结果转换和解释转换。
     *
     * @param context    排程上下文
     * @param taskList   任务列表
     * @param resultList 转换后的结果列表
     */
    private void mockVisibleSequencePersist(TmScheduleContext context, List<TmTaskDraft> taskList,
                                            List<TmScheduleResult> resultList) {
        for (TmTaskDraft task : taskList) {
            when(snapshotBuildService.buildTaskExplain(task, context)).thenReturn(new TmSnapshotBuildResult());
            when(persistService.convertExplain(eq(task), any(), eq(context))).thenReturn(buildExplain(task));
        }
        when(persistService.convertChainToResult(any(), eq(context))).thenReturn(resultList);
        when(scheduleResultMapper.insert(any(TmScheduleResult.class))).thenAnswer(invocation -> {
            TmScheduleResult result = invocation.getArgument(0);
            result.setId(1000L + result.getOrderNo().hashCode());
            return 1;
        });
        when(scheduleResultExplainMapper.insert(any(TmScheduleResultExplain.class))).thenReturn(1);
    }

    /**
     * 构造 1 班排程结果。
     *
     * @param context  排程上下文
     * @param task     任务草稿
     * @param sequence 原始班内顺序
     * @param planQty  计划量
     * @return 排程结果
     */
    private TmScheduleResult buildClass1Result(TmScheduleContext context, TmTaskDraft task,
                                               Integer sequence, String planQty) {
        TmScheduleResult result = buildVisibleSequenceResult(context, task);
        result.setClass1Sequence(sequence);
        result.setClass1PlanQty(new BigDecimal(planQty));
        return result;
    }

    /**
     * 构造 2 班排程结果。
     *
     * @param context  排程上下文
     * @param task     任务草稿
     * @param sequence 原始班内顺序
     * @param planQty  计划量
     * @return 排程结果
     */
    private TmScheduleResult buildClass2Result(TmScheduleContext context, TmTaskDraft task,
                                               Integer sequence, String planQty) {
        TmScheduleResult result = buildVisibleSequenceResult(context, task);
        result.setClass2Sequence(sequence);
        result.setClass2PlanQty(new BigDecimal(planQty));
        return result;
    }

    /**
     * 构造可见顺位测试的排程结果基础字段。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 排程结果
     */
    private TmScheduleResult buildVisibleSequenceResult(TmScheduleContext context, TmTaskDraft task) {
        TmScheduleResult result = buildResult(task);
        result.setFactoryCode(context.getFactoryCode());
        result.setBatchNo(context.getBatchNo());
        result.setScheduleDate(context.getScheduleDate());
        result.setMachineCode("TM001");
        return result;
    }

    /**
     * 按机台和胎面查找排程结果。
     *
     * @param resultList  结果列表
     * @param machineCode 机台编码
     * @param treadCode   胎面编码
     * @return 匹配的排程结果
     */
    private TmScheduleResult findResult(List<TmScheduleResult> resultList, String machineCode, String treadCode) {
        for (TmScheduleResult result : resultList) {
            if (machineCode.equals(result.getMachineCode()) && treadCode.equals(result.getTreadCode())) {
                return result;
            }
        }
        throw new AssertionError("未找到排程结果，machineCode=" + machineCode + ", treadCode=" + treadCode);
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
     * 将任务追加到已分配任务链。
     *
     * @param context    排程上下文
     * @param task       任务草稿
     * @param machineCode 机台编码
     * @param shiftOrder 班次顺序
     * @param planQty    计划量
     */
    private void appendAssignedTask(TmScheduleContext context, TmTaskDraft task, String machineCode,
                                    Integer shiftOrder, BigDecimal planQty) {
        task.setUnplannedReasonCode(null);
        task.setMachineCode(machineCode);
        task.setShiftOrder(shiftOrder);
        LocalDate scheduleDate = context.getScheduleDate() instanceof java.sql.Date
                ? ((java.sql.Date) context.getScheduleDate()).toLocalDate()
                : DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate();
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup().getOrCreate(machineCode, scheduleDate, shiftOrder);
        int sequence = chain.toList().size() + 1;
        chain.append(new ScheduleTaskNode<>(task.getBusinessKey(), task, machineCode, scheduleDate,
                        "CLASS" + shiftOrder, sequence, planQty),
                new ScheduleOperationContext("tester", "append", "TRACE-1"));
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

