package com.zlt.aps.tm.engine.scenario;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.enums.TmScheduleEventTypeEnum;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.TmInsertPosition;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.domain.TmTransferPosition;
import com.zlt.aps.tm.engine.event.TmScheduleEvent;
import com.zlt.aps.tm.engine.event.TmScheduleEventPublisher;
import com.zlt.aps.tm.engine.service.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 胎面排程详设场景测试。
 *
 * <p>使用 `docs/tm/tm_schedule_scenario_test_cases.md` 中的固定模拟数据，
 * 串联验证自动排程骨架、未排解释、人工操作、事件发布和落库转换。</p>
 */
public class TmScheduleScenarioTest {

    private static final String FACTORY_CODE = "F-TM-01";

    private static final String OPERATOR = "scenario-user";

    private static final String MACHINE_01 = "TM-01";

    private static final String MACHINE_02 = "TM-02";

    private static final String NO_AVAILABLE_MACHINE = TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode();

    /**
     * 验证自动排程骨架场景。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test
    public void autoScheduleScenarioShouldSortAssignExplainAndConvertResults() throws Exception {
        TmScheduleContext context = buildContext();
        TmTaskDraft task002 = buildTask("ORD-SC-002", MACHINE_01, "80", null);
        TmTaskDraft task001 = buildTask("ORD-SC-001", MACHINE_01, "120", "120");
        TmTaskDraft task003 = buildTask("ORD-SC-003", null, "60", "60");
        context.setTaskDraftList(Arrays.asList(task002, task001, task003));

        TmTaskChainScheduleService chainService = new TmTaskChainScheduleService();
        TmPersistService persistService = new TmPersistService();

        new TmPlanBootstrapService().bootstrap(context);
        new TmInventoryPredictService().predict(context);
        new TmPlanCalcService().calculate(context);
        new TmTaskSortService().sort(context);
        new TmMachineAssignService(chainService).assign(context);
        new TmSnapshotAndPersistService(new TmSnapshotBuildService(), persistService).snapshotAndPersist(context);

        assertTrue(context.getBatchNo().startsWith("TM"));
        assertNotNull(context.getTraceId());
        assertEquals("ORD-SC-001", context.getTaskDraftList().get(0).getOrderNo());
        assertEquals("ORD-SC-002", context.getTaskDraftList().get(1).getOrderNo());
        assertEquals("ORD-SC-003", context.getTaskDraftList().get(2).getOrderNo());
        assertEquals(new BigDecimal("80"), task002.getPlanQty());
        assertEquals(NO_AVAILABLE_MACHINE, task003.getUnplannedReasonCode());

        ScheduleTaskLinkedList<TmTaskDraft> machineOneChain = context.getTaskChain(MACHINE_01, 1);
        assertNotNull(machineOneChain);
        assertEquals(2, machineOneChain.getSize());
        assertEquals("ORD-SC-001", machineOneChain.toList().get(0).getTask().getOrderNo());
        assertEquals("ORD-SC-002", machineOneChain.toList().get(1).getTask().getOrderNo());

        assertEquals(3, context.getSnapshotMap().size());
        assertEquals(3, context.getPersistResult().getResultCount());
        assertEquals(3, context.getPersistResult().getExplainCount());
        assertEquals(1, context.getPersistResult().getUnplannedCount());

        List<TmScheduleResult> resultList = persistService.convertChainToResult(machineOneChain, context);
        assertEquals(2, resultList.size());
        assertEquals(Integer.valueOf(1), resultList.get(0).getClass1Sequence());
        assertEquals(new BigDecimal("120"), resultList.get(0).getClass1PlanQty());
        assertEquals(Integer.valueOf(2), resultList.get(1).getClass1Sequence());
        assertEquals(new BigDecimal("80"), resultList.get(1).getClass1PlanQty());
    }

    /**
     * 验证人工插单、调量、转机台和删除场景。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test
    public void manualOperationScenarioShouldUpdateChainsAndPublishEvents() throws Exception {
        TmScheduleContext context = buildContext();
        TmTaskChainScheduleService chainService = new TmTaskChainScheduleService();
        new TmPlanBootstrapService().bootstrap(context);
        new TmMachineAssignService(chainService).assignPrepared(context,
                Arrays.asList(buildTask("ORD-SC-001", MACHINE_01, "120", "120"),
                        buildTask("ORD-SC-002", MACHINE_01, "80", "80")));

        List<TmScheduleEvent> events = new ArrayList<>();
        TmScheduleOperationFacade facade = new TmScheduleOperationFacade(chainService,
                new TmScheduleProcessLogger(), new TmScheduleEventPublisher(Arrays.asList(events::add)));

        TmTaskDraft insertTask = buildTask("ORD-SC-004", null, "50", "50");
        TmInsertPosition insertPosition = new TmInsertPosition();
        insertPosition.setMachineCode(MACHINE_01);
        insertPosition.setShiftOrder(1);
        insertPosition.setAnchorTaskId(buildTask("ORD-SC-001", MACHINE_01, "120", "120").getBusinessKey());
        facade.insertTask(insertTask, insertPosition, context);

        String insertedTaskId = insertTask.getBusinessKey();
        facade.changeQty(insertedTaskId, new BigDecimal("65"), 1, context);
        TmTransferPosition transferPosition = new TmTransferPosition();
        transferPosition.setShiftOrder(1);
        facade.transferMachine(insertedTaskId, MACHINE_02, transferPosition, context);
        facade.removeTask(insertedTaskId, context);

        ScheduleTaskLinkedList<TmTaskDraft> machineOneChain = context.getTaskChain(MACHINE_01, 1);
        ScheduleTaskLinkedList<TmTaskDraft> machineTwoChain = context.getTaskChain(MACHINE_02, 1);
        assertEquals(2, machineOneChain.getSize());
        assertEquals(0, machineTwoChain.getSize());
        assertEquals(Integer.valueOf(1), machineOneChain.toList().get(0).getSequence());
        assertEquals(Integer.valueOf(2), machineOneChain.toList().get(1).getSequence());

        assertEquals(4, events.size());
        assertEquals(TmScheduleEventTypeEnum.MANUAL_INSERT.getCode(), events.get(0).getEventType());
        assertEquals(TmScheduleEventTypeEnum.CHANGE_QTY.getCode(), events.get(1).getEventType());
        assertEquals(TmScheduleEventTypeEnum.TRANSFER_MACHINE.getCode(), events.get(2).getEventType());
        assertEquals(TmScheduleEventTypeEnum.REMOVE_TASK.getCode(), events.get(3).getEventType());
    }

    /**
     * 验证非法班次场景。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidShiftScenarioShouldRejectResultConversion() throws Exception {
        ScheduleTaskLinkedList<TmTaskDraft> chain = new ScheduleTaskLinkedList<>();
        ScheduleTaskNode<TmTaskDraft> node = new ScheduleTaskNode<>("ORD-SC-099",
                buildTask("ORD-SC-099", MACHINE_01, "99", "99"), MACHINE_01,
                LocalDate.of(2026, 6, 15), "CLASS7", 7, new BigDecimal("99"));
        chain.append(node, new ScheduleOperationContext(OPERATOR, "SCENARIO_APPEND", "TRACE-SC"));

        new TmPersistService().convertChainToResult(chain, buildContext());
    }

    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode(FACTORY_CODE);
        context.setScheduleDate(DateUtil.parseDate("2026-06-15"));
        context.setOperator(OPERATOR);
        return context;
    }

    private TmTaskDraft buildTask(String orderNo, String machineCode, String demandQty, String planQty) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo(orderNo);
        task.setMachineCode(machineCode);
        task.setTreadCode("TR-" + orderNo.substring(orderNo.length() - 3));
        task.setGlueCode(orderNo.endsWith("003") ? "GL-B" : "GL-A");
        task.setMouthPlateCode(orderNo.endsWith("003") ? "MP-B" : "MP-A");
        task.setDemandQty(new BigDecimal(demandQty));
        if (planQty != null) {
            task.setPlanQty(new BigDecimal(planQty));
        }
        return task;
    }
}
