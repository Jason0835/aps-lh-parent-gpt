package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.service.impl.TmTaskChainScheduleService;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 胎面任务链排程服务测试。
 *
 * <p>验证自动追加、人工插单、删除、转机台和调量的链表操作正确性。</p>
 */
public class TmTaskChainScheduleServiceTest {

    /**
     * 测试内容：验证自动排程任务会追加到目标机台任务链尾部。
     * 测试场景：上下文存在排程日期，任务和候选机台均指向 TM01。
     * 预期结果：TM01 一班任务链新增 1 个节点，节点机台、计划量和上下文索引同步写入。
     */
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
        assertNotNull(context.getTaskNode(task.getBusinessKey()));
    }

    /**
     * 测试内容：验证自动追加任务时使用任务自身班次。
     * 测试场景：任务 shiftOrder=4，候选机台为 TM01。
     * 预期结果：任务落入 TM01 四班任务链，不误落入一班，节点班次编码为 CLASS4。
     */
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

    /**
     * 测试内容：验证自动追加任务时任务不能为空。
     * 测试场景：传入空任务、有效候选机台和有效上下文。
     * 预期结果：抛出业务异常，避免生成无业务键节点。
     */
    @Test(expected = ServiceException.class)
    public void appendAutoTaskShouldRejectNullTask() {
        new TmTaskChainScheduleService().appendAutoTask(null, buildCandidate("TM01"), buildContext());
    }

    /**
     * 测试内容：验证自动追加任务时候选机台不能为空。
     * 测试场景：任务和上下文有效，但候选机台编码为空。
     * 预期结果：抛出机台候选异常。
     */
    @Test(expected = ServiceException.class)
    public void appendAutoTaskShouldRejectBlankMachineCandidate() {
        new TmTaskChainScheduleService().appendAutoTask(buildTask("ORD-1", "TM01"), buildCandidate(""), buildContext());
    }

    /**
     * 测试内容：验证自动追加任务时排程日期不能为空。
     * 测试场景：上下文缺少 scheduleDate。
     * 预期结果：抛出排程日期异常，避免任务链日期键错误。
     */
    @Test(expected = ServiceException.class)
    public void appendAutoTaskShouldRejectMissingScheduleDate() {
        TmScheduleContext context = buildContext();
        context.setScheduleDate(null);

        new TmTaskChainScheduleService().appendAutoTask(buildTask("ORD-1", "TM01"), buildCandidate("TM01"), context);
    }

    /**
     * 测试内容：验证人工插单会插到锚点任务之后。
     * 测试场景：TM01 一班已有 ORD-1，人工插入 ORD-2，锚点为 ORD-1。
     * 预期结果：任务链顺序为 ORD-1、ORD-2，并使用业务键作为节点 taskId。
     */
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

    /**
     * 测试内容：验证人工插单位置不能为空。
     * 测试场景：传入插单任务和上下文，但 position 为 null。
     * 预期结果：抛出参数异常。
     */
    @Test(expected = ServiceException.class)
    public void insertManualTaskShouldRejectNullPosition() {
        new TmTaskChainScheduleService().insertManualTask(buildTask("ORD-2", "TM01"), null, buildContext());
    }

    /**
     * 测试内容：验证删除任务会从任务链和上下文索引中移除节点。
     * 测试场景：TM01 一班已有 ORD-1、ORD-2，删除 ORD-1。
     * 预期结果：任务链剩余 ORD-2，顺序重排为 1，删除节点从 taskNodeIndex 中移除。
     */
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
        assertNull(context.getTaskNode(taskId1));
        assertEquals(Integer.valueOf(1), chain.toList().get(0).getSequence());
        assertEquals("ORD-2|TR-ORD-2|GL-ORD-2|MP-ORD-2", chain.toList().get(0).getTaskId());
    }

    /**
     * 测试内容：验证转机台会把任务从原机台链移动到目标机台链。
     * 测试场景：ORD-1 已在 TM01 一班，转移到 TM02 一班。
     * 预期结果：TM01 链为空，TM02 链新增节点，节点机台和上下文索引都更新为 TM02。
     */
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
        assertEquals("TM02", context.getTaskNode(taskId).getMachineCode());
        assertEquals("TRANSFER", result.getOperationType());
    }

    /**
     * 测试内容：验证调量会同步更新任务链节点和任务草稿计划量。
     * 测试场景：ORD-1 已在 TM01 一班，将计划量调整为 999。
     * 预期结果：节点 planQty 与任务草稿 planQty 均为 999，并返回重排操作结果。
     */
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

    /**
     * 测试内容：验证删除任务时任务 ID 不能为空。
     * 测试场景：removeTask 传入空字符串 taskId。
     * 预期结果：抛出业务异常，不扫描任务链。
     */
    @Test(expected = ServiceException.class)
    public void removeTaskShouldRejectBlankTaskId() {
        new TmTaskChainScheduleService().removeTask("", buildContext());
    }

    /**
     * 测试内容：验证删除任务时上下文不能为空。
     * 测试场景：removeTask 传入有效 taskId，但 context 为 null。
     * 预期结果：抛出业务异常，避免访问空上下文。
     */
    @Test(expected = ServiceException.class)
    public void removeTaskShouldRejectNullContext() {
        new TmTaskChainScheduleService().removeTask("ORD-1", null);
    }

    /**
     * 测试内容：验证删除不存在任务时拒绝操作。
     * 测试场景：任务链中只有 ORD-1，删除 NOT_EXIST。
     * 预期结果：抛出任务不存在异常，原链路不被误删。
     */
    @Test(expected = ServiceException.class)
    public void removeTaskShouldRejectNotFoundTask() {
        TmTaskChainScheduleService service = new TmTaskChainScheduleService();
        TmScheduleContext context = buildContext();
        service.appendAutoTask(buildTask("ORD-1", "TM01"), buildCandidate("TM01"), context);
        service.removeTask("NOT_EXIST", context);
    }

    /**
     * 测试内容：验证转机台时任务 ID 不能为空。
     * 测试场景：transferMachine 传入空 taskId。
     * 预期结果：抛出业务异常，不执行转机。
     */
    @Test(expected = ServiceException.class)
    public void transferMachineShouldRejectBlankTaskId() {
        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(1);
        new TmTaskChainScheduleService().transferMachine("", "TM02", position, buildContext());
    }

    /**
     * 测试内容：验证转机台时目标机台不能为空。
     * 测试场景：transferMachine 传入空目标机台。
     * 预期结果：抛出业务异常，不移动任务节点。
     */
    @Test(expected = ServiceException.class)
    public void transferMachineShouldRejectBlankTargetMachine() {
        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(1);
        new TmTaskChainScheduleService().transferMachine("ORD-1", "", position, buildContext());
    }

    /**
     * 测试内容：验证转机台位置对象不能为空。
     * 测试场景：transferMachine 传入 null position。
     * 预期结果：抛出业务异常，避免缺少目标班次信息。
     */
    @Test(expected = ServiceException.class)
    public void transferMachineShouldRejectNullPosition() {
        new TmTaskChainScheduleService().transferMachine("ORD-1", "TM02", null, buildContext());
    }

    /**
     * 测试内容：验证转机台时找不到任务会拒绝。
     * 测试场景：上下文中没有目标任务节点。
     * 预期结果：抛出任务不存在异常。
     */
    @Test(expected = ServiceException.class)
    public void transferMachineShouldRejectNotFoundTask() {
        TmTransferPosition position = new TmTransferPosition();
        position.setShiftOrder(1);

        new TmTaskChainScheduleService().transferMachine("NOT_EXIST", "TM02", position, buildContext());
    }

    /**
     * 测试内容：验证调量时任务 ID 不能为空。
     * 测试场景：changeQty 传入空 taskId。
     * 预期结果：抛出业务异常，不执行计划量调整。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyShouldRejectBlankTaskId() {
        new TmTaskChainScheduleService().changeQty("", new BigDecimal("100"), 1, buildContext());
    }

    /**
     * 测试内容：验证调量时新计划量不能为空。
     * 测试场景：changeQty 传入 null planQty。
     * 预期结果：抛出业务异常，避免写入空计划量。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyShouldRejectNullPlanQty() {
        new TmTaskChainScheduleService().changeQty("ORD-1", null, 1, buildContext());
    }

    /**
     * 测试内容：验证调量时班次不能为空。
     * 测试场景：changeQty 传入 null shiftOrder。
     * 预期结果：抛出业务异常，避免无法定位任务链。
     */
    @Test(expected = ServiceException.class)
    public void changeQtyShouldRejectNullShiftOrder() {
        new TmTaskChainScheduleService().changeQty("ORD-1", new BigDecimal("100"), null, buildContext());
    }

    /**
     * 测试内容：验证调量时找不到任务会拒绝。
     * 测试场景：任务链中只有 ORD-1，调量目标为 NOT_EXIST。
     * 预期结果：抛出任务不存在异常，原节点计划量不被误改。
     */
    @Test(expected = ServiceException.class)
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
