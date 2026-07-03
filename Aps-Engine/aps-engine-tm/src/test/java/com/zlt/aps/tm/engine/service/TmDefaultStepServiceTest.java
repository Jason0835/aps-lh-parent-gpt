package com.zlt.aps.tm.engine.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.service.impl.*;
import com.zlt.aps.tm.engine.strategy.*;
import org.junit.Test;
import org.slf4j.LoggerFactory;

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
     * 测试内容：验证排序日志包含排序策略、排序摘要和排序依据字段。
     * 测试场景：两个任务按默认策略排序，日志捕获排序前后信息。
     * 预期结果：info日志可直接看到sortIndex、businessKey、supplyHours、planQty和demandQty。
     */
    @Test
    public void taskSortShouldWriteTraceableSortLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(TmTaskSortService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            TmScheduleContext context = buildContext();
            context.setBatchNo("BATCH-SORT");
            context.setTraceId("TRACE-SORT");
            TmTaskDraft taskB = buildTask("ORD-SORT-2", null);
            taskB.setSupplyHours(new BigDecimal("8"));
            taskB.setDemandQty(new BigDecimal("200"));
            taskB.setPlanQty(new BigDecimal("200"));
            TmTaskDraft taskA = buildTask("ORD-SORT-1", null);
            taskA.setSupplyHours(new BigDecimal("4"));
            taskA.setDemandQty(new BigDecimal("100"));
            taskA.setPlanQty(new BigDecimal("100"));
            context.setTaskDraftList(Arrays.asList(taskB, taskA));

            new TmTaskSortService(buildRegistry()).sort(context);

            assertTrue(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_TASK_SORT]")
                            && message.contains("batchNo=BATCH-SORT")
                            && message.contains("traceId=TRACE-SORT")
                            && message.contains("beforeOrder=")
                            && message.contains("afterOrder=")));
            assertTrue(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_TASK_SORT_DETAIL]")
                            && message.contains("sortIndex=1")
                            && message.contains("businessKey=" + taskA.getBusinessKey())
                            && message.contains("supplyHours=4")
                            && message.contains("planQty=100")
                            && message.contains("demandQty=100")));
        } finally {
            logger.detachAppender(appender);
        }
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
     * 测试内容：验证已排任务转换结果表时保留独立基部胶编码。
     * 测试场景：任务草稿包含主胶、基部胶和口型，并已进入机台任务链。
     * 预期结果：转换后的 T_TM_SCHEDULE_RESULT 实体写入 baseGlueCode，不依赖整条胶料组合编码。
     */
    @Test
    public void persistServiceShouldWriteBaseGlueCodeToScheduleResult() {
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-BASE", "TM01");
        task.setBaseGlueCode("BASE-ORD-BASE");
        TmMachineCandidate candidate = enabledCandidate("TM01", "1000");
        new TmTaskChainScheduleService().appendAutoTask(task, candidate, context);

        List<TmScheduleResult> resultList = new TmPersistService()
                .convertChainToResult(context.getTaskChain("TM01", 1), context);

        assertEquals(1, resultList.size());
        assertEquals("BASE-ORD-BASE", resultList.get(0).getBaseGlueCode());
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
     * 测试内容：验证一班动态排序会继承同机台前一天最后任务作为前置任务。
     * 测试场景：TM001 前一天链尾为 GL-A，当前一班输入顺序先 GL-B 后 GL-A。
     * 预期结果：机台分配时优先排与前置任务连续性更高的 GL-A。
     */
    @Test
    public void machineAssignShouldUsePreviousDayPredecessorForFirstShiftOrder() {
        TmScheduleContext context = buildContext();
        context.getMachinePredecessorMap().put("TM001", buildPredecessor("TM001", "TR-A", "GL-A", "MP-A"));
        TmTaskDraft laterTask = buildTask("ORD-LATER", null);
        laterTask.setTreadCode("TR-B");
        laterTask.setGlueCode("GL-B");
        laterTask.setMouthPlateCode("MP-B");
        laterTask.setPlanQty(new BigDecimal("100"));
        TmTaskDraft continuousTask = buildTask("ORD-CONTINUOUS", null);
        continuousTask.setTreadCode("TR-A");
        continuousTask.setGlueCode("GL-A");
        continuousTask.setMouthPlateCode("MP-A");
        continuousTask.setPlanQty(new BigDecimal("100"));
        context.setTaskDraftList(Arrays.asList(laterTask, continuousTask));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "1000");
        tm001.setMaxCapacity(new BigDecimal("1000"));
        context.setMachineCandidateList(Collections.singletonList(tm001));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain("TM001", 1);
        assertNotNull(chain);
        assertEquals("TR-A", chain.toList().get(0).getTask().getTreadCode());
        assertEquals("TR-B", chain.toList().get(1).getTask().getTreadCode());
    }

    /**
     * 测试内容：验证一班链式排序中主胶料连续优先于口型连续和产能适配。
     * 测试场景：TM001 前一天链尾为 GLUE07/SW007，当前一班同时存在口型匹配且计划量更大的 GLUE10
     * 和主胶料匹配但口型不匹配的 GLUE07。
     * 预期结果：机台分配时优先排 GLUE07，避免口型和产能分反超前置任务主胶料连续性。
     */
    @Test
    public void machineAssignShouldPreferPredecessorMainGlueBeforeMouthPlateAndCapacityForFirstShiftOrder() {
        TmScheduleContext context = buildContext();
        context.setScheduleDate(DateUtil.parseDate("2026-06-25"));
        context.getMachinePredecessorMap().put("TM001",
                buildPredecessor("TM001", "210400584", "GLUE07", "SW007"));
        TmTaskDraft mouthPlateAndCapacityTask = buildTask("ORD-GLUE10", null);
        mouthPlateAndCapacityTask.setTreadCode("210400584");
        mouthPlateAndCapacityTask.setGlueCode("GLUE10");
        mouthPlateAndCapacityTask.setMouthPlateCode("SW007");
        mouthPlateAndCapacityTask.setPlanQty(new BigDecimal("1200"));
        TmTaskDraft mainGlueTask = buildTask("ORD-GLUE07", null);
        mainGlueTask.setTreadCode("210400277");
        mainGlueTask.setGlueCode("GLUE07");
        mainGlueTask.setMouthPlateCode("SW001");
        mainGlueTask.setPlanQty(new BigDecimal("1000"));
        context.setTaskDraftList(Arrays.asList(mouthPlateAndCapacityTask, mainGlueTask));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "5300");
        tm001.setMaxCapacity(new BigDecimal("5300"));
        context.setMachineCandidateList(Collections.singletonList(tm001));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain("TM001", 1);
        assertNotNull(chain);
        assertEquals("210400277", chain.toList().get(0).getTask().getTreadCode());
        assertEquals("GLUE07", chain.toList().get(0).getTask().getGlueCode());
        assertEquals("210400584", chain.toList().get(1).getTask().getTreadCode());
        assertEquals("GLUE10", chain.toList().get(1).getTask().getGlueCode());
    }

    /**
     * 测试内容：验证没有任务匹配前置主胶料时仍保留口型和产能等原有排序能力。
     * 测试场景：TM001 前一天链尾为 GLUE07/SW007，当前一班两个任务都不匹配 GLUE07，
     * 其中一个任务匹配口型 SW007。
     * 预期结果：机台分配时仍优先排口型连续的任务，避免排序退化为只看主胶料。
     */
    @Test
    public void machineAssignShouldStillUseMouthPlateAndCapacityWhenNoTaskMatchesPredecessorMainGlue() {
        TmScheduleContext context = buildContext();
        context.getMachinePredecessorMap().put("TM001",
                buildPredecessor("TM001", "210400584", "GLUE07", "SW007"));
        TmTaskDraft mouthPlateTask = buildTask("ORD-MP", null);
        mouthPlateTask.setTreadCode("210400584");
        mouthPlateTask.setGlueCode("GLUE10");
        mouthPlateTask.setMouthPlateCode("SW007");
        mouthPlateTask.setPlanQty(new BigDecimal("1000"));
        TmTaskDraft otherTask = buildTask("ORD-OTHER", null);
        otherTask.setTreadCode("210400999");
        otherTask.setGlueCode("GLUE11");
        otherTask.setMouthPlateCode("SW001");
        otherTask.setPlanQty(new BigDecimal("1000"));
        context.setTaskDraftList(Arrays.asList(otherTask, mouthPlateTask));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "5300");
        tm001.setMaxCapacity(new BigDecimal("5300"));
        context.setMachineCandidateList(Collections.singletonList(tm001));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain("TM001", 1);
        assertNotNull(chain);
        assertEquals("210400584", chain.toList().get(0).getTask().getTreadCode());
        assertEquals("GLUE10", chain.toList().get(0).getTask().getGlueCode());
    }
    /**
     * 测试内容：验证二班排序继承同机台上一班链尾，而不是继续使用前一天尾部任务。
     * 测试场景：TM001 前一天链尾为 GL-OLD，一班排入 GL-A，二班同时存在 GL-OLD 和 GL-A。
     * 预期结果：二班优先排 GL-A，说明前置任务已切换为一班链尾。
     */
    @Test
    public void machineAssignShouldUsePreviousShiftTailBeforePreviousDayTail() {
        TmScheduleContext context = buildContext();
        context.getMachinePredecessorMap().put("TM001", buildPredecessor("TM001", "TR-OLD", "GL-OLD", "MP-OLD"));
        TmTaskDraft shiftOneTask = buildTask("ORD-SHIFT1", null);
        shiftOneTask.setTreadCode("TR-A");
        shiftOneTask.setGlueCode("GL-A");
        shiftOneTask.setMouthPlateCode("MP-A");
        shiftOneTask.setPlanQty(new BigDecimal("100"));
        TmTaskDraft oldGlueTask = buildTask("ORD-OLD", null);
        oldGlueTask.setShiftOrder(2);
        oldGlueTask.setTreadCode("TR-OLD");
        oldGlueTask.setGlueCode("GL-OLD");
        oldGlueTask.setMouthPlateCode("MP-OLD");
        oldGlueTask.setPlanQty(new BigDecimal("100"));
        TmTaskDraft continuousTask = buildTask("ORD-SHIFT2", null);
        continuousTask.setShiftOrder(2);
        continuousTask.setTreadCode("TR-A");
        continuousTask.setGlueCode("GL-A");
        continuousTask.setMouthPlateCode("MP-A");
        continuousTask.setPlanQty(new BigDecimal("100"));
        context.setTaskDraftList(Arrays.asList(shiftOneTask, oldGlueTask, continuousTask));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "1000");
        tm001.setMaxCapacity(new BigDecimal("1000"));
        context.setMachineCandidateList(Collections.singletonList(tm001));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> shiftTwoChain = context.getTaskChain("TM001", 2);
        assertNotNull(shiftTwoChain);
        assertEquals("TR-A", shiftTwoChain.toList().get(0).getTask().getTreadCode());
        assertEquals("TR-OLD", shiftTwoChain.toList().get(1).getTask().getTreadCode());
    }

    /**
     * 测试内容：验证多机台前置任务按同机台独立生效。
     * 测试场景：TM001 前置 GL-A，TM002 前置 GL-B，当前一班存在两个对应任务。
     * 预期结果：两个任务分别排到匹配自己前置任务的机台，不发生跨机台串扰。
     */
    @Test
    public void machineAssignShouldKeepMachinePredecessorsIndependent() {
        TmScheduleContext context = buildContext();
        context.getMachinePredecessorMap().put("TM001", buildPredecessor("TM001", "TR-A", "GL-A", "MP-A"));
        context.getMachinePredecessorMap().put("TM002", buildPredecessor("TM002", "TR-B", "GL-B", "MP-B"));
        TmTaskDraft taskA = buildTask("ORD-A", null);
        taskA.setTreadCode("TR-A");
        taskA.setGlueCode("GL-A");
        taskA.setMouthPlateCode("MP-A");
        taskA.setPlanQty(new BigDecimal("100"));
        TmTaskDraft taskB = buildTask("ORD-B", null);
        taskB.setTreadCode("TR-B");
        taskB.setGlueCode("GL-B");
        taskB.setMouthPlateCode("MP-B");
        taskB.setPlanQty(new BigDecimal("100"));
        context.setTaskDraftList(Arrays.asList(taskB, taskA));
        TmMachineCandidate tm001 = enabledCandidate("TM001", "1000");
        tm001.setMaxCapacity(new BigDecimal("1000"));
        TmMachineCandidate tm002 = enabledCandidate("TM002", "1000");
        tm002.setMaxCapacity(new BigDecimal("1000"));
        context.setMachineCandidateList(Arrays.asList(tm001, tm002));

        new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

        assertEquals("TR-A", context.getTaskChain("TM001", 1).toList().get(0).getTask().getTreadCode());
        assertEquals("TR-B", context.getTaskChain("TM002", 1).toList().get(0).getTask().getTreadCode());
    }
    /**
     * 测试内容：验证选机摘要和任务链日志包含人工排查所需字段。
     * 测试场景：任务无机台，两个候选机台评分后选择主胶连续的TM01。
     * 预期结果：info日志包含候选统计、选中机台、评分分项、计划量、产能承接和链顺序摘要。
     */
    @Test
    public void machineAssignShouldWriteSummaryAndChainLogs() {
        Logger assignLogger = (Logger) LoggerFactory.getLogger(TmMachineAssignService.class);
        Logger chainLogger = (Logger) LoggerFactory.getLogger(TmTaskChainScheduleService.class);
        ListAppender<ILoggingEvent> assignAppender = new ListAppender<>();
        ListAppender<ILoggingEvent> chainAppender = new ListAppender<>();
        assignAppender.start();
        chainAppender.start();
        assignLogger.addAppender(assignAppender);
        chainLogger.addAppender(chainAppender);
        try {
            TmScheduleContext context = buildContext();
            context.setBatchNo("BATCH-ASSIGN");
            context.setTraceId("TRACE-ASSIGN");
            TmTaskDraft task = buildTask("ORD-ASSIGN", null);
            task.setPlanQty(new BigDecimal("500"));
            task.setGlueCode("GL-A");
            context.setTaskDraftList(Collections.singletonList(task));
            TmMachineCandidate lower = enabledCandidate("TM02", "700");
            lower.setTailMainGlueCode("GL-B");
            TmMachineCandidate higher = enabledCandidate("TM01", "700");
            higher.setTailMainGlueCode("GL-A");
            context.setMachineCandidateList(Arrays.asList(lower, higher));

            new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry()).assign(context);

            assertTrue(assignAppender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_MACHINE_ASSIGN_SUMMARY]")
                            && message.contains("batchNo=BATCH-ASSIGN")
                            && message.contains("traceId=TRACE-ASSIGN")
                            && message.contains("businessKey=" + task.getBusinessKey())
                            && message.contains("candidateCount=2")
                            && message.contains("passedCount=2")
                            && message.contains("rejectedCount=0")
                            && message.contains("selectedMachineCode=TM01")
                            && message.contains("selectedScore=")
                            && message.contains("scoreItems=")
                            && message.contains("planQty=500")
                            && message.contains("assignedQty=500")
                            && message.contains("overflowQty=0")));
            assertTrue(chainAppender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("[TM_TASK_CHAIN]")
                            && message.contains("batchNo=BATCH-ASSIGN")
                            && message.contains("traceId=TRACE-ASSIGN")
                            && message.contains("operation=AUTO_APPEND")
                            && message.contains("machineCode=TM01")
                            && message.contains("shiftOrder=1")
                            && message.contains("chainOrder=" + task.getBusinessKey())));
        } finally {
            assignLogger.detachAppender(assignAppender);
            chainLogger.detachAppender(chainAppender);
        }
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
     * 构建机台前置任务快照。
     *
     * @param machineCode 机台编码
     * @param treadCode 胎面编码
     * @param glueCode 主胶料编码
     * @param mouthPlateCode 口型板编码
     * @return 机台前置任务快照
     */
    private TmTaskPredecessor buildPredecessor(String machineCode, String treadCode, String glueCode,
                                               String mouthPlateCode) {
        TmTaskPredecessor predecessor = new TmTaskPredecessor();
        predecessor.setMachineCode(machineCode);
        predecessor.setTreadCode(treadCode);
        predecessor.setGlueCode(glueCode);
        predecessor.setMouthPlateCode(mouthPlateCode);
        return predecessor;
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
     * 测试内容：验证小胶种后续同胶任务优先沿用已绑定机台。
     * 测试场景：首个小胶种任务已预置到 TM002，后续同小胶种同主胶任务在 TM001 评分更高。
     * 预期结果：后续任务仍排到 TM002，避免小胶种频繁切换机台。
     */
    @Test
    public void machineAssignShouldKeepSmallGlueTaskOnBoundMachineWhenAvailable() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft firstTask = buildSmallGlueTask("ORD-SMALL-FIRST", "TM002");
        TmTaskDraft secondTask = buildSmallGlueTask("ORD-SMALL-SECOND", null);
        TmMachineCandidate higherScoreMachine = enabledCandidate("TM001", "1000");
        higherScoreMachine.setTailMainGlueCode("GL-SMALL");
        TmMachineCandidate boundMachine = enabledCandidate("TM002", "1000");
        boundMachine.setTailMainGlueCode("GL-OTHER");
        context.setMachineCandidateList(Arrays.asList(higherScoreMachine, boundMachine));
        context.setTaskDraftList(Arrays.asList(firstTask, secondTask));

        service.assign(context);

        assertEquals("TM002", firstTask.getMachineCode());
        assertEquals("TM002", secondTask.getMachineCode());
        assertEquals("TM002", context.getSmallGlueMachineMap().get("GL-SMALL"));
    }

    /**
     * 测试内容：验证小胶种绑定机台硬约束不可用时允许切换机台。
     * 测试场景：首个小胶种任务绑定 TM002，后续同小胶种任务处理时 TM002 被禁用，TM001 可用。
     * 预期结果：后续任务切换到 TM001，并刷新小胶种绑定机台。
     */
    @Test
    public void machineAssignShouldSwitchSmallGlueMachineWhenBoundMachineUnavailable() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft firstTask = buildSmallGlueTask("ORD-SMALL-FIRST", "TM002");
        TmTaskDraft secondTask = buildSmallGlueTask("ORD-SMALL-SECOND", null);
        TmMachineCandidate availableMachine = enabledCandidate("TM001", "1000");
        TmMachineCandidate disabledBoundMachine = enabledCandidate("TM002", "1000");
        disabledBoundMachine.setEnabled(Boolean.FALSE);
        context.setMachineCandidateList(Arrays.asList(availableMachine, disabledBoundMachine));
        context.setTaskDraftList(Arrays.asList(firstTask, secondTask));

        service.assign(context);

        assertEquals("TM002", firstTask.getMachineCode());
        assertEquals("TM001", secondTask.getMachineCode());
        assertEquals("TM001", context.getSmallGlueMachineMap().get("GL-SMALL"));
    }

    /**
     * 测试内容：验证普通胶种任务不复用小胶种绑定机台。
     * 测试场景：首个任务为小胶种并绑定 TM002，后续同主胶任务不是小胶种且 TM001 评分更高。
     * 预期结果：普通任务按原评分逻辑选择 TM001，不受小胶种绑定记录影响。
     */
    @Test
    public void machineAssignShouldNotApplySmallGlueBindingToNormalTaskWithSameGlueCode() {
        TmScheduleContext context = buildContext();
        TmMachineAssignService service = new TmMachineAssignService(new TmTaskChainScheduleService(), buildRegistry());
        TmTaskDraft firstTask = buildSmallGlueTask("ORD-SMALL-FIRST", "TM002");
        TmTaskDraft normalTask = buildTask("ORD-NORMAL-SAME-GLUE", null);
        normalTask.setGlueCode("GL-SMALL");
        normalTask.setPlanQty(new BigDecimal("100"));
        normalTask.setSmallGlueFlag(Boolean.FALSE);
        TmMachineCandidate higherScoreMachine = enabledCandidate("TM001", "1000");
        higherScoreMachine.setTailMainGlueCode("GL-SMALL");
        TmMachineCandidate boundMachine = enabledCandidate("TM002", "1000");
        boundMachine.setTailMainGlueCode("GL-OTHER");
        context.setMachineCandidateList(Arrays.asList(higherScoreMachine, boundMachine));
        context.setTaskDraftList(Arrays.asList(firstTask, normalTask));

        service.assign(context);

        assertEquals("TM002", firstTask.getMachineCode());
        assertEquals("TM001", normalTask.getMachineCode());
        assertEquals("TM002", context.getSmallGlueMachineMap().get("GL-SMALL"));
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

    /**
     * 构建小胶种测试任务。
     *
     * @param orderNo 来源工单号
     * @param machineCode 预置机台编码
     * @return 小胶种任务
     */
    private TmTaskDraft buildSmallGlueTask(String orderNo, String machineCode) {
        TmTaskDraft task = buildTask(orderNo, machineCode);
        task.setTreadCode("TR-" + orderNo);
        task.setGlueCode("GL-SMALL");
        task.setMouthPlateCode("MP-SMALL");
        task.setPlanQty(new BigDecimal("100"));
        task.setSmallGlueFlag(Boolean.TRUE);
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
