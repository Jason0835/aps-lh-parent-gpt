package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.enums.TmGenerateModeEnum;
import com.zlt.aps.tm.api.enums.TmScheduleReleaseStatusEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.impl.TmPersistService;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 胎面排程落库转换服务测试。
 *
 * <p>验证运行态任务链能转换为 1-6 班结果字段，解释转换能带上批次和追踪信息。</p>
 */
public class TmPersistServiceTest {

    /**
     * 测试内容：验证任务链转换结果时，1-6 班能分别写入对应 classN 字段。
     * 测试场景：同一链表中追加 1-6 班节点，并设置统一开始、结束时间。
     * 预期结果：每条结果写入对应班次的 sequence、planQty、startTime、endTime。
     */
    @Test
    public void convertChainToResultShouldMapShiftOneToSixClassFields() {
        // 准备持久化服务、排程上下文和待转换任务链。
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        ScheduleTaskLinkedList<TmTaskDraft> chain = new ScheduleTaskLinkedList<>();
        Date startTime = DateUtil.parse("2026-06-13 08:00:00");
        Date endTime = DateUtil.parse("2026-06-13 09:00:00");
        // 逐班追加节点，覆盖 class1 到 class6 的完整字段映射。
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            ScheduleTaskNode<TmTaskDraft> node = buildNode(shiftOrder);
            node.setStartTime(startTime);
            node.setEndTime(endTime);
            chain.append(node, new ScheduleOperationContext("tester", "append", "TRACE-1"));
        }

        // 执行任务链到结果实体的转换。
        List<TmScheduleResult> resultList = service.convertChainToResult(chain, context);

