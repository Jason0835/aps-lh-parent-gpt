package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.engine.domain.*;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 胎面任务链排程服务测试。
 *
 * <p>验证自动追加、人工插单、删除、转机台和调量的链表操作正确性。</p>
 */
public class TmTaskChainScheduleServiceTest {

    @Test
    public void appendAutoTaskShouldAddNodeToEndOfChain() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-1", "TM01");

        ScheduleChainChangeResult<TmTaskDraft> result = service.appendAutoTask(task, buildCandidate("TM01"), context);

        assertNotNull(result);
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1);
        assertEquals(1, chain.getSize());
        assertEquals("TM01", chain.toList().get(0).getMachineCode());
        assertEquals(new BigDecimal("100"), chain.toList().get(0).getPlanQty());
    }

    @Test
    public void appendAutoTaskShouldUseTaskShiftOrder() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-4", "TM01");
        task.setShiftOrder(4);

        service.appendAutoTask(task, buildCandidate("TM01"), context);

        assertNull(context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1));
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 4);
        assertEquals(1, chain.getSize());
        assertEquals(Integer.valueOf(4), chain.toList().get(0).getShiftOrder());
        assertEquals("CLASS4", chain.toList().get(0).getShiftCode());
    }

    @Test
    public void insertManualTaskShouldAddNodeAfterAnchor() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task1 = buildTask("ORD-1", "TM01");
        service.appendAutoTask(task1, buildCandidate("TM01"), context);
        TmTaskDraft task2 = buildTask("ORD-2", "TM01");
        TmInsertPosition position = new TmInsertPosition();
        position.setMachineCode("TM01");
        position.setShiftOrder(1);
        position.setAnchorTaskId("ORD-1");

        ScheduleChainChangeResult<TmTaskDraft> result = service.insertManualTask(task2, position, context);

        assertNotNull(result);
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1);
        assertEquals(2, chain.getSize());
        // taskId 使用 businessKey 格式
        assertEquals(task1.getBusinessKey(), chain.toList().get(0).getTaskId());
        assertEquals(task2.getBusinessKey(), chain.toList().get(1).getTaskId());
    }

    @Test
    public void removeTaskShouldRemoveNodeFromChainAndResequence() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task1 = buildTask("ORD-1", "TM01");
        TmTaskDraft task2 = buildTask("ORD-2", "TM01");
        service.appendAutoTask(task1, buildCandidate("TM01"), context);
        service.appendAutoTask(task2, buildCandidate("TM01"), context);
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1);
        assertEquals(2, chain.getSize());

        // taskId 使用 businessKey 格式: orderNo|treadCode|glueCode|mouthPlateCode
        String taskId1 = task1.getBusinessKey();
        ScheduleChainChangeResult<TmTaskDraft> result = service.removeTask(taskId1, context);

        assertEquals(1, chain.getSize());
        assertEquals("REMOVE", result.getOperationType());
        assertFalse(result.getRemovedNodes().isEmpty());
        assertEquals(Integer.valueOf(1), chain.toList().get(0).getSequence());
        assertEquals("ORD-2|TR-ORD-2|GL-ORD-2|MP-ORD-2", chain.toList().get(0).getTaskId());
    }

    @Test
    public void transferMachineShouldMoveNodeBetweenChains() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-1", "TM01");
        service.appendAutoTask(task, buildCandidate("TM01"), context);

        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(1);
        String taskId = task.getBusinessKey();
        ScheduleChainChangeResult<TmTaskDraft> result = service.transferMachine(
                taskId, "TM02", position, context);

        ScheduleTaskLinkedList<TmTaskDraft> sourceChain = context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1);
        ScheduleTaskLinkedList<TmTaskDraft> targetChain = context.getTaskChainGroup()
                .get("TM02", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1);
        assertEquals(0, sourceChain.getSize());
        assertEquals(1, targetChain.getSize());
        assertEquals("TM02", targetChain.toList().get(0).getMachineCode());
        assertEquals("TRANSFER", result.getOperationType());
    }

    @Test
    public void changeQtyShouldUpdateNodePlanQty() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("ORD-1", "TM01");
        service.appendAutoTask(task, buildCandidate("TM01"), context);

        BigDecimal newQty = new BigDecimal("999");
        String taskId = task.getBusinessKey();
        ScheduleChainChangeResult<TmTaskDraft> result = service.changeQty(taskId, newQty, 1, context);

        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .get("TM01", DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate(), 1);
        ScheduleTaskNode<TmTaskDraft> node = chain.findByTaskId(taskId);
        assertEquals(newQty, node.getPlanQty());
        assertEquals(newQty, node.getTask().getPlanQty());
        assertEquals("RESEQUENCE", result.getOperationType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeTaskShouldRejectBlankTaskId() {
        new TmTaskChainScheduleService().removeTask("", buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeTaskShouldRejectNullContext() {
        new TmTaskChainScheduleService().removeTask("ORD-1", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeTaskShouldRejectNotFoundTask() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        service.appendAutoTask(buildTask("ORD-1", "TM01"), buildCandidate("TM01"), context);
        service.removeTask("NOT_EXIST", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferMachineShouldRejectBlankTaskId() {
        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(1);
        new TmTaskChainScheduleService().transferMachine("", "TM02", position, buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferMachineShouldRejectBlankTargetMachine() {
        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(1);
        new TmTaskChainScheduleService().transferMachine("ORD-1", "", position, buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferMachineShouldRejectNullPosition() {
        new TmTaskChainScheduleService().transferMachine("ORD-1", "TM02", null, buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeQtyShouldRejectBlankTaskId() {
        new TmTaskChainScheduleService().changeQty("", new BigDecimal("100"), 1, buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeQtyShouldRejectNullPlanQty() {
        new TmTaskChainScheduleService().changeQty("ORD-1", null, 1, buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeQtyShouldRejectNullShiftOrder() {
        new TmTaskChainScheduleService().changeQty("ORD-1", new BigDecimal("100"), null, buildContext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeQtyShouldRejectNotFoundTask() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        service.appendAutoTask(buildTask("ORD-1", "TM01"), buildCandidate("TM01"), context);
        service.changeQty("NOT_EXIST", new BigDecimal("100"), 1, context);
    }

    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F1");
        context.setBatchNo("BATCH-1");
        context.setTraceId("TRACE-1");
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
        task.setPlanQty(new BigDecimal("100"));
        return task;
    }

    private TmMachineCandidate buildCandidate(String machineCode) {
        TmMachineCandidate candidate = new TmMachineCandidate();
        candidate.setMachineCode(machineCode);
        return candidate;
    }
}
