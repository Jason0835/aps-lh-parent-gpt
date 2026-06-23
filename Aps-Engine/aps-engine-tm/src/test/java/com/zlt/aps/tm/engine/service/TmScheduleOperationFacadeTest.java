package com.zlt.aps.tm.engine.service;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.tm.api.enums.TmScheduleEventTypeEnum;
import com.zlt.aps.tm.engine.domain.TmInsertPosition;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.domain.TmTransferPosition;
import com.zlt.aps.tm.engine.event.TmScheduleEvent;
import com.zlt.aps.tm.engine.event.TmScheduleEventPublisher;
import com.zlt.aps.tm.engine.service.impl.TmTaskChainScheduleService;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 胎面排程操作门面测试。
 *
 * <p>验证插单、删除、转机台、调量统一经过门面完成任务链修改并发布调度事件。</p>
 */
public class TmScheduleOperationFacadeTest {

    @Test
    public void facadeShouldInsertRemoveTransferAndChangeQtyWithEvents() {
        List<TmScheduleEvent> events = new ArrayList<>();
        TmScheduleEventPublisher publisher = new TmScheduleEventPublisher(
                java.util.Collections.singletonList(events::add));
        TmScheduleOperationFacade facade = new TmScheduleOperationFacade(
                new TmTaskChainScheduleService(), new TmScheduleProcessLogger(), publisher);
        TmScheduleContext context = buildContext();

        TmInsertPosition insertPosition = new TmInsertPosition();
        insertPosition.setMachineCode("TM01");
        insertPosition.setShiftOrder(1);
        TmTaskDraft task = buildTask("ORD-1", null);
        facade.insertTask(task, insertPosition, context);
        String taskId = task.getBusinessKey();
        facade.changeQty(taskId, new BigDecimal("120"), 1, context);
        TmTransferPosition transferPosition = new TmTransferPosition();
        transferPosition.setShiftOrder(1);
        facade.transferMachine(taskId, "TM02", transferPosition, context);
        facade.removeTask(taskId, context);

        ScheduleTaskLinkedList<TmTaskDraft> sourceChain = context.getTaskChain("TM01", 1);
        ScheduleTaskLinkedList<TmTaskDraft> targetChain = context.getTaskChain("TM02", 1);
        assertEquals(0, sourceChain.getSize());
        assertEquals(0, targetChain.getSize());
        assertEquals(4, events.size());
        assertEquals(TmScheduleEventTypeEnum.MANUAL_INSERT.getCode(), events.get(0).getEventType());
        assertEquals(TmScheduleEventTypeEnum.CHANGE_QTY.getCode(), events.get(1).getEventType());
        assertEquals(TmScheduleEventTypeEnum.TRANSFER_MACHINE.getCode(), events.get(2).getEventType());
        assertEquals(TmScheduleEventTypeEnum.REMOVE_TASK.getCode(), events.get(3).getEventType());
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
}