        // 逐项断言每个班次写入自己的 classN 字段，避免班次字段错位。
        assertEquals(6, resultList.size());
        assertEquals(Integer.valueOf(1), resultList.get(0).getClass1Sequence());
        assertEquals(new BigDecimal("11"), resultList.get(0).getClass1PlanQty());
        assertEquals(startTime, resultList.get(0).getClass1StartTime());
        assertEquals(endTime, resultList.get(0).getClass1EndTime());
        assertEquals(Integer.valueOf(2), resultList.get(1).getClass2Sequence());
        assertEquals(new BigDecimal("12"), resultList.get(1).getClass2PlanQty());
        assertEquals(Integer.valueOf(3), resultList.get(2).getClass3Sequence());
        assertEquals(new BigDecimal("13"), resultList.get(2).getClass3PlanQty());
        assertEquals(Integer.valueOf(4), resultList.get(3).getClass4Sequence());
        assertEquals(new BigDecimal("14"), resultList.get(3).getClass4PlanQty());
        assertEquals(Integer.valueOf(5), resultList.get(4).getClass5Sequence());
        assertEquals(new BigDecimal("15"), resultList.get(4).getClass5PlanQty());
        assertEquals(Integer.valueOf(6), resultList.get(5).getClass6Sequence());
        assertEquals(new BigDecimal("16"), resultList.get(5).getClass6PlanQty());
    }

    /**
     * 测试内容：验证任务链转换会拒绝不支持的班次。
     * 测试场景：构造 shiftOrder=7 的任务节点。
     * 预期结果：转换过程抛出 ServiceException，阻止非法班次落库。
     */
    @Test(expected = ServiceException.class)
    public void convertChainToResultShouldRejectUnsupportedShiftOrder() {
        // 准备包含非法 7 班节点的链表。
        TmPersistService service = new TmPersistService();
        ScheduleTaskLinkedList<TmTaskDraft> chain = new ScheduleTaskLinkedList<>();
        chain.append(buildNode(7), new ScheduleOperationContext("tester", "append", "TRACE-1"));

        // 执行转换时应触发班次合法性校验异常。
        service.convertChainToResult(chain, buildContext());
    }

    /**
     * 测试内容：验证解释记录转换会带上上下文、任务计划和未排信息。
     * 测试场景：构造带需求、计划量、未排原因、规则命中和候选机台快照的任务。
     * 预期结果：解释实体包含批次、追踪号、需求、计划量、未排原因、快照 JSON 和默认状态。
     */
    @Test
    public void convertExplainShouldFillContextAndPlanFields() {
        // 准备服务、上下文和带业务字段的任务草稿。
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(1);
        task.setDemandQty(new BigDecimal("21"));
        task.setMachineCode("TM01");
        task.setUnplannedReasonCode("NO_MACHINE");
        task.setUnplannedReasonDesc("无可用机台");
        // 准备快照信息，验证解释表能保留规则、候选机台和系统分析内容。
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setRuleHitJson("[{\"ruleCode\":\"R1\"}]");
        snapshot.setCandidateMachineJson("[{\"machineCode\":\"TM01\"}]");
        snapshot.setUnplannedEvidenceJson("[{\"reason\":\"NO_MACHINE\"}]");
        snapshot.setSysAnalysis("系统分析");

        // 执行解释实体转换。
        TmScheduleResultExplain explain = service.convertExplain(task, snapshot, context);

        // 断言上下文、任务计划、未排原因和快照字段全部写入解释实体。
        assertEquals("F1", explain.getFactoryCode());
        assertEquals("BATCH-1", explain.getBatchNo());
        assertEquals("TRACE-1", explain.getTraceId());
        assertEquals("TR-1|GL-1|MP-1|1", explain.getTaskBusinessKey());
        assertEquals("ORD-1", explain.getTaskOrderNo());
        assertEquals("SRC-ORD-1", explain.getSourceOrderNos());
        assertEquals(Integer.valueOf(1), explain.getShiftOrder());
        assertEquals(new BigDecimal("21"), explain.getBaseDemandQty());
        assertEquals(new BigDecimal("21"), explain.getRequiredQty());
        assertEquals(new BigDecimal("11"), explain.getFinalPlanQty());
        assertEquals("NO_MACHINE", explain.getUnplannedReasonCode());
        assertEquals("无可用机台", explain.getUnplannedReasonDesc());
        assertEquals(snapshot.getRuleHitJson(), explain.getRuleHitJson());
        assertEquals(snapshot.getCandidateMachineJson(), explain.getCandidateMachineJson());
        assertEquals(snapshot.getUnplannedEvidenceJson(), explain.getUnplannedEvidenceJson());
        assertEquals("系统分析", explain.getSysAnalysis());
        assertEquals(TmScheduleReleaseStatusEnum.WAIT_RELEASE.getCode(), explain.getResultStatus());
        assertEquals(TmScheduleStepEnum.PERSIST.getCode(), explain.getCurrentStepCode());
        assertEquals(TmGenerateModeEnum.ENGINE_SKELETON.getCode(), explain.getGenerateMode());
    }

    /**
     * 测试内容：验证持久化结果对象能累计错误信息并返回最后一条错误。
     * 测试场景：连续添加结果表和解释表写入失败信息。
     * 预期结果：错误数量、错误列表和最后错误信息都与添加顺序一致。
     */
    @Test
    public void persistResultShouldKeepErrorMessagesAndLastError() {
        // 准备空持久化结果对象。
        TmPersistResult persistResult = new TmPersistResult();

        // 连续追加两条模拟落库错误，覆盖错误累计和最后错误提取。
        persistResult.addErrorMsg("结果表写入失败:ORD-001");
        persistResult.addErrorMsg("解释表写入失败:ORD-002");

        // 断言错误计数、列表长度和最后错误内容。
        assertEquals(2, persistResult.getErrorCount());
        assertEquals(2, persistResult.getErrorMsgList().size());
        assertEquals("解释表写入失败:ORD-002", persistResult.getLastErrorMsg());
    }

    /**
     * 测试内容：验证未排任务转换结果时不写机台编码，并保留解释信息。
     * 测试场景：构造无机台未排任务和对应快照。
     * 预期结果：未排结果机台为空，解释记录包含未排原因和规则命中 JSON。
     */
    @Test
    public void convertUnplannedShouldReturnResultWithEmptyMachineCode() {
        // 准备未排任务，模拟没有可用机台导致无法排产。
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(1);
        task.setUnplannedReasonCode("NO_MACHINE");
        task.setUnplannedReasonDesc("无可用机台");
        // 准备未排快照，验证解释转换可保留无候选机台证据。
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setRuleHitJson("[{\"ruleCode\":\"R1\"}]");
        snapshot.setCandidateMachineJson("[]");
        snapshot.setUnplannedEvidenceJson("[{\"reason\":\"NO_MACHINE\"}]");
        snapshot.setSysAnalysis("未找到可用机台");

        // 分别转换未排结果和未排解释。
        TmScheduleResult unplannedResult = service.convertUnplanned(task, context);
        TmScheduleResultExplain unplannedExplain = service.convertExplain(task, snapshot, context);

        // 断言未排结果基础字段，重点确认 machineCode 不被写入。
        assertEquals("F1", unplannedResult.getFactoryCode());
        assertEquals("BATCH-1", unplannedResult.getBatchNo());
        assertEquals(null, unplannedResult.getMachineCode());
        assertEquals("TR-1", unplannedResult.getTreadCode());

        // 断言未排解释字段，确保原因和规则快照可追踪。
        assertEquals("NO_MACHINE", unplannedExplain.getUnplannedReasonCode());
        assertEquals("无可用机台", unplannedExplain.getUnplannedReasonDesc());
        assertEquals("[{\"ruleCode\":\"R1\"}]", unplannedExplain.getRuleHitJson());
    }

    /**
     * 测试内容：验证单个未排任务能按自身班次写入对应 classN 字段。
     * 测试场景：构造 shiftOrder=4 的未排任务。
     * 预期结果：只写 class4Sequence 和 class4PlanQty，其他班次字段保持空。
     */
    @Test
    public void convertUnplannedShouldMapTaskShiftFields() {
        // 准备 4 班未排任务，聚焦验证 class4 字段映射。
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(4);
        task.setShiftOrder(4);

        // 执行未排结果转换。
        TmScheduleResult unplannedResult = service.convertUnplanned(task, context);

        // 断言未排结果不带机台，且只写入 4 班对应字段。
        assertEquals(null, unplannedResult.getMachineCode());
        assertEquals(Integer.valueOf(1), unplannedResult.getClass4Sequence());
        assertEquals(new BigDecimal("14"), unplannedResult.getClass4PlanQty());
        assertEquals(null, unplannedResult.getClass1Sequence());
    }

    /**
     * 测试内容：验证未排转换覆盖 1-6 班完整映射。
     * 测试场景：循环构造 1-6 班未排任务。
     * 预期结果：每个任务只写本班次 classN 字段，其他班次字段为空。
     */
    @Test
    public void convertUnplannedShouldMapEveryShiftToClassFields() {
        // 准备服务和公共上下文，循环覆盖所有合法班次。
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();

        // 逐班执行未排转换，避免只验证 shiftOrder=1。
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            TmTaskDraft task = buildTask(shiftOrder);
            task.setShiftOrder(shiftOrder);

            // 执行当前班次未排结果转换。
            TmScheduleResult result = service.convertUnplanned(task, context);

            // 断言当前班次字段已写入，非当前班次字段保持空。
            assertShiftFields(result, shiftOrder, Integer.valueOf(1), new BigDecimal(10 + shiftOrder));
            assertOtherShiftFieldsEmpty(result, shiftOrder);
        }
    }

    /**
     * 测试内容：验证未排转换拒绝 0 班。
     * 测试场景：构造 shiftOrder=0 的任务。
     * 预期结果：转换抛出 ServiceException，避免非法班次进入结果表。
     */
    @Test(expected = ServiceException.class)
    public void convertUnplannedShouldRejectZeroShiftOrder() {
        // 准备非法 0 班任务。
        TmTaskDraft task = buildTask(0);
        task.setShiftOrder(0);

        // 执行未排转换时应触发班次校验异常。
        new TmPersistService().convertUnplanned(task, buildContext());
    }

    /**
     * 测试内容：验证未排持久化入口在合法任务、快照和上下文下可正常执行。
     * 测试场景：构造 1 班未排任务和空规则快照。
     * 预期结果：方法执行不抛异常，说明入口参数和转换链路可用。
     */
    @Test
    public void persistUnplannedShouldCompleteWithoutException() {
        // 准备合法未排任务、上下文和最小快照数据。
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(1);
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setRuleHitJson("[]");

        // 执行未排持久化入口，当前测试只验证无异常完成。
        service.persistUnplanned(task, snapshot, context);
    }

    /**
     * 测试内容：验证未排持久化入口拒绝空任务。
     * 测试场景：task 参数传 null。
     * 预期结果：方法抛出 ServiceException，避免空任务继续转换。
     */
    @Test(expected = ServiceException.class)
    public void persistUnplannedShouldRejectNullTask() {
        // 传入空任务，验证入口参数校验。
        new TmPersistService().persistUnplanned(null, new TmSnapshotBuildResult(), buildContext());
    }

    /**
     * 测试内容：验证未排持久化入口拒绝空上下文。
     * 测试场景：context 参数传 null。
     * 预期结果：方法抛出 ServiceException，避免缺少批次、工厂等上下文信息。
     */
    @Test(expected = ServiceException.class)
    public void persistUnplannedShouldRejectNullContext() {
        // 传入空上下文，验证入口参数校验。
        new TmPersistService().persistUnplanned(buildTask(1), new TmSnapshotBuildResult(), null);
    }

    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F1");
        context.setBatchNo("BATCH-1");
        context.setTraceId("TRACE-1");
        context.setScheduleDate(DateUtil.parseDate("2026-06-13"));
        return context;
    }

    private ScheduleTaskNode<TmTaskDraft> buildNode(int shiftOrder) {
        return new ScheduleTaskNode<>("TASK-" + shiftOrder, buildTask(shiftOrder), "TM01",
                LocalDate.of(2026, 6, 13), "CLASS" + shiftOrder, shiftOrder,
                new BigDecimal(10 + shiftOrder));
    }

    private TmTaskDraft buildTask(int shiftOrder) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo("ORD-" + shiftOrder);
        task.setSourceOrderNos("SRC-ORD-" + shiftOrder);
        task.setTreadCode("TR-" + shiftOrder);
        task.setGlueCode("GL-" + shiftOrder);
        task.setMouthPlateCode("MP-" + shiftOrder);
        task.setShiftOrder(shiftOrder);
        task.setPlanQty(new BigDecimal(10 + shiftOrder));
        return task;
    }

    private void assertShiftFields(TmScheduleResult result, int shiftOrder, Integer sequence, BigDecimal planQty) {
        if (shiftOrder == 1) {
            assertEquals(sequence, result.getClass1Sequence());
            assertEquals(planQty, result.getClass1PlanQty());
        } else if (shiftOrder == 2) {
            assertEquals(sequence, result.getClass2Sequence());
            assertEquals(planQty, result.getClass2PlanQty());
        } else if (shiftOrder == 3) {
            assertEquals(sequence, result.getClass3Sequence());
            assertEquals(planQty, result.getClass3PlanQty());
        } else if (shiftOrder == 4) {
            assertEquals(sequence, result.getClass4Sequence());
            assertEquals(planQty, result.getClass4PlanQty());
        } else if (shiftOrder == 5) {
            assertEquals(sequence, result.getClass5Sequence());
            assertEquals(planQty, result.getClass5PlanQty());
        } else if (shiftOrder == 6) {
            assertEquals(sequence, result.getClass6Sequence());
            assertEquals(planQty, result.getClass6PlanQty());
        }
    }

    private void assertOtherShiftFieldsEmpty(TmScheduleResult result, int expectedShiftOrder) {
        if (expectedShiftOrder != 1) {
            assertEquals(null, result.getClass1Sequence());
            assertEquals(null, result.getClass1PlanQty());
        }
        if (expectedShiftOrder != 2) {
            assertEquals(null, result.getClass2Sequence());
            assertEquals(null, result.getClass2PlanQty());
        }
        if (expectedShiftOrder != 3) {
            assertEquals(null, result.getClass3Sequence());
            assertEquals(null, result.getClass3PlanQty());
        }
        if (expectedShiftOrder != 4) {
            assertEquals(null, result.getClass4Sequence());
            assertEquals(null, result.getClass4PlanQty());
        }
        if (expectedShiftOrder != 5) {
            assertEquals(null, result.getClass5Sequence());
            assertEquals(null, result.getClass5PlanQty());
        }
        if (expectedShiftOrder != 6) {
            assertEquals(null, result.getClass6Sequence());
            assertEquals(null, result.getClass6PlanQty());
        }
    }
}
