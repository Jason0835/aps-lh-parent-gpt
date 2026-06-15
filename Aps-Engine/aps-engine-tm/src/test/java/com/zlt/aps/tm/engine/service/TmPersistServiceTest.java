package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.enums.TmScheduleReleaseStatusEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
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

    @Test
    public void convertChainToResultShouldMapShiftOneToSixClassFields() {
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        ScheduleTaskLinkedList<TmTaskDraft> chain = new ScheduleTaskLinkedList<>();
        Date startTime = DateUtil.parse("2026-06-13 08:00:00");
        Date endTime = DateUtil.parse("2026-06-13 09:00:00");
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            ScheduleTaskNode<TmTaskDraft> node = buildNode(shiftOrder);
            node.setStartTime(startTime);
            node.setEndTime(endTime);
            chain.append(node, new ScheduleOperationContext("tester", "append", "TRACE-1"));
        }

        List<TmScheduleResult> resultList = service.convertChainToResult(chain, context);

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

    @Test(expected = IllegalArgumentException.class)
    public void convertChainToResultShouldRejectUnsupportedShiftOrder() {
        TmPersistService service = new TmPersistService();
        ScheduleTaskLinkedList<TmTaskDraft> chain = new ScheduleTaskLinkedList<>();
        chain.append(buildNode(7), new ScheduleOperationContext("tester", "append", "TRACE-1"));

        service.convertChainToResult(chain, buildContext());
    }

    @Test
    public void convertExplainShouldFillContextAndPlanFields() {
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(1);
        task.setDemandQty(new BigDecimal("21"));
        task.setMachineCode("TM01");
        task.setUnplannedReasonCode("NO_MACHINE");
        task.setUnplannedReasonDesc("无可用机台");
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setRuleHitJson("[{\"ruleCode\":\"R1\"}]");
        snapshot.setCandidateMachineJson("[{\"machineCode\":\"TM01\"}]");
        snapshot.setUnplannedEvidenceJson("[{\"reason\":\"NO_MACHINE\"}]");
        snapshot.setSysAnalysis("系统分析");

        TmScheduleResultExplain explain = service.convertExplain(task, snapshot, context);

        assertEquals("F1", explain.getFactoryCode());
        assertEquals("BATCH-1", explain.getBatchNo());
        assertEquals("TRACE-1", explain.getTraceId());
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
        assertEquals("ENGINE_SKELETON", explain.getGenerateMode());
    }

    @Test
    public void convertUnplannedShouldReturnResultWithEmptyMachineCode() {
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(1);
        task.setUnplannedReasonCode("NO_MACHINE");
        task.setUnplannedReasonDesc("无可用机台");
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setRuleHitJson("[{\"ruleCode\":\"R1\"}]");
        snapshot.setCandidateMachineJson("[]");
        snapshot.setUnplannedEvidenceJson("[{\"reason\":\"NO_MACHINE\"}]");
        snapshot.setSysAnalysis("未找到可用机台");

        TmScheduleResult unplannedResult = service.convertUnplanned(task, context);
        TmScheduleResultExplain unplannedExplain = service.convertExplain(task, snapshot, context);

        assertEquals("F1", unplannedResult.getFactoryCode());
        assertEquals("BATCH-1", unplannedResult.getBatchNo());
        assertEquals(null, unplannedResult.getMachineCode());
        assertEquals("TR-1", unplannedResult.getTreadCode());

        assertEquals("NO_MACHINE", unplannedExplain.getUnplannedReasonCode());
        assertEquals("无可用机台", unplannedExplain.getUnplannedReasonDesc());
        assertEquals("[{\"ruleCode\":\"R1\"}]", unplannedExplain.getRuleHitJson());
    }

    @Test
    public void persistUnplannedShouldCompleteWithoutException() {
        TmPersistService service = new TmPersistService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask(1);
        TmSnapshotBuildResult snapshot = new TmSnapshotBuildResult();
        snapshot.setRuleHitJson("[]");

        service.persistUnplanned(task, snapshot, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void persistUnplannedShouldRejectNullTask() {
        new TmPersistService().persistUnplanned(null, new TmSnapshotBuildResult(), buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void persistUnplannedShouldRejectNullContext() {
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
        task.setTreadCode("TR-" + shiftOrder);
        task.setGlueCode("GL-" + shiftOrder);
        task.setMouthPlateCode("MP-" + shiftOrder);
        task.setPlanQty(new BigDecimal(10 + shiftOrder));
        return task;
    }
}
