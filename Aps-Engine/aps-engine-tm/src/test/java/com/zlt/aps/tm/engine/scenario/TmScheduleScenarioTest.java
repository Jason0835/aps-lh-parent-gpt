package com.zlt.aps.tm.engine.scenario;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
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
import com.zlt.aps.tm.engine.mapper.TmEngineInventoryPredictMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import com.zlt.aps.tm.engine.service.TmScheduleOperationFacade;
import com.zlt.aps.tm.engine.service.TmScheduleProcessLogger;
import com.zlt.aps.tm.engine.service.impl.*;
import com.zlt.aps.tm.engine.strategy.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
     * 测试内容：验证自动排程骨架能完成初始化、库存预测、计划量计算、排序、派机、快照和落库转换。
     * 测试场景：构造两个可排任务和一个无可用机台任务，验证同机台任务按规则排序并生成未排解释。
     * 预期结果：任务顺序、计划量、未排原因、任务链顺序、快照数量和 1 班落库字段均符合预期。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test
    public void autoScheduleScenarioShouldSortAssignExplainAndConvertResults() throws Exception {
        // 准备排程上下文和三条任务，覆盖正常可排、需要补计划量、无机台未排三类数据。
        TmScheduleContext context = buildContext();
        TmTaskDraft task002 = buildTask("ORD-SC-002", MACHINE_01, "80", null);
        TmTaskDraft task001 = buildTask("ORD-SC-001", MACHINE_01, "120", "120");
        TmTaskDraft task003 = buildTask("ORD-SC-003", null, "60", "60");
        context.setTaskDraftList(Arrays.asList(task002, task001, task003));

        // 准备排程服务和 mapper mock，避免依赖真实库存、库存预测数据库数据。
        TmTaskChainScheduleService chainService = new TmTaskChainScheduleService();
        TmPersistService persistService = new TmPersistService();
        TmEngineStockMapper stockMapper = mock(TmEngineStockMapper.class);
        TmEngineInventoryPredictMapper inventoryPredictMapper = mock(TmEngineInventoryPredictMapper.class);
        when(stockMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(inventoryPredictMapper.selectFirstShiftDemandRows(any(), any(), any())).thenReturn(Collections.emptyList());
        when(inventoryPredictMapper.selectFirstShiftPlanRows(any(), any(), any())).thenReturn(Collections.emptyList());
        TmStrategyRegistry registry = buildRegistry();

        // 按自动排程主流程逐步执行，验证各步骤之间的上下文传递和任务链生成。
        new TmPlanBootstrapService().bootstrap(context);
        new TmInventoryPredictService(stockMapper, inventoryPredictMapper).predict(context);
        new TmPlanCalcService(registry).calculate(context);
        new TmTaskSortService(registry).sort(context);
        new TmMachineAssignService(chainService, registry).assign(context);
        new TmSnapshotAndPersistService(new TmSnapshotBuildService(), persistService).snapshotAndPersist(context);

        // 断言批次、追踪号和排序结果，确保排程初始化和任务排序按预期生效。
        assertTrue(context.getBatchNo().startsWith("TM"));
        assertNotNull(context.getTraceId());
        assertEquals("ORD-SC-001", context.getTaskDraftList().get(0).getOrderNo());
        assertEquals("ORD-SC-002", context.getTaskDraftList().get(1).getOrderNo());
        assertEquals("ORD-SC-003", context.getTaskDraftList().get(2).getOrderNo());
        assertEquals(new BigDecimal("80"), task002.getPlanQty());
        assertEquals(NO_AVAILABLE_MACHINE, task003.getUnplannedReasonCode());

        // 断言 TM-01 的 1 班任务链顺序，确保派机后链表顺序和 sequence 生成基础正确。
        ScheduleTaskLinkedList<TmTaskDraft> machineOneChain = context.getTaskChain(MACHINE_01, 1);
        assertNotNull(machineOneChain);
        assertEquals(2, machineOneChain.getSize());
        assertEquals("ORD-SC-001", machineOneChain.toList().get(0).getTask().getOrderNo());
        assertEquals("ORD-SC-002", machineOneChain.toList().get(1).getTask().getOrderNo());

        // 断言快照和持久化统计，确保正常结果、解释和未排记录都进入落库结果汇总。
        assertEquals(3, context.getSnapshotMap().size());
        assertEquals(3, context.getPersistResult().getResultCount());
        assertEquals(3, context.getPersistResult().getExplainCount());
        assertEquals(1, context.getPersistResult().getUnplannedCount());

        // 将任务链转换为结果实体，验证 1 班 sequence 和计划量写入 class1 字段。
        List<TmScheduleResult> resultList = persistService.convertChainToResult(machineOneChain, context);
        assertEquals(2, resultList.size());
        assertEquals(Integer.valueOf(1), resultList.get(0).getClass1Sequence());
        assertEquals(new BigDecimal("120"), resultList.get(0).getClass1PlanQty());
        assertEquals(Integer.valueOf(2), resultList.get(1).getClass1Sequence());
        assertEquals(new BigDecimal("80"), resultList.get(1).getClass1PlanQty());
    }

    /**
     * 测试内容：验证 1-6 班任务都能进入独立任务链，并转换到对应 classN 结果字段。
     * 测试场景：每个班次各构造一条同机台任务，需求量和计划量按班次递增。
     * 预期结果：每个班次都有一条独立任务链，落库转换只写对应班次的 sequence 和 planQty。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test
    public void allShiftScenarioShouldAssignAndConvertClassFields() throws Exception {
        // 初始化排程上下文和任务链服务，先生成批次号、追踪号等排程基础信息。
        TmScheduleContext context = buildContext();
        TmTaskChainScheduleService chainService = new TmTaskChainScheduleService();
        TmPersistService persistService = new TmPersistService();
        new TmPlanBootstrapService().bootstrap(context);

        // 为 1-6 班分别构造一条任务，避免只覆盖 shiftOrder=1 的单班场景。
        List<TmTaskDraft> taskList = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            taskList.add(buildShiftTask(shiftOrder));
        }
        // 直接使用已计算计划量的任务派机，聚焦验证班次链路和结果转换。
        new TmMachineAssignService(chainService, buildRegistry()).assignPrepared(context, taskList);

        // 逐班断言任务链和 classN 字段映射，确保所有班次都能独立转换。
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain(MACHINE_01, shiftOrder);
            assertNotNull(chain);
            assertEquals(1, chain.getSize());
            assertEquals("ORD-SHIFT-" + shiftOrder, chain.toList().get(0).getTask().getOrderNo());

            List<TmScheduleResult> resultList = persistService.convertChainToResult(chain, context);
            assertEquals(1, resultList.size());
            assertClassField(resultList.get(0), shiftOrder, Integer.valueOf(1), new BigDecimal(100 + shiftOrder * 10));
        }
    }

    /**
     * 测试内容：验证人工插单、调量、转机台和删除操作会更新任务链并发布事件。
     * 测试场景：在 1 班已有两条任务的机台链中插入新任务，再调量、转机台、删除。
     * 预期结果：原机台链恢复两条任务，新机台链为空，四类人工事件按操作顺序发布。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test
    public void manualOperationScenarioShouldUpdateChainsAndPublishEvents() throws Exception {
        // 准备已有排程链，模拟自动排程后同一机台 1 班已经存在两条任务。
        TmScheduleContext context = buildContext();
        TmTaskChainScheduleService chainService = new TmTaskChainScheduleService();
        new TmPlanBootstrapService().bootstrap(context);
        new TmMachineAssignService(chainService, buildRegistry()).assignPrepared(context,
                Arrays.asList(buildTask("ORD-SC-001", MACHINE_01, "120", "120"),
                        buildTask("ORD-SC-002", MACHINE_01, "80", "80")));

        // 准备事件收集器，用于验证人工操作会发布对应类型的排程事件。
        List<TmScheduleEvent> events = new ArrayList<>();
        TmScheduleOperationFacade facade = new TmScheduleOperationFacade(chainService,
                new TmScheduleProcessLogger(), new TmScheduleEventPublisher(Arrays.asList(events::add)));

        // 在指定锚点之后插入任务，验证插单会进入目标机台和目标班次链。
        TmTaskDraft insertTask = buildTask("ORD-SC-004", null, "50", "50");
        TmInsertPosition insertPosition = new TmInsertPosition();
        insertPosition.setMachineCode(MACHINE_01);
        insertPosition.setShiftOrder(1);
        insertPosition.setAnchorTaskId(buildTask("ORD-SC-001", MACHINE_01, "120", "120").getBusinessKey());
        facade.insertTask(insertTask, insertPosition, context);

        // 对插入任务依次执行调量、转机台和删除，覆盖人工操作完整链路。
        String insertedTaskId = insertTask.getBusinessKey();
        facade.changeQty(insertedTaskId, new BigDecimal("65"), 1, context);
        TmTransferPosition transferPosition = new TmTransferPosition();
        transferPosition.setShiftOrder(1);
        facade.transferMachine(insertedTaskId, MACHINE_02, transferPosition, context);
        facade.removeTask(insertedTaskId, context);

        // 断言删除后目标任务不再留在任一机台链，原机台链重新顺序化。
        ScheduleTaskLinkedList<TmTaskDraft> machineOneChain = context.getTaskChain(MACHINE_01, 1);
        ScheduleTaskLinkedList<TmTaskDraft> machineTwoChain = context.getTaskChain(MACHINE_02, 1);
        assertEquals(2, machineOneChain.getSize());
        assertEquals(0, machineTwoChain.getSize());
        assertEquals(Integer.valueOf(1), machineOneChain.toList().get(0).getSequence());
        assertEquals(Integer.valueOf(2), machineOneChain.toList().get(1).getSequence());

        // 断言事件类型顺序，确保每个操作都留下可追踪的事件记录。
        assertEquals(4, events.size());
        assertEquals(TmScheduleEventTypeEnum.MANUAL_INSERT.getCode(), events.get(0).getEventType());
        assertEquals(TmScheduleEventTypeEnum.CHANGE_QTY.getCode(), events.get(1).getEventType());
        assertEquals(TmScheduleEventTypeEnum.TRANSFER_MACHINE.getCode(), events.get(2).getEventType());
        assertEquals(TmScheduleEventTypeEnum.REMOVE_TASK.getCode(), events.get(3).getEventType());
    }

    /**
     * 测试内容：验证人工操作在 1-6 班上都只影响目标班次链。
     * 测试场景：循环每个班次执行插单、调量、转机台、删除，同时保留其他班次基础任务。
     * 预期结果：目标班次任务按操作变化，非目标班次仍只保留原始任务且顺序不变。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test
    public void manualOperationScenarioShouldSupportEveryShiftIndependently() throws Exception {
        // 逐班执行同一组人工操作，确保测试覆盖所有 shiftOrder 而不是只覆盖 1 班。
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            // 每轮重新构造上下文，避免上一班次操作结果影响当前班次断言。
            TmScheduleContext context = buildContext();
            TmTaskChainScheduleService chainService = new TmTaskChainScheduleService();
            new TmPlanBootstrapService().bootstrap(context);
            // 先为 1-6 班各准备一条基础任务，作为验证跨班次互不影响的对照数据。
            List<TmTaskDraft> baseTasks = new ArrayList<>();
            for (int baseShift = 1; baseShift <= 6; baseShift++) {
                baseTasks.add(buildTask("ORD-BASE-" + baseShift, MACHINE_01, "100", "100", baseShift));
            }
            new TmMachineAssignService(chainService, buildRegistry()).assignPrepared(context, baseTasks);

            // 收集当前班次人工操作事件，校验完整操作链路都被记录。
            List<TmScheduleEvent> events = new ArrayList<>();
            TmScheduleOperationFacade facade = new TmScheduleOperationFacade(chainService,
                    new TmScheduleProcessLogger(), new TmScheduleEventPublisher(Arrays.asList(events::add)));

            // 插入当前目标班次的新任务，锚点固定为该班次原有基础任务。
            TmTaskDraft insertTask = buildTask("ORD-INSERT-" + shiftOrder, null, "50", "50", shiftOrder);
            TmInsertPosition insertPosition = new TmInsertPosition();
            insertPosition.setMachineCode(MACHINE_01);
            insertPosition.setShiftOrder(shiftOrder);
            insertPosition.setAnchorTaskId(baseTasks.get(shiftOrder - 1).getBusinessKey());
            facade.insertTask(insertTask, insertPosition, context);

            // 调整插入任务数量，验证只改目标任务的计划量。
            String insertedTaskId = insertTask.getBusinessKey();
            facade.changeQty(insertedTaskId, new BigDecimal("65"), shiftOrder, context);
            assertEquals(new BigDecimal("65"), insertTask.getPlanQty());

            // 将插入任务转到另一台机台的同一班次，验证原链摘除和新链追加。
            TmTransferPosition transferPosition = new TmTransferPosition();
            transferPosition.setShiftOrder(shiftOrder);
            facade.transferMachine(insertedTaskId, MACHINE_02, transferPosition, context);
            assertEquals(1, context.getTaskChain(MACHINE_01, shiftOrder).getSize());
            assertEquals(1, context.getTaskChain(MACHINE_02, shiftOrder).getSize());

            // 删除插入任务，验证目标班次恢复为仅保留原基础任务。
            facade.removeTask(insertedTaskId, context);
            assertEquals(1, context.getTaskChain(MACHINE_01, shiftOrder).getSize());
            assertEquals(0, context.getTaskChain(MACHINE_02, shiftOrder).getSize());

            // 检查非目标班次没有被串链或误删，仍保持各自原始基础任务。
            for (int otherShift = 1; otherShift <= 6; otherShift++) {
                if (otherShift == shiftOrder) {
                    continue;
                }
                assertEquals(1, context.getTaskChain(MACHINE_01, otherShift).getSize());
                assertEquals("ORD-BASE-" + otherShift,
                        context.getTaskChain(MACHINE_01, otherShift).toList().get(0).getTask().getOrderNo());
            }
            // 每轮都应产生插单、调量、转机台、删除四个事件。
            assertEquals(4, events.size());
        }
    }

    /**
     * 测试内容：验证非法班次不能转换为排程结果字段。
     * 测试场景：构造 shiftOrder=7 的任务链节点，模拟超出 1-6 班支持范围的数据。
     * 预期结果：落库转换抛出 ServiceException，避免写入不存在的 classN 字段。
     *
     * @throws Exception 场景步骤执行失败时由测试框架抛出
     */
    @Test(expected = ServiceException.class)
    public void invalidShiftScenarioShouldRejectResultConversion() throws Exception {
        // 构造包含非法 7 班节点的链表，直接触发持久化转换边界校验。
        ScheduleTaskLinkedList<TmTaskDraft> chain = new ScheduleTaskLinkedList<>();
        ScheduleTaskNode<TmTaskDraft> node = new ScheduleTaskNode<>("ORD-SC-099",
                buildTask("ORD-SC-099", MACHINE_01, "99", "99"), MACHINE_01,
                LocalDate.of(2026, 6, 15), "CLASS7", 7, new BigDecimal("99"));
        chain.append(node, new ScheduleOperationContext(OPERATOR, "SCENARIO_APPEND", "TRACE-SC"));

        // 执行转换时应识别非法班次并抛出异常。
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
        return buildTask(orderNo, machineCode, demandQty, planQty, 1);
    }

    private TmTaskDraft buildTask(String orderNo, String machineCode, String demandQty, String planQty, Integer shiftOrder) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo(orderNo);
        task.setMachineCode(machineCode);
        task.setTreadCode("TR-" + orderNo.substring(orderNo.length() - 3));
        task.setGlueCode(orderNo.endsWith("003") ? "GL-B" : "GL-A");
        task.setMouthPlateCode(orderNo.endsWith("003") ? "MP-B" : "MP-A");
        task.setShiftOrder(shiftOrder);
        task.setDemandQty(new BigDecimal(demandQty));
        if (planQty != null) {
            task.setPlanQty(new BigDecimal(planQty));
        }
        return task;
    }

    private TmTaskDraft buildShiftTask(int shiftOrder) {
        BigDecimal qty = new BigDecimal(100 + shiftOrder * 10);
        TmTaskDraft task = buildTask("ORD-SHIFT-" + shiftOrder, MACHINE_01, qty.toPlainString(), qty.toPlainString(), shiftOrder);
        task.setTreadCode("TR-215-001");
        task.setGlueCode("GL-A");
        task.setBaseGlueCode("BASE-A");
        task.setMouthPlateCode("MP-A");
        return task;
    }

    private void assertClassField(TmScheduleResult result, int shiftOrder, Integer sequence, BigDecimal planQty) {
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

    private TmStrategyRegistry buildRegistry() {
        return new TmStrategyRegistry(Collections.singletonList(new TmGuardDemandQtyStrategy()),
                Collections.singletonList(new TmDefaultPlanQtyStrategy()),
                Collections.singletonList(new TmDefaultMachineFilterRule()),
                Collections.singletonList(new TmDefaultMachineScoreStrategy()),
                Collections.singletonList(new TmDefaultTaskSortStrategy()));
    }
}
