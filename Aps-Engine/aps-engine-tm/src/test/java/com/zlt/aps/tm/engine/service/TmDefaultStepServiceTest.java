package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.impl.*;
import com.zlt.aps.tm.engine.strategy.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * 胎面默认步骤服务测试。
 *
 * <p>验证第16章模板注入所需的默认步骤服务可运行，同时不写死第15章未确认业务算法。</p>
 */
public class TmDefaultStepServiceTest {

    /**
     * 测试内容：验证初始化步骤会补齐批次号和追踪号。
     * 测试场景：排程上下文已有工厂、日期和操作人，但 batchNo、traceId 为空。
     * 预期结果：初始化后批次号和追踪号非空，批次号以胎面前缀 TM 开头。
     */
    @Test
    public void bootstrapShouldFillBatchNoAndTraceIdWhenMissing() {
        TmScheduleContext context = buildContext();
        context.setBatchNo(null);
        context.setTraceId(null);

        new TmPlanBootstrapService().bootstrap(context);

        assertTrue(StrUtil.isNotBlank(context.getBatchNo()));
        assertTrue(StrUtil.isNotBlank(context.getTraceId()));
        assertTrue(context.getBatchNo().startsWith("TM"));
    }

    /**
     * 测试内容：验证初始化步骤拒绝缺少排程日期的上下文。
     * 测试场景：上下文工厂和操作人存在，scheduleDate 为空。
     * 预期结果：抛出 ServiceException，不生成批次号。
     */
    @Test(expected = ServiceException.class)
    public void bootstrapShouldRejectMissingScheduleDate() {
        TmScheduleContext context = buildContext();
        context.setScheduleDate(null);

        new TmPlanBootstrapService().bootstrap(context);
    }

    /**
     * 测试内容：验证默认排序步骤按业务键稳定排序。
     * 测试场景：任务列表输入顺序为 ORD-2、ORD-1。
     * 预期结果：排序后变为 ORD-1、ORD-2，任务自身内容不被改写。
     */
    @Test
    public void taskSortShouldSortByBusinessKeyWithoutChangingTaskContent() {
        TmScheduleContext context = buildContext();
        TmTaskDraft taskB = buildTask("ORD-2", "TM01");
        TmTaskDraft taskA = buildTask("ORD-1", "TM01");
        context.setTaskDraftList(Arrays.asList(taskB, taskA));

        new TmTaskSortService(buildRegistry()).sort(context);

        assertEquals("ORD-1", context.getTaskDraftList().get(0).getOrderNo());
        assertEquals("ORD-2", context.getTaskDraftList().get(1).getOrderNo());
    }

    /**
     * 测试内容：验证机台分配步骤处理预设机台和空机台任务。
     * 测试场景：一条任务已有机台 TM01，另一条任务机台为空且无候选机台。
     * 预期结果：预设机台任务追加到 TM01 链，空机台任务标记无可用机台未排。
     */
    @Test
    public void machineAssignShouldAppendPresetMachineTaskAndMarkBlankMachineUnplanned() {
        TmScheduleContext context = buildContext();
        TmTaskDraft assigned = buildTask("ORD-1", "TM01");
        TmTaskDraft unassigned = buildTask("ORD-2", null);
        context.setTaskDraftList(Arrays.asList(assigned, unassigned));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain("TM01", 1);
        assertNotNull(chain);
        assertEquals(1, chain.getSize());
        assertEquals("ORD-1", chain.toList().get(0).getTask().getOrderNo());
        assertEquals(TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode(), unassigned.getUnplannedReasonCode());
    }

    /**
     * 测试内容：验证默认快照落库步骤会构建解释快照并生成落库汇总。
     * 测试场景：一条已排任务和一条未排任务经过机台分配后进入快照落库。
     * 预期结果：上下文存在快照，结果数和解释数为 2，未排数为 1。
     */
    @Test
    public void snapshotAndPersistShouldBuildSnapshotsAndPersistSummary() {
        TmScheduleContext context = buildContext();
        context.setTaskDraftList(Arrays.asList(buildTask("ORD-1", "TM01"), buildTask("ORD-2", null)));
        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        new TmSnapshotAndPersistService(new TmSnapshotBuildService(), new TmPersistService())
                .snapshotAndPersist(context);

        assertFalse(context.getSnapshotMap().isEmpty());
        assertNotNull(context.getPersistResult());
        assertEquals(2, context.getPersistResult().getResultCount());
        assertEquals(2, context.getPersistResult().getExplainCount());
        assertEquals(1, context.getPersistResult().getUnplannedCount());
    }

