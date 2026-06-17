package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * 胎面默认步骤服务测试。
 *
 * <p>验证第16章模板注入所需的默认步骤服务可运行，同时不写死第15章未确认业务算法。</p>
 */
public class TmDefaultStepServiceTest {

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

    @Test(expected = IllegalArgumentException.class)
    public void bootstrapShouldRejectMissingScheduleDate() {
        TmScheduleContext context = buildContext();
        context.setScheduleDate(null);

        new TmPlanBootstrapService().bootstrap(context);
    }

    @Test
    public void taskSortShouldSortByBusinessKeyWithoutChangingTaskContent() {
        TmScheduleContext context = buildContext();
        TmTaskDraft taskB = buildTask("ORD-2", "TM01");
        TmTaskDraft taskA = buildTask("ORD-1", "TM01");
        context.setTaskDraftList(Arrays.asList(taskB, taskA));

        new TmTaskSortService().sort(context);

        assertEquals("ORD-1", context.getTaskDraftList().get(0).getOrderNo());
        assertEquals("ORD-2", context.getTaskDraftList().get(1).getOrderNo());
    }

    @Test
    public void machineAssignShouldAppendPresetMachineTaskAndMarkBlankMachineUnplanned() {
        TmScheduleContext context = buildContext();
        TmTaskDraft assigned = buildTask("ORD-1", "TM01");
        TmTaskDraft unassigned = buildTask("ORD-2", null);
        context.setTaskDraftList(Arrays.asList(assigned, unassigned));

        new TmMachineAssignService(new TmTaskChainScheduleService()).assign(context);

        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain("TM01", 1);
        assertNotNull(chain);
        assertEquals(1, chain.getSize());
        assertEquals("ORD-1", chain.toList().get(0).getTask().getOrderNo());
        assertEquals(TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode(), unassigned.getUnplannedReasonCode());
    }

    @Test
    public void snapshotAndPersistShouldBuildSnapshotsAndPersistSummary() {
        TmScheduleContext context = buildContext();
        context.setTaskDraftList(Arrays.asList(buildTask("ORD-1", "TM01"), buildTask("ORD-2", null)));
        new TmMachineAssignService(new TmTaskChainScheduleService()).assign(context);

        new TmSnapshotAndPersistService(new TmSnapshotBuildService(), new TmPersistService())
                .snapshotAndPersist(context);

        assertFalse(context.getSnapshotMap().isEmpty());
        assertNotNull(context.getPersistResult());
        assertEquals(2, context.getPersistResult().getResultCount());
        assertEquals(2, context.getPersistResult().getExplainCount());
        assertEquals(1, context.getPersistResult().getUnplannedCount());
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
}
