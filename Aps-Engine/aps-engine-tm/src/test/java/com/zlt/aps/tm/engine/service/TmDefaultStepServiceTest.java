package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.impl.*;
import com.zlt.aps.tm.engine.strategy.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        // 6点库存净值 200、14点预计库存 200，库存抵扣后基础需求=max(100-200,500-200,0)=300，保持原预期。
        TmStockForecast forecast = new TmStockForecast();
        forecast.setTreadCode("TR-ORD-1");
        forecast.setSixClockStockQty(new BigDecimal("200"));
        forecast.setRollingStockQty(new BigDecimal("200"));
        context.getStockForecastMap().put("TR-ORD-1", forecast);

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

    /**
     * 测试内容：验证当前班产能不足时，剩余量优先顺延到下一班同机台。
     * 测试场景：TM001 一班已排 3000，机台最大班产 5300，新任务计划量 6144，TM002 同班仍有产能。
     * 预期结果：新任务一班先在 TM001 排 2300，剩余 3844 进入 TM001 二班，不再进入 TM002 一班。
     */
    @Test
    public void machineAssignShouldCarryCapacityOverflowToNextShiftBeforeSameShiftOtherMachine() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft existing = buildTask("ORD-EXISTING", "TM001");
        existing.setTreadCode("TR-OVERFLOW");
        existing.setGlueCode("GL-OVERFLOW");
        existing.setMouthPlateCode("MP-OVERFLOW");
        existing.setPlanQty(new BigDecimal("3000"));
        service.assignPrepared(context, Collections.singletonList(existing));

        TmTaskDraft overflowTask = buildTask("ORD-OVERFLOW", null);
        overflowTask.setTreadCode("TR-OVERFLOW");
        overflowTask.setGlueCode("GL-OVERFLOW");
        overflowTask.setMouthPlateCode("MP-OVERFLOW");
        overflowTask.setPlanQty(new BigDecimal("6144"));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "5300");
        tm001.setMaxCapacity(new BigDecimal("5300"));
        TmMachineCandidate tm002 = enabledCandidate("TM002", "3844");
        tm002.setMaxCapacity(new BigDecimal("3844"));
        context.setMachineCandidateList(Arrays.asList(tm001, tm002));
        context.setTaskDraftList(Collections.singletonList(overflowTask));

        service.assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> shiftOneChain = context.getTaskChain("TM001", 1);
        assertNotNull(shiftOneChain);
        assertEquals(2, shiftOneChain.getSize());
        assertEquals(new BigDecimal("2300"), shiftOneChain.toList().get(1).getTask().getPlanQty());

        ScheduleTaskLinkedList<TmTaskDraft> shiftTwoChain = context.getTaskChain("TM001", 2);
        assertNotNull(shiftTwoChain);
        assertEquals(1, shiftTwoChain.getSize());
        assertEquals(new BigDecimal("3844"), shiftTwoChain.toList().get(0).getTask().getPlanQty());
        assertEquals(Integer.valueOf(2), shiftTwoChain.toList().get(0).getTask().getShiftOrder());
        assertEquals("TM001", shiftTwoChain.toList().get(0).getTask().getMachineCode());
        assertNull(context.getTaskChain("TM002", 1));
    }
    /**
     * 测试内容：验证当前班产能溢出后，从下一班开始优先使用匹配机台剩余产能。
     * 测试场景：任务从 5 班开始排，TM001/TM002 每班最大产能 100，任务计划量 350。
     * 预期结果：5 班只在选中 TM001 排 100，6 班 TM001/TM002 各排 100，剩余 50 产能不足未排。
     */
    @Test
    public void machineAssignShouldCarryOverflowToNextShiftMatchedMachines() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft overflowTask = buildTask("ORD-ROLL", null);
        overflowTask.setShiftOrder(5);
        overflowTask.setPlanQty(new BigDecimal("350"));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "100");
        tm001.setMaxCapacity(new BigDecimal("100"));
        TmMachineCandidate tm002 = enabledCandidate("TM002", "100");
        tm002.setMaxCapacity(new BigDecimal("100"));
        context.setMachineCandidateList(Arrays.asList(tm001, tm002));
        context.setTaskDraftList(Collections.singletonList(overflowTask));

        service.assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> tm001ShiftFiveChain = context.getTaskChain("TM001", 5);
        assertNotNull(tm001ShiftFiveChain);
        assertEquals(new BigDecimal("100"), tm001ShiftFiveChain.toList().get(0).getTask().getPlanQty());
        assertNull(context.getTaskChain("TM002", 5));

        ScheduleTaskLinkedList<TmTaskDraft> tm001ShiftSixChain = context.getTaskChain("TM001", 6);
        assertNotNull(tm001ShiftSixChain);
        assertEquals(new BigDecimal("100"), tm001ShiftSixChain.toList().get(0).getTask().getPlanQty());
        ScheduleTaskLinkedList<TmTaskDraft> tm002ShiftSixChain = context.getTaskChain("TM002", 6);
        assertNotNull(tm002ShiftSixChain);
        assertEquals(new BigDecimal("100"), tm002ShiftSixChain.toList().get(0).getTask().getPlanQty());

        List<TmTaskDraft> unplannedTasks = context.getTaskDraftList().stream()
                .filter(task -> TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode().equals(task.getUnplannedReasonCode()))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, unplannedTasks.size());
        assertEquals(new BigDecimal("50"), unplannedTasks.get(0).getPlanQty());
    }

    /**
     * 测试内容：验证工装限制压掉的顺延量会优先合并到下一班同机台同胎面任务。
     * 测试场景：当前班工装限制后计划 400，工装溢出 200；下一班 TM001 已有同胎面计划 300。
     * 预期结果：当前班只排 400，下一班同机台同胎面任务合并为 500。
     */
    @Test
    public void machineAssignShouldMergeToolOverflowToNextShiftSameMachineSameTreadTask() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft nextShiftExisting = buildTask("ORD-TOOL-NEXT", "TM001");
        nextShiftExisting.setTreadCode("TR-CARRY");
        nextShiftExisting.setGlueCode("GL-CARRY");
        nextShiftExisting.setMouthPlateCode("MP-CARRY");
        nextShiftExisting.setShiftOrder(2);
        nextShiftExisting.setPlanQty(new BigDecimal("300"));
        service.assignPrepared(context, Collections.singletonList(nextShiftExisting));
        TmTaskDraft currentTask = buildTask("ORD-TOOL-CURRENT", null);
        currentTask.setTreadCode("TR-CARRY");
        currentTask.setGlueCode("GL-CARRY");
        currentTask.setMouthPlateCode("MP-CARRY");
        currentTask.setPlanQty(new BigDecimal("400"));
        currentTask.setToolOverflowQty(new BigDecimal("200"));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "1000");
        tm001.setMaxCapacity(new BigDecimal("1000"));
        context.setMachineCandidateList(Collections.singletonList(tm001));
        context.setTaskDraftList(Collections.singletonList(currentTask));

        service.assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> shiftOneChain = context.getTaskChain("TM001", 1);
        assertNotNull(shiftOneChain);
        assertEquals(new BigDecimal("400"), shiftOneChain.toList().get(0).getTask().getPlanQty());
        ScheduleTaskLinkedList<TmTaskDraft> shiftTwoChain = context.getTaskChain("TM001", 2);
        assertNotNull(shiftTwoChain);
        assertEquals(1, shiftTwoChain.getSize());
        assertEquals(new BigDecimal("500"), shiftTwoChain.toList().get(0).getTask().getPlanQty());
    }

    /**
     * 测试内容：验证当前班产能不足产生的顺延量会优先合并到下一班同机台同胎面任务。
     * 测试场景：TM001 一班剩余产能 400，当前任务计划 600；下一班 TM001 已有同胎面计划 300。
     * 预期结果：当前班只排 400，产能溢出 200 合并到下一班同胎面任务，下一班计划量变为 500。
     */
    @Test
    public void machineAssignShouldMergeCapacityOverflowToNextShiftSameMachineSameTreadTask() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft shiftOneExisting = buildTask("ORD-CAP-EXISTING-1", "TM001");
        shiftOneExisting.setTreadCode("TR-OTHER");
        shiftOneExisting.setPlanQty(new BigDecimal("200"));
        TmTaskDraft nextShiftExisting = buildTask("ORD-CAP-EXISTING-2", "TM001");
        nextShiftExisting.setTreadCode("TR-CAP");
        nextShiftExisting.setGlueCode("GL-CAP");
        nextShiftExisting.setMouthPlateCode("MP-CAP");
        nextShiftExisting.setShiftOrder(2);
        nextShiftExisting.setPlanQty(new BigDecimal("300"));
        service.assignPrepared(context, Arrays.asList(shiftOneExisting, nextShiftExisting));
        TmTaskDraft currentTask = buildTask("ORD-CAP-CURRENT", null);
        currentTask.setTreadCode("TR-CAP");
        currentTask.setGlueCode("GL-CAP");
        currentTask.setMouthPlateCode("MP-CAP");
        currentTask.setPlanQty(new BigDecimal("600"));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "600");
        tm001.setMaxCapacity(new BigDecimal("600"));
        context.setMachineCandidateList(Collections.singletonList(tm001));
        context.setTaskDraftList(Collections.singletonList(currentTask));

        service.assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> shiftOneChain = context.getTaskChain("TM001", 1);
        assertNotNull(shiftOneChain);
        assertEquals(2, shiftOneChain.getSize());
        assertEquals(new BigDecimal("400"), shiftOneChain.toList().get(1).getTask().getPlanQty());
        ScheduleTaskLinkedList<TmTaskDraft> shiftTwoChain = context.getTaskChain("TM001", 2);
        assertNotNull(shiftTwoChain);
        assertEquals(1, shiftTwoChain.getSize());
        assertEquals(new BigDecimal("500"), shiftTwoChain.toList().get(0).getTask().getPlanQty());
    }
    /**
     * 测试内容：验证滚动到六班后匹配机台产能仍不足时，剩余计划量写入产能不足未排。
     * 测试场景：任务从 5 班开始排，TM001/TM002 每班最大产能 100，任务计划量 450。
     * 预期结果：5、6 班合计排 400，剩余 50 标记为产能不足未排。
     */
    @Test
    public void machineAssignShouldMarkRestUnplannedWhenAllMatchedMachinesLackCapacityAfterSixthShift() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft overflowTask = buildTask("ORD-ROLL-UNPLANNED", null);
        overflowTask.setShiftOrder(5);
        overflowTask.setPlanQty(new BigDecimal("450"));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "100");
        tm001.setMaxCapacity(new BigDecimal("100"));
        TmMachineCandidate tm002 = enabledCandidate("TM002", "100");
        tm002.setMaxCapacity(new BigDecimal("100"));
        context.setMachineCandidateList(Arrays.asList(tm001, tm002));
        context.setTaskDraftList(Collections.singletonList(overflowTask));

        service.assign(context);

        assertEquals(new BigDecimal("100"), context.getTaskChain("TM001", 5).toList().get(0).getTask().getPlanQty());
        assertNull(context.getTaskChain("TM002", 5));
        assertEquals(new BigDecimal("100"), context.getTaskChain("TM001", 6).toList().get(0).getTask().getPlanQty());
        assertEquals(new BigDecimal("100"), context.getTaskChain("TM002", 6).toList().get(0).getTask().getPlanQty());

        List<TmTaskDraft> unplannedTasks = context.getTaskDraftList().stream()
                .filter(task -> TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode().equals(task.getUnplannedReasonCode()))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, unplannedTasks.size());
        assertEquals(new BigDecimal("150"), unplannedTasks.get(0).getPlanQty());
        assertEquals(Integer.valueOf(6), unplannedTasks.get(0).getShiftOrder());
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

    /**
     * 测试内容：验证候选机台全部因剩余产能不足被过滤时，未排原因归类为产能不足。
     * 测试场景：任务无机台，两台候选机台剩余产能均为 0。
     * 预期结果：未排原因编码为 CAPACITY_NOT_ENOUGH，而非一律标 NO_AVAILABLE_MACHINE。
     */
    @Test
    public void machineAssignShouldMarkCapacityNotEnoughWhenAllCandidatesFilteredByCapacity() {
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-NOCAP", null);
        task.setPlanQty(new BigDecimal("500"));
        context.setTaskDraftList(Collections.singletonList(task));
        // 两台候选机台剩余产能均为 0，全部会被 NO_REMAIN_CAPACITY 过滤。
        TmMachineCandidate tm01 = enabledCandidate("TM01", "0");
        TmMachineCandidate tm02 = enabledCandidate("TM02", "0");
        context.setMachineCandidateList(Arrays.asList(tm01, tm02));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        assertEquals(TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode(), task.getUnplannedReasonCode());
    }

    /**
     * 测试内容：验证多个来源工单滚动到六班后仍未排完时，未排顺延任务业务键保持唯一。
     * 测试场景：两个不同来源工单使用同胎面、同胶料、同口型，均在六班部分承接后仍有剩余未排。
     * 预期结果：两个产能不足未排任务的业务键不同，避免解释表唯一键冲突。
     */
    @Test
    public void machineAssignShouldKeepUnplannedCarryoverBusinessKeyUniqueForDifferentSourceOrders() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft firstTask = buildSameSpecCarryoverTask("ORD-SAME-1");
        TmTaskDraft secondTask = buildSameSpecCarryoverTask("ORD-SAME-2");
        TmMachineCandidate tm001 = enabledCandidate("TM001", "100");
        tm001.setMaxCapacity(new BigDecimal("100"));
        TmMachineCandidate tm002 = enabledCandidate("TM002", "100");
        tm002.setMaxCapacity(new BigDecimal("100"));
        context.setMachineCandidateList(Arrays.asList(tm001, tm002));
        context.setTaskDraftList(Arrays.asList(firstTask, secondTask));

        service.assign(context);

        List<TmTaskDraft> unplannedTasks = context.getTaskDraftList().stream()
                .filter(task -> TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode().equals(task.getUnplannedReasonCode()))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(2, unplannedTasks.size());
        assertNotEquals(unplannedTasks.get(0).getBusinessKey(), unplannedTasks.get(1).getBusinessKey());
    }

    /**
     * 构建同规格顺延测试任务。
     *
     * @param orderNo 来源工单号
     * @return 同胎面、同胶料、同口型的六班待排任务
     */
    private TmTaskDraft buildSameSpecCarryoverTask(String orderNo) {
        TmTaskDraft task = buildTask(orderNo, null);
        task.setTreadCode("TR-SAME");
        task.setGlueCode("GL-SAME");
        task.setMouthPlateCode("MP-SAME");
        task.setShiftOrder(6);
        task.setPlanQty(new BigDecimal("250"));
        return task;
    }

    private TmStrategyRegistry buildRegistry() {
        return new TmStrategyRegistry(Collections.singletonList(new TmGuardDemandQtyStrategy()),
                Collections.singletonList(new TmDefaultPlanQtyStrategy()),
                Collections.singletonList(new TmDefaultMachineFilterRule()),
                Collections.singletonList(new TmDefaultMachineScoreStrategy()),
                Collections.singletonList(new TmDefaultTaskSortStrategy()));
    }
}