    /**
     * 测试内容：验证计划计算步骤调用需求策略并回填需求字段。
     * 测试场景：当前班需求 100，库存保障需求 500，滚动库存 200，计划量为空。
     * 预期结果：库存缺口、最终需求、供应时长和计划量均按默认策略回填。
     */
    @Test
    public void planCalcShouldUseDemandStrategyAndBackfillDemandFields() {
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-1", null);
        task.setCurrentShiftDemandQty(new BigDecimal("100"));
        task.setGuardDemandQty(new BigDecimal("500"));
        task.setRollingStockQty(new BigDecimal("200"));
        task.setGuardRangeHours(new BigDecimal("8"));
        task.setPlanQty(null);
        context.setTaskDraftList(Collections.singletonList(task));

        new TmPlanCalcService(buildRegistry()).calculate(context);

        assertEquals(new BigDecimal("300"), task.getStockGapQty());
        assertEquals(new BigDecimal("300"), task.getDemandQty());
        assertEquals(new BigDecimal("3.20"), task.getSupplyHours());
        assertEquals(new BigDecimal("300"), task.getPlanQty());
    }

    /**
     * 测试内容：验证未预设机台任务会通过候选评分选择最佳机台。
     * 测试场景：任务无机台，两个候选机台产能相同，其中 TM01 链尾主胶与任务主胶一致。
     * 预期结果：任务选择评分更高的 TM01，并被追加到 TM01 一班任务链。
     */
    @Test
    public void machineAssignShouldScoreUnassignedTaskAndChooseBestCandidate() {
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-1", null);
        task.setPlanQty(new BigDecimal("500"));
        task.setGlueCode("GL-A");
        context.setTaskDraftList(Collections.singletonList(task));
        TmMachineCandidate lower = enabledCandidate("TM02", "700");
        lower.setTailMainGlueCode("GL-B");
        TmMachineCandidate higher = enabledCandidate("TM01", "700");
        higher.setTailMainGlueCode("GL-A");
        context.setMachineCandidateList(Arrays.asList(lower, higher));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        assertEquals("TM01", task.getMachineCode());
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain("TM01", 1);
        assertNotNull(chain);
        assertEquals(1, chain.getSize());
    }

    /**
     * 测试内容：验证候选机台评分相同时按机台编码升序稳定选择。
     * 测试场景：两个候选机台得分完全一致，输入顺序为 TM02、TM01。
     * 预期结果：最终选择编码更小的 TM01，保证重复排程结果稳定。
     */
    @Test
    public void machineAssignShouldChooseMachineCodeAscWhenScoreTie() {
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-TIE", null);
        task.setPlanQty(new BigDecimal("500"));
        context.setTaskDraftList(Collections.singletonList(task));
        TmMachineCandidate tm02 = enabledCandidate("TM02", "700");
        TmMachineCandidate tm01 = enabledCandidate("TM01", "700");
        context.setMachineCandidateList(Arrays.asList(tm02, tm01));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        assertEquals("TM01", task.getMachineCode());
        assertNotNull(context.getTaskChain("TM01", 1));
    }

    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F1");
        context.setScheduleDate(DateUtil.parseDate("2026-06-13"));
        context.setOperator("tester");
        return context;
    }

    private TmTaskDraft buildTask(String orderNo, String machineCode) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo(orderNo);
        task.setMachineCode(machineCode);
        task.setTreadCode("TR-" + orderNo);
        task.setGlueCode("GL-" + orderNo);
        task.setMouthPlateCode("MP-" + orderNo);
        task.setDemandQty(new BigDecimal("100"));
        task.setPlanQty(new BigDecimal("100"));
        return task;
    }

    private TmMachineCandidate enabledCandidate(String machineCode, String remainCapacity) {
        TmMachineCandidate candidate = new TmMachineCandidate();
        candidate.setMachineCode(machineCode);
        candidate.setEnabled(Boolean.TRUE);
        candidate.setRemainCapacity(new BigDecimal(remainCapacity));
        candidate.setMouthPlateMatched(Boolean.TRUE);
        candidate.setGlueMachineMatched(Boolean.TRUE);
        candidate.setFixedMachineSelected(Boolean.TRUE);
        candidate.setFixedMachineExcluded(Boolean.FALSE);
        return candidate;
    }

    private TmStrategyRegistry buildRegistry() {
        return new TmStrategyRegistry(Collections.singletonList(new TmGuardDemandQtyStrategy()),
                Collections.singletonList(new TmDefaultPlanQtyStrategy()),
                Collections.singletonList(new TmDefaultMachineFilterRule()),
                Collections.singletonList(new TmDefaultMachineScoreStrategy()),
                Collections.singletonList(new TmDefaultTaskSortStrategy()));
    }
}
